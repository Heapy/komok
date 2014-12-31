package io.heapy.vipassana.core.di

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.typeOf

/**
 * Functional type that represents a module.
 * It may be functional interface, but it tend to implement module as classes,
 * what introduces more boilerplate.
 */
typealias ModuleBuilder = Binder.() -> Unit

typealias ModuleProvider = () -> Module

interface Module {
    /**
     * Source of the module (class fqn, or file and variable name).
     * Uniqueness will be enforced by library
     */
    val source: String

    /**
     * Dependencies of this module
     */
    val dependencies: List<ModuleProvider>

    /**
     * Bindings defined in this module
     */
    val bindings: List<Binding>
}

internal class DefaultModule(
    override val source: String,
    override val dependencies: List<ModuleProvider>,
    override val bindings: List<Binding>
) : Module

/**
 * Binder used to collect all [Binding]s and create context from them.
 */
interface Binder {
    @ModuleDSL
    fun dependency(module: ModuleProvider)

    @ModuleDSL
    fun contribute(binding: Binding)
}

@ModuleDSL
fun module(builder: ModuleBuilder): ReadOnlyProperty<Any?, ModuleProvider> =
    ModuleBuilderDelegate(builder)

private class ContainerBinder : Binder {
    private val _modules = mutableListOf<ModuleProvider>()
    private val _bindings = mutableListOf<Binding>()

    val modules: List<ModuleProvider> by ::_modules
    val bindings: List<Binding> by ::_bindings

    override fun dependency(module: ModuleProvider) {
        _modules.add(module)
    }

    override fun contribute(binding: Binding) {
        _bindings.add(binding)
    }
}

private class ModuleBuilderDelegateModuleProvider(
    private val builder: ModuleBuilder,
    private val source: String,
) : ModuleProvider {
    override fun invoke(): Module {
        val binder = ContainerBinder()
        this.builder.invoke(binder)

        return DefaultModule(
            source = source,
            dependencies = binder.modules,
            bindings = binder.bindings
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModuleBuilderDelegateModuleProvider

        if (builder != other.builder) return false
        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        var result = builder.hashCode()
        result = 31 * result + source.hashCode()
        return result
    }

    override fun toString(): String {
        return "ModuleBuilderDelegateModuleProvider(source='$source')"
    }
}

private class ModuleBuilderDelegate(
    private val builder: ModuleBuilder
) : ReadOnlyProperty<Any?, ModuleProvider> {
    override operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): ModuleProvider {
        val fullName = builder::class.toString()
        val variableName = property.name
        val place = fullName.substringBefore("$$variableName")
        val source = "$place.$variableName"

        return ModuleBuilderDelegateModuleProvider(
            builder = builder,
            source = source
        )
    }
}

data class Key(
    val type: KType,
)

@DslMarker
annotation class ModuleDSL

/**
 * Provides instance of type T.
 */
interface Provider<out T> {
    suspend fun new(): T
}

sealed class Binding(
    internal val key: Key
)

class BindingWithModule(
    key: Key,
    internal val binding: Binding,
    internal val module: Module
) : Binding(key)

class InstanceBinding(
    key: Key,
    internal val instance: Any?
) : Binding(key)

class ZeroArgProviderBinding(
    key: Key,
    internal val provider: suspend () -> Any?
) : Binding(key)

class ProviderBinding(
    key: Key,
    internal val provider: KFunction<Any?>
) : Binding(key)

class ListProviderBinding(
    key: Key,
    internal val provider: KFunction<Any?>
) : Binding(key)

class DelegateBinding(
    key: Key,
    delegatedTo: Key
) : Binding(key)

@JvmInline
value class GenericType<T : Any>(
    val actual: KType
)

inline fun <reified T : Any> type(): GenericType<T> {
    return GenericType(typeOf<T>())
}

interface Context {
    suspend fun <T : Any> get(key: Key): T
    suspend fun <T : Any> getOrNull(key: Key): T? {
        return try {
            get(key)
        } catch (e: Exception) {
            null
        }
    }
}

