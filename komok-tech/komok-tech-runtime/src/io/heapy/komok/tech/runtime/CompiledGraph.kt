package io.heapy.komok.tech.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** A compiler-checked provider address, including its produced type. */
class Binding<T>(val id: BindingId)

/** Intrinsic: addresses a specific service function, including a set contribution. */
fun <T> binding(provider: kotlin.reflect.KFunction<T>): Binding<T> = compilerRequired()

class GraphOptions {
    var dispatcher: CoroutineDispatcher = Dispatchers.Default
    var rootContext: CoroutineContext = EmptyCoroutineContext
}

/** Intrinsic: the compiler constructs and registers the module DAG at this call. */
fun <M : Any> graph(configure: GraphOptions.() -> Unit = {}): KomokGraph<M> = compilerRequired()

/** Intrinsic: a graph containing the visible @Service and @ProvidersFrom bindings. */
fun services(configure: GraphOptions.() -> Unit = {}): KomokGraph<Unit> = compilerRequired()

fun compilerRequired(): Nothing = error("This call requires the Komok FIR/IR compiler plugin.")

/** Compiler-emitted wiring. Module instances are interned once and shared by all forks. */
class GraphDefinition {
    val modules: MutableMap<String, Any> = linkedMapOf()
    val descriptors: MutableList<ModuleDescriptor> = mutableListOf()
    val providers: MutableMap<BindingId, suspend context(KomokContext) () -> Any?> = linkedMapOf()
    val types: MutableMap<String, BindingId> = linkedMapOf()
    private val ambiguousTypes: MutableSet<String> = mutableSetOf()
    val hooks: MutableList<CompiledHook> = mutableListOf()
    val decorators: MutableMap<BindingId, (Any?) -> Any?> = linkedMapOf()

    fun registerModule(name: String, module: Any, dependencies: List<String>, bindings: List<String>) {
        modules[name] = module
        descriptors.add(ModuleDescriptor(name, dependencies, bindings))
    }

    fun registerProvider(
        owner: String,
        name: String,
        parameters: String,
        type: String,
        provider: suspend context(KomokContext) () -> Any?,
        instrumented: Boolean = true,
        exposeType: Boolean = true,
        decorate: (Any?) -> Any? = { it },
    ) {
        val id = BindingId(owner, ProviderSignature(name, parameters))
        decorators[id] = decorate
        if (owner !in modules) {
            val index = descriptors.indexOfFirst { it.fqn == owner }
            if (index < 0) descriptors.add(ModuleDescriptor(owner, emptyList(), listOf(id.display())))
            else descriptors[index] = descriptors[index].let { it.copy(bindings = it.bindings + id.display()) }
        }
        providers[id] = {
            val context = currentKomokContext()
            context.realm.runtime.binding(id) {
                val entered = currentKomokContext()
                val value = provider(if (instrumented) KomokContext(entered.realm, entered.callerStack, id) else entered)
                if (instrumented) value else decorate(value)
            }
        }
        if (exposeType) {
            if (types.put(type, id) != null) ambiguousTypes.add(type)
        }
    }

    fun registerHook(owner: String, startup: Boolean, action: suspend context(KomokContext) () -> Unit) {
        hooks.add(CompiledHook(owner, startup, action))
    }

    context(_: KomokContext)
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> resolveValue(type: String): T {
        val id = bindingId(type)
        return checkNotNull(providers[id])() as T
    }

    fun bindingId(type: String): BindingId {
        check(type !in ambiguousTypes) { "Multiple bindings for $type; select a provider through Module.Bindings." }
        return checkNotNull(types[type]) { "No binding for $type" }
    }

    fun registerSet(owner: String, name: String, type: String, contributions: List<String>) {
        val ids = contributions.map { address -> providers.keys.single { it.display() == address } }
        registerProvider(owner, name, "", type, {
            val values = linkedSetOf<Any?>()
            ids.forEach { id -> values.add(checkNotNull(providers[id])()) }
            values
        }, instrumented = false)
    }
}

class CompiledHook(
    val owner: String,
    val startup: Boolean,
    val action: suspend context(KomokContext) () -> Unit,
)

