package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.*
import java.util.zip.ZipFile

private val serviceAnnotation = FqName("io.heapy.komok.tech.runtime.Service")
private val providersFromAnnotation = FqName("io.heapy.komok.tech.runtime.ProvidersFrom")
internal val intoSetAnnotation = FqName("io.heapy.komok.tech.runtime.IntoSet")

internal data class ServiceBinding(
    val target: IrFunction,
    val providedType: IrType,
    val objectOwner: IrClass? = null,
    val intoSet: Boolean = false,
) {
    val exported: Boolean = target.visibility == DescriptorVisibilities.PUBLIC &&
        (target.parent as? IrClass)?.visibility.let { it == null || it == DescriptorVisibilities.PUBLIC }
    val name: String = if (target is IrConstructor) target.parentAsClass.name.asString().replaceFirstChar(Char::lowercaseChar)
        else target.name.asString()
    val packageName: String = target.getPackageFragment().packageFqName.asString()
    val owner: String = "$packageName.Services"
    val parameters: List<IrValueParameter> = target.parameters.filter { it.kind == IrParameterKind.Regular }
    val descriptor: String = if (parameters.isEmpty()) "" else parameters.joinToString(", ", "(", ")") { it.type.typeKey() }
    val id: String = "$owner.$name$descriptor"

    fun encode(): String = when {
        target is IrConstructor -> "class|${target.parentAsClass.fqNameWhenAvailable}||"
        objectOwner != null -> "object|${objectOwner.fqNameWhenAvailable}|${target.name}|$descriptor"
        else -> "function|$packageName|${target.name}|$descriptor"
    }
}