internal class KomodoContext(
    private val definitions: Map<Key, BindingWithModule>,
) : Context {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> get(key: Key): T {
        return if (key.isProvider()) {
            object : Provider<Any?> {
                override suspend fun new(): Any? {
                    return createType(Key(key.type.arguments.first().type!!), definitions)
                }
            }
        } else {
            createType(key, definitions)
        } as T
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Key.isProvider(): Boolean {
    return type.classifier == Provider::class
}

suspend fun <T : Any> createContextAndGet(
    type: GenericType<T>,
    moduleProvider: ModuleProvider,
): T {
    val bindings = mutableMapOf<Key, BindingWithModule>()
    val processedModules = mutableMapOf<ModuleProvider, Module>()

    val rootModule = moduleProvider()
    processedModules[moduleProvider] = rootModule

    processModuleTree(rootModule, processedModules)

    processedModules.forEach { (_, module) ->
        processBindings(bindings, module)
    }

    return KomodoContext(bindings.toMap()).get(Key(type.actual))
}

private fun processModuleTree(parent: Module, modules: MutableMap<ModuleProvider, Module>) {
    parent.dependencies.forEach { moduleProvider ->
        val processedModule = modules[moduleProvider]
        if (processedModule == null) {
            val module = moduleProvider()
            modules[moduleProvider] = module

            if (module.dependencies.isNotEmpty()) {
                processModuleTree(module, modules)
            }
        } else {
            print("Same module defined in [${processedModule.source}] and [${parent.source}].")
        }
    }
}

private fun processBindings(bindings: MutableMap<Key, BindingWithModule>, module: Module) {
    module.bindings.forEach { binding ->
        val processedBinding = bindings[binding.key]

        if (processedBinding == null) {
            bindings[binding.key] = BindingWithModule(
                key = binding.key,
                module = module,
                binding = binding
            )
        } else {
            val processed = processedBinding.module.source
            val current = module.source

            if (current == processed) {
                throw ContextException("Binding duplicated in module [$current].")
            } else {
                throw ContextException("Binding already present in module [$processed]. " +
                    "Current module: [$current]")
            }
        }
    }
}

internal suspend fun createType(
    key: Key,
    definitions: Map<Key, BindingWithModule>,
    stack: MutableList<Key> = mutableListOf()
): Any? {
    if (stack.contains(key)) {
        throw ContextException("A circular dependency found: ${printCircularDependencyGraph(key, stack, definitions)}")
    }

    val type = key.type
    val classifier = type.classifier
    val isOptional = type.isMarkedNullable

    if (classifier is KClass<*>) {
        if (classifier.objectInstance != null) {
            throw ContextException("Objects not allowed to bind")
        }
    }

    val binding = definitions[key]

    return if (binding == null) {
        if (isOptional) {
            null
        } else {
            throw ContextException("Required $key not found in context.")
        }
    } else {
        stack.add(key)
        // TODO: Don't create anything, but just save all actions and metadata,
        //   it will allow to implement dry-run and
        //   and create source code - equivalent of DI
        val instance = when (val actualBinding = binding.binding) {
            is BindingWithModule -> throw ContextException("BindingWithModule shouldn't be used not in root set.")
            is InstanceBinding -> actualBinding.instance
            is ZeroArgProviderBinding -> actualBinding.provider.invoke()
            is ProviderBinding -> {
                val params = actualBinding.provider.parameters.associateWith { param ->
                    createType(Key(param.type), definitions, stack)
                }

                if (actualBinding.provider.isSuspend) {
                    actualBinding.provider.callSuspendBy(params)
                } else {
                    actualBinding.provider.callBy(params)
                }
            }

            is ListProviderBinding -> TODO()
            is DelegateBinding -> TODO()
        }
        stack.remove(key)

        instance
    }
}

internal class ContextException(
    override val message: String
) : RuntimeException()

fun printCircularDependencyGraph(
    key: Key,
    stack: MutableList<Key>,
    bindings: Map<Key, BindingWithModule>
): String {
    return buildString {
        appendLine()
        stack.forEachIndexed { idx, stackKey ->
            append(" ".repeat(idx * 2))
            append(stackKey.type.classifier)
            bindings[stackKey]?.let {
                append(" implemented by ")
                val desc = when (val binding = it.binding) {
                    is InstanceBinding -> "instance [${binding.instance}]"
                    is ZeroArgProviderBinding -> "provider [${binding.provider::class}]"
                    is ProviderBinding -> "provider [${binding.provider::class}]"
                    is ListProviderBinding -> TODO()
                    is BindingWithModule -> TODO()
                    is DelegateBinding -> TODO()
                    else -> TODO()
                }
                append(desc)
            }
            if (stackKey == key) {
                appendLine(" <-- Circular dependency starts here")
            } else {
                appendLine()
            }
        }
        append(" ".repeat(stack.size * 2))
        append(key.type.classifier)
        bindings[key]?.let {
            append(" implemented by ")
            val desc = when (val binding = it.binding) {
                is InstanceBinding -> "instance [${binding.instance}]"
                is ZeroArgProviderBinding -> "provider [${binding.provider::class}]"
                is ProviderBinding -> "provider [${binding.provider::class}]"
                is ListProviderBinding -> TODO()
                is BindingWithModule -> TODO()
                is DelegateBinding -> TODO()
                else -> TODO()
            }
            append(desc)
        }
    }
}

// 3. Support override of dependencies (through optional wrapping?, i.e. override particular case of decoration)

// TODO: Provide configuration overview - like what classes overwrite by other classes. modules loaded. etc
// Configuration should be exported as data class
// also add ability to check is context can be started: e.g. validate context
// (List<Binder> -> Context)
// context.validate() -> dry run
// context.inspect() -> context overview
// check that wrappers work with delegation (by impl)

// https://rise4fun.com/agl
// Komodo visualizer site -> past json(dot files?) from terminal to visualize dependency graph!!!

// Scopes
// We support only singleton scope, since other scopes can be implemented in user space based on singleton scope
// And session/request scope should be managed by underline request framework.
// Inject Provider of any bean (useful for custom scopes)

// start/stop functions
// order problem: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/DependsOn.html

// warmUp function - top level function that can be executed before server will be started

@ModuleDSL
inline fun <reified I : Any> Binder.provide(
    noinline provider: suspend () -> I,
    customizer: (Binding) -> Binding = { it }
) {
    ZeroArgProviderBinding(
        key = Key(typeOf<I>()),
        provider = provider
    ).also {
        contribute(customizer(it))
    }
}

@ModuleDSL
inline fun <reified I : Any> Binder.provide(
    provider: KFunction<I>,
    customizer: (Binding) -> Binding = { it }
) {
    ProviderBinding(
        key = Key(typeOf<I>()),
        provider = provider
    ).also {
        contribute(customizer(it))
    }
}

@ModuleDSL
inline fun <reified I : Any> Binder.provideList(
    provider: KFunction<I>,
    customizer: (Binding) -> Binding = { it }
) {
    ListProviderBinding(
        key = Key(typeOf<I>()),
        provider = provider
    ).also {
        contribute(customizer(it))
    }
}

@ModuleDSL
inline fun <reified I1 : Any, reified I2 : I1> Binder.implementBy(
    customizer: (Binding) -> Binding = { it }
) {
    DelegateBinding(
        key = Key(typeOf<I1>()),
        delegatedTo = Key(typeOf<I2>())
    ).also {
        contribute(customizer(it))
    }
}
