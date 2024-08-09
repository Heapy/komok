package io.heapy.komok.tech.di.ez

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

@ModuleDSL
fun module(
    builder: ModuleBuilder,
): ReadOnlyProperty<Any?, ModuleProvider> {
    return ModuleBuilderPropertyDelegate(builder)
}

private class ModuleBuilderPropertyDelegate(
    private val builder: ModuleBuilder,
) : ReadOnlyProperty<Any?, ModuleProvider> {
    override operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): ModuleProvider {
        val fullName = builder::class.toString()
        val variableName = property.name
        val place = fullName.substringBefore("$\$Lambda")
        val source = "$place.$variableName"

        return ModuleBuilderDelegateModuleProvider(
            builder = builder,
            source = source,
        )
    }
}

private data class ModuleBuilderDelegateModuleProvider(
    private val builder: ModuleBuilder,
    private val source: String,
) : ModuleProvider {
    override fun module(): Module {
        val binder = ContainerBinder(source)
        this.builder.invoke(binder)

        return DefaultModule(
            source = source,
            dependencies = binder.modules,
            bindings = binder.bindings,
        )
    }

    override fun toString(): String {
        return "ModuleBuilderDelegateModuleProvider(source='$source')"
    }
}

private class ContainerBinder(
    override val source: String,
) : Binder {
    private val _modules = mutableListOf<ModuleProvider>()
    private val _bindings = mutableListOf<Binding<*>>()

    val modules: List<ModuleProvider> by ::_modules
    val bindings: List<Binding<*>> by ::_bindings

    override fun dependency(module: ModuleProvider) {
        _modules.add(module)
    }

    override fun contribute(binding: Binding<*>) {
        _bindings.add(binding)
    }
}

internal class DefaultModule(
    override val source: String,
    override val dependencies: List<ModuleProvider>,
    override val bindings: List<Binding<*>>,
) : Module

sealed interface Binding<T> {
    val key: GenericKey<T>
    val source: String
}

data class ZeroArgProviderBinding<T>(
    override val key: GenericKey<T>,
    internal val provider: () -> T,
    override val source: String,
) : Binding<T>

data class ProviderBinding<T>(
    override val key: GenericKey<T>,
    @PublishedApi internal val provider: KFunction<T>,
    override val source: String,
) : Binding<T>

internal class KomokContext(
    private val definitions: Map<Key, Binding<*>>,
) : Context {
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Key): T {
        val executionContext = object : ExecutionContext {
            override val definitions: Map<Key, Binding<*>> = this@KomokContext.definitions
            override val stack: MutableList<Key> = mutableListOf()
            override val instances: MutableMap<Key, Any?> = mutableMapOf()
        }

        return if (key.isProvider()) {
            object : Provider<Any?> {
                override fun get(): Any? {
                    return executionContext.createType(
                        key = GenericKey<Any?>(type = key.type.arguments.first().type!!),
                    )
                }
            }
        } else {
            executionContext.createType(key)
        } as T
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Key.isProvider(): Boolean {
    return type.classifier == Provider::class
}

fun <T : Any> createContextAndGet(
    type: GenericKey<T>,
    moduleProvider: ModuleProvider,
): T {
    val bindings = mutableMapOf<Key, Binding<*>>()
    val processedModules = mutableMapOf<ModuleProvider, Module>()

    val rootModule = moduleProvider.module()
    processedModules[moduleProvider] = rootModule

    processModuleTree(
        rootModule,
        processedModules,
    )

    processedModules.forEach { (_, module) ->
        processBindings(
            bindings,
            module,
        )
    }

    return KomokContext(bindings.toMap()).get(type)
}

private fun processModuleTree(
    parent: Module,
    modules: MutableMap<ModuleProvider, Module>,
) {
    parent.dependencies.forEach { moduleProvider ->
        val processedModule = modules[moduleProvider]
        if (processedModule == null) {
            val module = moduleProvider.module()
            modules[moduleProvider] = module

            if (module.dependencies.isNotEmpty()) {
                processModuleTree(
                    parent = module,
                    modules = modules,
                )
            }
        }
    }
}

private fun processBindings(
    bindings: MutableMap<Key, Binding<*>>,
    module: Module,
) {
    module.bindings.forEach { binding ->
        val processedBinding = bindings[binding.key]

        if (processedBinding == null) {
            bindings[binding.key] = binding
        } else {
            val processed = processedBinding.source
            val current = module.source

            if (current == processed) {
                throw ContextException("Binding [${processedBinding.key}] duplicated in module [$current].")
            } else {
                throw ContextException(
                    "Binding [${processedBinding.key}] already present in module " + "[$processed]. Current module: [$current]",
                )
            }
        }
    }
}

interface ExecutionContext {
    val definitions: Map<Key, Binding<*>>
    val stack: MutableList<Key>
    val instances: MutableMap<Key, Any?>
}

internal fun ExecutionContext.createType(
    key: Key,
    saveInstance: Boolean = true,
): Any? {
    if (stack.contains(key)) {
        val graph = printCircularDependencyGraph(
            key,
            stack,
            definitions,
        )
        throw ContextException(
            "A circular dependency found:$graph",
        )
    }

    val type = key.type
    val classifier = type.classifier
    val isOptional = type.isMarkedNullable

    if (classifier is KClass<*>) {
        if (classifier.objectInstance != null) {
            throw ContextException("Objects not allowed to be bound.")
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
        val alreadyCreated = instances.contains(key)

        if (alreadyCreated) {
            instances[key]
        } else {
            stack.add(key)
            // TODO: Don't create anything, but just save all actions and metadata,
            //   it will allow to implement dry-run and
            //   and create source code - equivalent of DI
            val instance = when (binding) {
                is ZeroArgProviderBinding -> binding.provider()
                is ProviderBinding -> {
                    val params = binding.provider.parameters.associateWith { param ->
                        createType(
                            key = GenericKey<Any>(type = param.type),
                        )
                    }

                    binding.provider.callBy(params)
                }
            }
            stack.remove(key)

            if (saveInstance) {
                instances[key] = instance
            }

            instance
        }
    }
}

internal class ContextException(
    override val message: String,
) : RuntimeException()

fun printCircularDependencyGraph(
    key: Key,
    stack: MutableList<Key>,
    bindings: Map<Key, Binding<*>>,
): String {
    return buildString {
        appendLine()
        stack.forEachIndexed { idx, stackKey ->
            append(" ".repeat(idx * 2))
            append(stackKey.type.classifier)
            bindings[stackKey]?.let {
                append(" implemented by ")
                val desc = when (it) {
                    is ZeroArgProviderBinding -> "provider [${it.provider::class}]"
                    is ProviderBinding -> "provider [${it.provider::class}]"
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
            val desc = when (it) {
                is ZeroArgProviderBinding -> "provider [${it.provider::class}]"
                is ProviderBinding -> "provider [${it.provider::class}]"
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
// Komok visualizer site -> past json(dot files?) from terminal to visualize dependency graph!!!

// start/stop functions
// order problem: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/DependsOn.html
// auto-mocks in tests, i.e. provide mocks for all dependencies of module, auto `confirmVerified` for all mocks

@ModuleDSL
inline fun <reified I : Any> Binder.provideInstance(
    noinline provider: () -> I,
) {
    contribute(
        ZeroArgProviderBinding(
            key = genericKey<I>(),
            provider = provider,
            source = source,
        )
    )
}

@ModuleDSL
inline fun <reified I> Binder.provide(
    provider: KFunction<I>,
) {
    contribute(
        ProviderBinding(
            key = genericKey<I>(),
            provider = provider,
            source = source,
        )
    )
}
