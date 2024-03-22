package io.heapy.komok.infra.di

@KomokDsl
suspend inline fun <reified T : EntryPoint<Unit>> komok(
    noinline builder: KomokBuilder.() -> Unit,
): Unit = komokReturning<T, Unit>(builder)

@KomokDsl
suspend inline fun <reified T : EntryPoint<R>, R> komokReturning(
    noinline builder: KomokBuilder.() -> Unit,
): R {
    val komokBuilder = DefaultKomokBuilder()
    builder(komokBuilder)
    val komok = komokBuilder.komok()
    return komok.run(builder::class.toString(), genericKey<T>())
}

@DslMarker
private annotation class KomokDsl

/**
 * Collects all external dependencies to build instance of [Komok].
 */
@KomokDsl
interface KomokBuilder : Binder {
    @KomokDsl
    fun args(args: Array<String>)

    @KomokDsl
    fun env(env: Map<String, String>)

    @KomokDsl
    fun props(props: Map<String, String>)
}

/**
 * Represents entry point of application.
 *
 * Usually this method runs something like web server or desktop application.
 *
 * @author Ruslan Ibrahimau
 */
interface EntryPoint<out R> {
    suspend fun run(): R
}

/**
 * @author Ruslan Ibrahimau
 */
@PublishedApi
internal class DefaultKomokBuilder : KomokBuilder {
    private val modules = mutableListOf<ModuleProvider>()
    private val bindings = mutableListOf<Binding<*>>()
    private val args = mutableListOf<String>()
    private val env = mutableMapOf<String, String>()
    private val props = mutableMapOf<String, String>()

    override fun args(args: Array<String>) {
        this.args += args
    }

    override fun env(env: Map<String, String>) {
        this.env += env
    }

    override fun props(props: Map<String, String>) {
        this.props += props
    }

    override fun dependency(module: ModuleProvider) {
        modules += module
    }

    override fun contribute(binding: Binding<*>) {
        bindings += binding
    }

    @PublishedApi
    internal fun komok(): Komok {
        return DefaultKomok(
            modules = modules.toList(),
            env = env.toMap(),
            args = args.toTypedArray(),
            props = props.toMap(),
            bindings = bindings.toList(),
        )
    }
}

@PublishedApi
internal interface Komok {
    suspend fun <T : EntryPoint<R>, R> run(
        source: String,
        type: GenericKey<T>,
    ): R
}

@KomokDsl
internal class DefaultKomok(
    private val modules: List<ModuleProvider>,
    private val bindings: List<Binding<*>>,
    private val env: Map<String, String>,
    private val args: Array<String>,
    private val props: Map<String, String>,
) : Komok {
    override suspend fun <T : EntryPoint<R>, R> run(
        source: String,
        type: GenericKey<T>,
    ): R {
        val komokModule by module {
            provide<KomokEnv>({ DefaultKomokEnv(env) })
            provide<KomokProps>({ DefaultKomokProps(props) })
            provide<KomokArgs>({ DefaultKomokArgs(args.toList()) })
        }

        val komokModuleProvider = object : ModuleProvider {
            override fun module(): Module {
                return object : Module {
                    override val source = source
                    override val dependencies = modules + komokModule
                    override val bindings = this@DefaultKomok.bindings
                }
            }
        }

        return createContextAndGet(type, komokModuleProvider).run()
    }
}

/**
 * Interface to access environment variables in komok.
 *
 * @author Ruslan Ibrahimau
 */
interface KomokEnv {
    val env: Map<String, String>
}

/**
 * Default implementation of [KomokEnv].
 * Which just data class with read-only map of environment variables.
 *
 * @author Ruslan Ibrahimau
 */
internal data class DefaultKomokEnv(
    override val env: Map<String, String>
) : KomokEnv

/**
 * Interface to access jvm properties in komok.
 *
 * @author Ruslan Ibrahimau
 */
interface KomokProps {
    val properties: Map<String, String>
}

/**
 * Default implementation of [KomokProps].
 * Which just data class with read-only map of properties variables.
 *
 * @author Ruslan Ibrahimau
 */
internal data class DefaultKomokProps(
    override val properties: Map<String, String>
) : KomokProps

/**
 * Interface to access command line arguments in komok.
 *
 * @author Ruslan Ibrahimau
 */
interface KomokArgs {
    val args: List<String>
}

/**
 * Default implementation of [KomokArgs].
 * Which just data class with read-only list of arguments.
 *
 * @author Ruslan Ibrahimau
 */
internal data class DefaultKomokArgs(
    override val args: List<String>
) : KomokArgs
