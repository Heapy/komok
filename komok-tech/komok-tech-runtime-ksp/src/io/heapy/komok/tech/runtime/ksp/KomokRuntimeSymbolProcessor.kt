package io.heapy.komok.tech.runtime.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.writeTo
import io.heapy.komok.tech.runtime.ksp.wrapper.WrapperRegistry

/**
 * Generates the Komok runtime graph code for every `@Module`: the `${M}_Komok` subclass,
 * `${M}Keys`, `${M}GraphApi`, and (treating each module as a possible root) the interned
 * DAG, `modules` container, `${M}Graph` and `create${M}Graph`. Detects module cycles at
 * compile time.
 */
class KomokRuntimeSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    // Wrapper kinds are ServiceLoader-discovered plugins (built-ins included) — the
    // processor itself knows no concrete wrapper.
    private val wrapperRegistry by lazy { WrapperRegistry.load(logger) }
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) {
            return emptyList()
        }
        val models = collectModules(resolver)
        val currentIndexedBindings = collectCurrentIndexedBindings(resolver)
        val currentProvidersFromBindings = collectProvidersFromBindings(resolver)
        val classpathIndexedBindings = collectClasspathIndexedBindings(resolver)
        val generatedMockTypes = collectGeneratedMockTypes(resolver, wrapperRegistry, logger)
        val syntheticModule = collectSyntheticModule(
            resolver = resolver,
            moduleFqn = options["komok.providers.module"],
            logger = logger,
            indexedBindings = classpathIndexedBindings + currentProvidersFromBindings,
        )
        if (
            models.isEmpty() &&
            syntheticModule == null &&
            currentIndexedBindings.isEmpty() &&
            generatedMockTypes.isEmpty()
        ) {
            return emptyList()
        }
        generated = true
        val modelsByDeclaration = models.associateBy { it.declaration }
        val sourceModels = models.filter { it.declaration.containingFile != null }

        if (!validateModules(models)) {
            return emptyList()
        }

        val wrappedTypes = collectWrappedTypes(sourceModels, wrapperRegistry)
        if (!validateWrappedTypes(wrappedTypes)) {
            return emptyList()
        }
        val mockTypes = (wrappedTypes + generatedMockTypes).distinctBy { it.fqn }
        if (!validateWrappedTypes(mockTypes)) {
            return emptyList()
        }
        val wrappedTypeFqns = wrappedTypes.mapTo(mutableSetOf()) { it.fqn }

        val depsFirst = try {
            models
                .associate { it.declaration to it.dependencies }
                .topologicalSort()
                .reversed()
        } catch (cycle: ModuleCycleException) {
            logger.error(cycle.message ?: "Module graph has a cycle")
            return emptyList()
        }

        wrappedTypes.forEach { type ->
            generateDecoratorFile(type).writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
                originatingKSFiles = listOfNotNull(type.declaration.containingFile),
            )
        }

        mockTypes.forEach { type ->
            val originatingFiles = listOfNotNull(type.declaration.containingFile)
            generateMockFile(type).writeTo(
                codeGenerator = codeGenerator,
                aggregating = originatingFiles.isEmpty(),
                originatingKSFiles = originatingFiles,
            )
        }

        sourceModels.forEach { model ->
            // aggregating: the module file's content depends on wrapper annotations carried
            // by provider return types declared in *other* files (wrappedTypeFqns) — with
            // isolating output, adding @Log to a service would not rewire existing modules
            // until a clean build.
            generateModuleFile(model, modelsByDeclaration, wrappedTypeFqns, syntheticModule).writeTo(
                codeGenerator = codeGenerator,
                aggregating = true,
                originatingKSFiles = listOfNotNull(model.declaration.containingFile),
            )

            val reachable = reachableModules(model, modelsByDeclaration, depsFirst)
            generateGraphFile(model, reachable, modelsByDeclaration, syntheticModule).writeTo(
                codeGenerator = codeGenerator,
                aggregating = true,
                originatingKSFiles = reachable.mapNotNull { it.declaration.containingFile },
            )
        }

        if (currentIndexedBindings.isNotEmpty()) {
            generateBindingIndexFile(currentIndexedBindings).writeTo(
                codeGenerator = codeGenerator,
                aggregating = true,
                originatingKSFiles = emptyList(),
            )
        }

        if (syntheticModule != null) {
            generateSyntheticServiceFile(syntheticModule).writeTo(
                codeGenerator = codeGenerator,
                aggregating = true,
                originatingKSFiles = syntheticModule.providers.mapNotNull {
                    when (it) {
                        is ServiceProviderModel.Function -> it.declaration.containingFile
                        is ServiceProviderModel.Constructor -> it.declaration.containingFile
                        is ServiceProviderModel.AggregateSet -> null
                        is ServiceProviderModel.ExternalFunction -> null
                    }
                },
            )
        }

        return emptyList()
    }

    private fun validateModules(models: List<ModuleModel>): Boolean {
        var valid = true
        models.forEach { model ->
            val parameterCount = model.declaration.primaryConstructor?.parameters?.size ?: 0
            if (parameterCount != model.dependencies.size) {
                valid = false
                logger.error(
                    "@Module ${model.fqn} constructor may only declare other @Module types as parameters.",
                    model.declaration,
                )
            }

            val duplicates = duplicateModuleTypes(
                model.dependencies.mapNotNull { it.qualifiedName?.asString() },
            )
            if (duplicates.isNotEmpty()) {
                valid = false
                logger.error(
                    "@Module ${model.fqn} declares multiple constructor parameters of the same module " +
                        "type (${duplicates.sorted().joinToString()}). Module identity is singleton-by-type " +
                        "in v1; use distinct @Module classes.",
                    model.declaration,
                )
            }

            model.providers.forEach { provider ->
                if (provider.typeParameters.isNotEmpty()) {
                    valid = false
                    logger.error(
                        "Provider ${model.fqn}.${provider.simpleName.asString()} must not declare " +
                            "type parameters (v1) — the generated override cannot reproduce them.",
                        provider,
                    )
                }
            }

            (model.startupHooks + model.shutdownHooks).forEach { hook ->
                if (!hook.isLifecycleHookShapeValid()) {
                    valid = false
                    logger.error(
                        "@Startup/@Shutdown ${model.fqn}.${hook.simpleName.asString()} must be a " +
                            "non-private `suspend` function with no value parameters returning Unit.",
                        hook,
                    )
                }
            }
        }
        return valid
    }

    private fun validateWrappedTypes(types: List<WrappedType>): Boolean {
        var valid = true
        types.forEach { type ->
            type.properties.forEach { property ->
                if (property.declaration.isMutable) {
                    valid = false
                    logger.error(
                        "Decoratable property ${type.fqn}.${property.name} must be a `val` (v1).",
                        property.declaration,
                    )
                }
            }
            type.methods.forEach { method ->
                val declaration = method.declaration
                val where = "${type.fqn}.${method.name}"
                if (declaration.typeParameters.isNotEmpty()) {
                    valid = false
                    logger.error("Decoratable method $where must not declare type parameters (v1).", declaration)
                }
                if (declaration.parameters.any { it.isVararg }) {
                    valid = false
                    logger.error("Decoratable method $where must not use vararg parameters (v1).", declaration)
                }
                if (method.isWrapped && Modifier.SUSPEND !in declaration.modifiers) {
                    valid = false
                    logger.error("Wrapped method $where must be `suspend`.", declaration)
                }
                method.applied.forEach { applied ->
                    applied.plugin.validate(declaration, applied.annotation).forEach { message ->
                        valid = false
                        logger.error("Wrapped method $where: $message", declaration)
                    }
                }
            }
        }
        return valid
    }

    private fun reachableModules(
        root: ModuleModel,
        modelsByDeclaration: Map<KSClassDeclaration, ModuleModel>,
        depsFirst: List<KSClassDeclaration>,
    ): List<ModuleModel> {
        val visited = HashSet<KSClassDeclaration>()
        fun visit(declaration: KSClassDeclaration) {
            if (visited.add(declaration)) {
                modelsByDeclaration.getValue(declaration).dependencies.forEach(::visit)
            }
        }
        visit(root.declaration)
        return depsFirst
            .filter { it in visited }
            .map { modelsByDeclaration.getValue(it) }
    }
}
