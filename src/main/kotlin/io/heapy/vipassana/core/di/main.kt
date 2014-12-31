package io.heapy.vipassana.core.di

@DslMarker
internal annotation class ApplicationDSL

@ApplicationDSL
suspend inline fun <reified T : EntryPoint<R>, R> application(
    noinline builder: ApplicationBuilder.() -> Unit,
): R =
    DefaultApplicationBuilder()
        .apply(builder)
        .instantiate()
        .run(builder::class.toString(), type<T>())

@ApplicationDSL
interface ApplicationBuilder : Binder {
    @ApplicationDSL
    fun args(args: Array<String>)

    @ApplicationDSL
    fun env(env: Map<String, String>)

    @ApplicationDSL
    fun props(props: Map<String, String>)
}

interface EntryPoint<out R> {
    suspend fun run(): R
}

@PublishedApi
internal class DefaultApplicationBuilder : ApplicationBuilder {
    private val modules = mutableListOf<ModuleProvider>()
    private val bindings = mutableListOf<Binding>()
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

    override fun contribute(binding: Binding) {
        bindings += binding
    }

    @PublishedApi
    internal fun instantiate(): Komodo {
        return DefaultKomodo(
            modules = modules.toList(),
            env = env.toMap(),
            args = args.toTypedArray(),
            props = props.toMap(),
            bindings = bindings.toList()
        )
    }
}

@PublishedApi
internal interface Komodo {
    suspend fun <T : EntryPoint<R>, R> run(
        source: String,
        type: GenericType<T>,
    ): R
}

internal class EnvMap(val env: Map<String, String>)
internal class PropMap(val props: Map<String, String>)
internal class ArgList(val args: Array<String>)

@ApplicationDSL
internal class DefaultKomodo(
    private val modules: List<ModuleProvider>,
    private val bindings: List<Binding>,
    private val env: Map<String, String>,
    private val args: Array<String>,
    private val props: Map<String, String>
) : Komodo {
    override suspend fun <T : EntryPoint<R>, R> run(source: String, type: GenericType<T>): R {
        val komodoModule by module {
            provide({ EnvMap(env) })
            provide({ PropMap(props) })
            provide({ ArgList(args) })
        }

        val komodoModuleProvider = {
            object : Module {
                override val source = source
                override val dependencies = modules + komodoModule
                override val bindings = this@DefaultKomodo.bindings
            }
        }

        return createContextAndGet(type, komodoModuleProvider).run()
    }
}