open class GraphScope(val definition: GraphDefinition) {
    /** Intrinsic: lowered to a module lookup using its compile-time qualified name. */
    fun <M : Any> module(): M = compilerRequired()

    /** Intrinsic: preserves the full generic type when addressing a service binding. */
    fun <T> binding(): Binding<T> = compilerRequired()

    /** Intrinsic: resolves a dependency in the current realm, including a replacement body. */
    context(_: KomokContext)
    suspend fun <T> provider(): T = compilerRequired()

    context(_: KomokContext)
    suspend fun <T> providerByType(type: String): T = definition.resolveValue(type)

    fun <T> bindingByType(type: String): Binding<T> =
        Binding(definition.bindingId(type))

    @Suppress("UNCHECKED_CAST")
    fun <M : Any> moduleByName(name: String): M =
        checkNotNull(definition.modules[name]) { "Module $name is not part of this graph." } as M
}

class GraphForkScope<M : Any>(definition: GraphDefinition, val root: M) : GraphScope(definition) {
    internal val overrides = linkedMapOf<BindingId, BindingOverride<*>>()

    fun <T> replace(binding: Binding<T>, value: T) {
        require(binding.id in definition.providers) { "Unknown binding ${binding.id}" }
        overrides[binding.id] = BindingOverride.Value(value)
    }

    fun <T> replaceWith(binding: Binding<T>, body: suspend context(KomokContext) GraphForkScope<M>.() -> T) {
        val decorate = checkNotNull(definition.decorators[binding.id]) { "Unknown binding ${binding.id}" }
        overrides[binding.id] = BindingOverride.Provider { decorate(body(this@GraphForkScope)) }
    }
}

class KomokGraph<M : Any>(
    val root: M,
    definition: GraphDefinition,
    val handle: RealmHandle,
    private val ownsRuntime: Boolean,
) : GraphScope(definition) {
    private val lifecycle: Lifecycle by lazy {
        val registry = LifecycleRegistry()
        definition.hooks.forEach { hook ->
            registry.register(hook.owner, if (hook.startup) LifecyclePhase.STARTUP else LifecyclePhase.SHUTDOWN) {
                handle.read(hook.action)
            }
        }
        Lifecycle(registry)
    }

    suspend operator fun <T> invoke(body: suspend context(KomokContext) M.() -> T): T =
        handle.read { body(root) }

    suspend fun <T> read(body: suspend context(KomokContext) KomokGraph<M>.() -> T): T =
        handle.read { body(this@KomokGraph) }

    /** Intrinsic: resolves the exact type, including all generic arguments. */
    suspend fun <T> resolve(): T = compilerRequired()

    suspend fun <T> resolveType(type: String): T = handle.read { definition.resolveValue(type) }

    @Suppress("UNCHECKED_CAST")
    suspend operator fun <T> get(binding: Binding<T>): T =
        handle.read { checkNotNull(definition.providers[binding.id]) { "Unknown binding ${binding.id}" }() as T }

    fun fork(configure: GraphForkScope<M>.() -> Unit): KomokGraph<M> {
        val scope = GraphForkScope(definition, root).apply(configure)
        return KomokGraph(root, definition, handle.fork(scope.overrides), ownsRuntime = false)
    }

    suspend fun realizeAll() {
        handle.read { definition.providers.values.forEach { realized(it()) } }
    }

    suspend fun start() {
        if (ownsRuntime) lifecycle.startup()
    }

    suspend fun stop() {
        if (ownsRuntime) {
            try {
                lifecycle.shutdown()
            } finally {
                handle.runtime.close()
            }
        } else {
            handle.close()
        }
    }

    fun graphPayload(): GraphPayload = GraphPayload(definition.descriptors, handle.runtime.edgeDescriptors())
}

/** Entry point called by the compiler after emitting a reflection-free wiring lambda. */
fun <M : Any> compiledGraph(
    configure: GraphOptions.() -> Unit = {},
    factory: (GraphDefinition) -> M,
): KomokGraph<M> {
    val options = GraphOptions().apply(configure)
    val definition = GraphDefinition()
    val root = factory(definition)
    val runtime = KomokRuntime(options.dispatcher, options.rootContext)
    return KomokGraph(root, definition, runtime.rootHandle(), ownsRuntime = true)
}