internal class ServiceCatalog(
    private val pluginContext: IrPluginContext,
    private val configuration: CompilerConfiguration,
    private val module: IrModuleFragment,
) {
    private val finder = pluginContext.finderForSource(module.files.first())
    private val local = linkedMapOf<String, ServiceBinding>()
    val bindings: List<ServiceBinding>

    init {
        module.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) { element.acceptChildrenVoid(this) }
            override fun visitClass(declaration: IrClass) {
                if (declaration.hasAnnotation(serviceAnnotation)) {
                    val constructor = requireNotNull(declaration.primaryConstructor) { "@Service class requires a primary constructor." }
                    add(constructor, declaration.getAnnotation(serviceAnnotation), declaration.defaultType)
                }
                importProviders(declaration)
                declaration.acceptChildrenVoid(this)
            }
            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                if (declaration.hasAnnotation(serviceAnnotation)) {
                    komokRequire(declaration.parent is IrFile) { "@Service functions must be top-level." }
                    add(declaration, declaration.getAnnotation(serviceAnnotation), declaration.returnType)
                }
                importProviders(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })
        val combined = linkedMapOf<String, ServiceBinding>()
        readIndexes().forEach { encoded ->
            decode(encoded)?.let { combined[it.id] = it }
        }
        combined.putAll(local)
        bindings = combined.values.filter { it.exported || it.id in local }.toList()
    }

    private fun add(target: IrFunction, annotation: IrConstructorCall?, defaultType: IrType) {
        komokRequire(target.visibility == DescriptorVisibilities.PUBLIC || target.visibility == DescriptorVisibilities.INTERNAL) {
            "@Service providers must be public or internal: ${target.name}"
        }
        komokRequire(target.typeParameters.isEmpty() && (target !is IrConstructor || target.parentAsClass.typeParameters.isEmpty())) {
            "@Service providers require concrete types: ${target.name}"
        }
        komokRequire(!defaultType.isUnit()) { "@Service providers must return a value: ${target.name}" }
        komokRequire(target.parameters.none { it.kind == IrParameterKind.ExtensionReceiver } &&
            target.parameters.filter { it.kind == IrParameterKind.Context }.all { it.type.classFqName == KomokNames.context.asSingleFqName() }) {
            "@Service providers only support the KomokContext context parameter and no extension receiver."
        }
        val explicit = listOf("bind", "value").firstNotNullOfOrNull { name ->
            (annotation?.argument(name) as? IrClassReference)?.classType?.takeUnless { it.isUnit() }
        }
        if (explicit != null) komokRequire(defaultType.isSubtypeOfClass(explicit.classOrNull!!)) {
            "@Service result ${defaultType.typeKey()} does not implement ${explicit.typeKey()}"
        }
        val binding = ServiceBinding(target, explicit ?: defaultType,
            intoSet = target.hasAnnotation(intoSetAnnotation))
        local[binding.id] = binding
    }

    private fun importProviders(declaration: IrAnnotationContainer) {
        val arguments = declaration.getAnnotation(providersFromAnnotation)?.argument("value") as? IrVararg ?: return
        arguments.elements.filterIsInstance<IrClassReference>().forEach { reference ->
            val owner = requireNotNull(reference.classType.classOrNull).owner
            komokRequire(owner.kind == ClassKind.OBJECT) { "@ProvidersFrom requires an object: ${owner.name}" }
            owner.functions.filter { function ->
                function.visibility == DescriptorVisibilities.PUBLIC && !function.isMethodOfAny() && !function.returnType.isUnit() &&
                    function.parameters.none { it.kind == IrParameterKind.Regular }
            }.forEach { function ->
                val binding = ServiceBinding(function, function.returnType, owner, function.hasAnnotation(intoSetAnnotation))
                local[binding.id] = binding
            }
        }
    }

    private fun decode(encoded: String): ServiceBinding? {
        val parts = encoded.split('|')
        komokRequire(parts.size == 4) { "Invalid Komok compiler binding index: $encoded" }
        return when (parts[0]) {
            "class" -> {
                val owner = finder.findClass(ClassId.topLevel(FqName(parts[1])))?.owner ?: return null
                val constructor = owner.primaryConstructor ?: return null
                val annotation = owner.getAnnotation(serviceAnnotation)
                val type = listOf("bind", "value").firstNotNullOfOrNull {
                    (annotation?.argument(it) as? IrClassReference)?.classType?.takeUnless { type -> type.isUnit() }
                } ?: owner.defaultType
                ServiceBinding(constructor, type)
            }
            "function", "object" -> {
                val owner = if (parts[0] == "object") finder.findClass(ClassId.topLevel(FqName(parts[1])))?.owner else null
                val function = if (owner != null) owner.functions.single { it.name.asString() == parts[2] && it.providerDescriptor() == parts[3] }
                    else finder.findFunctions(CallableId(FqName(parts[1]), Name.identifier(parts[2]))).singleOrNull { it.owner.providerDescriptor() == parts[3] }?.owner ?: return null
                val annotation = function.getAnnotation(serviceAnnotation)
                val type = listOf("bind", "value").firstNotNullOfOrNull {
                    (annotation?.argument(it) as? IrClassReference)?.classType?.takeUnless { type -> type.isUnit() }
                } ?: function.returnType
                ServiceBinding(function, type, owner, function.hasAnnotation(intoSetAnnotation))
            }
            else -> komokError("Unknown Komok binding index kind: ${parts[0]}")
        }
    }

    private fun readIndexes(): List<String> = buildList {
        val roots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS).filterIsInstance<JvmClasspathRoot>()
        fun read(bytes: ByteArray) {
            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                    if (descriptor != "Lio/heapy/komok/tech/runtime/KomokBindingIndex;") return null
                    return object : AnnotationVisitor(Opcodes.ASM9) {
                        override fun visit(name: String?, value: Any) {
                            if (name == "version") komokRequire(value == 1) { "Unsupported Komok binding index version: $value" }
                        }
                        override fun visitArray(name: String): AnnotationVisitor? {
                            if (name != "bindings") return null
                            return object : AnnotationVisitor(Opcodes.ASM9) {
                                override fun visit(name: String?, value: Any) { if (value is String) add(value) }
                            }
                        }
                    }
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }
        roots.map { it.file }.distinct().forEach { file ->
            if (file.isDirectory) {
                file.walkTopDown().filter { it.name.startsWith("__KomokIndex_") && it.extension == "class" }
                    .forEach { read(it.readBytes()) }
            } else if (file.extension == "jar") {
                ZipFile(file).use { zip ->
                    zip.entries().asSequence().filter { it.name.substringAfterLast('/').startsWith("__KomokIndex_") && it.name.endsWith(".class") }
                        .forEach { entry -> zip.getInputStream(entry).use { read(it.readBytes()) } }
                }
            }
        }
    }

    fun emitIndex() {
        if (local.isEmpty()) return
        val name = configuration.get(CommonConfigurationKeys.MODULE_NAME).orEmpty().replace(Regex("[^A-Za-z0-9_]"), "_")
        val index = pluginContext.irFactory.buildClass {
            this.name = Name.identifier("__KomokIndex_$name")
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            parent = this@ServiceCatalog.module.files.first()
            superTypes = listOf(pluginContext.irBuiltIns.anyType)
            createThisReceiverParameter()
        }
        val constructor = index.addConstructor { isPrimary = true }
        val builder = DeclarationIrBuilder(pluginContext, constructor.symbol)
        constructor.body = builder.irBlockBody {
            +irDelegatingConstructorCall(pluginContext.irBuiltIns.anyClass.owner.constructors.single())
        }
        val annotation = finder.findClass(ClassId(KomokNames.runtimePackage, Name.identifier("KomokBindingIndex")))!!.owner
        index.annotations += with(builder) {
            irAnnotation(annotation.constructors.single().symbol, emptyList()).apply {
                arguments[0] = irVararg(pluginContext.irBuiltIns.stringType, local.values.filter { it.exported }.map { irString(it.encode()) })
            }
        }
        module.files.first().declarations.add(index)
    }
}

internal fun IrConstructorCall.argument(name: String): IrExpression? =
    symbol.owner.parameters.singleOrNull { it.name.asString() == name }?.let { arguments[it.indexInParameters] }
