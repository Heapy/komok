# Komok Runtime: FIR + IR

Komok combines compile-time wiring with a runtime dependency graph, memoization,
production decorators, explicit mocks and persistent forks. The compiler plugin targets
Kotlin **2.4.21 / K2**. The runtime and current integration support **JVM**; other
backends are not validated.

## Modules and graph reads

```kotlin
@Module
class ConfigurationModule {
    context(_: KomokContext)
    suspend fun url(): String = "jdbc:production"
}

@Module
class DatabaseModule(private val configuration: ConfigurationModule) {
    context(_: KomokContext)
    suspend fun database(): Database = Database(configuration.url())
}

val graph = graph<DatabaseModule>()
val database = graph { database() }
val sameDatabase = graph[DatabaseModule.Bindings.database]
val url = graph.read { module<ConfigurationModule>().url() }
```

FIR generates the nested `Bindings` object with a `Binding<T>` for each provider.
IR instruments provider bodies in place and emits graph construction at `graph<M>()`.
Class modules remain final: there is no generated module subclass and no all-open
plugin. Interface modules receive a generated implementation of their default methods.

Class providers are public suspend non-Unit methods with exactly one
`context(_: KomokContext)` parameter and no value parameters. Private functions,
non-suspend functions, Unit functions and class methods with value parameters remain
ordinary helpers. Interface providers can take value parameters; the graph resolves
these by exact produced type, including generic arguments and nullability. Use a
binding read (`graph[Module.Bindings.provider]`) to invoke a provider with autowired
parameters. An ordinary Kotlin call still takes its declared arguments.

Constructor parameters of a module must reference other modules. A module type is
instantiated once per graph, including when multiple paths in a diamond reach it.
Bindings are identified by module FQN, provider name and parameter descriptor.
Modules and providers must have concrete types; overloaded provider names within a
module are rejected.

## Realization and forks

A provider read enters the runtime boundary, records the caller edge, checks for a
cycle and obtains the realm's memoized value. Self-calls and calls across modules enter
the same boundary. Autowired argument resolution happens inside the provider boundary,
so these dependencies also produce edges. Actual execution determines the graph,
including conditional dependencies.

Realizations belong to the realm's coroutine scope. Cancelling a waiting caller does
not cancel shared work. Provider failures, cycle detection and concurrent reads retain
the runtime's existing cell semantics. `KomokContext` flows lexically; it is not a
thread-local or a coroutine-context service locator.

```kotlin
val fork = graph.fork {
    replace(ConfigurationModule.Bindings.url, "jdbc:test")
    replaceWith(DatabaseModule.Bindings.database) {
        Database("test:" + module<ConfigurationModule>().url())
    }
}
val testDatabase = fork { database() }
fork.stop()
graph.stop()
```

A fork snapshots the known dependency graph. Replaced bindings and their reverse
transitive dependents are recomputed in the child realm. Already-realized values outside
that cone are shared by reference. `realizeAll()` can establish the graph before a fork;
late discoveries cannot retroactively expand an existing fork's snapshot.

`replace(binding, value)` pins a terminal value and skips production decoration.
`replaceWith(binding) { ... }` runs lazily in the fork's context and retains production
decoration. Calls inside that body record edges against the replaced binding. Do not
capture a provider context for later graph reads from a returned service.

## Services and sets

```kotlin
@Service fun settings(): Settings = Settings("production")
@Service class Client(val settings: Settings)

@Service @IntoSet fun jsonCodec(): Codec = JsonCodec()
@Service @IntoSet fun xmlCodec(): Codec = XmlCodec()
@Service class Registry(val codecs: Set<Codec>)

val graph = services()
val client = graph.resolve<Client>()
val fork = graph.fork {
    replace(binding<Settings>(), Settings("test"))
    replace(binding(::jsonCodec), YamlCodec())
    replaceWith(binding<Client>()) { Client(provider<Settings>()) }
}
```

`services()` wires visible `@Service` functions and constructors. Module graphs also
include these bindings. `@Service(bind = Interface::class)` or `@Service(Interface::class)`
exposes a provider under that interface. `@ProvidersFrom(Providers::class)` imports
public no-argument value functions from an object in an ordinary dependency.

`binding<T>()` selects a unique binding by exact type; `binding(::provider)` selects a
specific service function, including an individual set contribution. Set aggregation
preserves contribution order and participates in memoization and fork invalidation.
A whole set can be replaced with `replace(binding<Set<Codec>>(), replacement)`.

The compiler emits a versioned `@KomokBindingIndex` carrier into bytecode. Downstream
compilations read these carriers from their compile classpath, including dependency
JARs. Reflection and classpath scanning are not used at runtime. Service binding owners
are `<package>.Services`; sets use `komok.sets`. Missing and ambiguous autowired
dependencies and static dependency cycles fail compilation.

## Lifecycle and introspection

`graph.start()` executes module `@Startup` hooks in dependency order. Service-class
hooks follow their static dependency order. `graph.stop()` executes shutdown hooks in
reverse order and closes the runtime. A failed startup compensates completed owners;
a later stop does not repeat that compensation. Starting/stopping a fork does not run
the root's lifecycle hooks; stopping it releases only its overlay.

Module hooks are public suspend Unit functions with no value parameters. Hooks can
read providers through `KomokContext`. The graph options retain `dispatcher` and
`rootContext` configuration. `graphPayload()` combines static module/binding descriptors
with observed binding edges and can serialize them with `toJson()`.

## Decorators

Interfaces returned by providers can use `@Log`, `@Metric` and `@Retry`. IR emits a
produced-value wrapper and delegates every member, including properties and interface
defaults, to the actual implementation. The production order is Log → Metric → Retry →
delegate. Logging and metrics use the context parameters supplied by the caller.
Method metadata is initialized once in static fields. Decorator implementations are
reused within a compilation.

A custom wrapper is an ordinary library containing an annotation and a helper:

```kotlin
@CallWrapper("example.auditCall", order = 250)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Audit

context(trail: AuditTrail)
suspend fun <T> auditCall(meta: MethodMeta, call: suspend () -> T): T {
    trail.record(meta)
    return call()
}
```

The helper has `meta` and `call` parameters, one result type parameter and any required
context parameters. Annotation arguments map by parameter name; helper defaults must
match the annotation's defaults. The decorated method supplies matching context
parameters. Decorated methods must be suspend; generic method declarations are not
supported. There is no KSP processor, KotlinPoet dependency or wrapper ServiceLoader SPI.

## Explicit mocks and spies

```kotlin
val double = mock<Service>()
double.every { load("") }.answers { call -> "mock:${call.arguments[0]}" }
double.every { count() }.returns(42)
val service: Service = double.value
val result = service.load("item")
check(double.calls { load("") }.single().arguments == listOf("item"))

val spy = spy<Service>(RealService())
spy.every { count() }.returns(7)
```

Only `mock<T>()` and `spy<T>(real)` requests generate doubles. Merely returning or
decorating an interface does not generate a mock. External interfaces, generic interface
instantiations, properties, Unit methods and suspend methods are supported. Selectors
identify one member and are never executed; their arguments are placeholders, not
argument matchers. Use `this.property` when a local variable shadows a property.
`coAnswers` accepts a suspend answer. Spies delegate unstubbed members, including
implementation overrides of default methods. Call logs are per-instance and concurrent.
Mock targets must be interfaces; generic method declarations are not supported.

## Build and migration

Apply `//templates/runtime.module-template.yaml` to source and test consumers. The
repository's Kotlin Toolchain 0.13 integration builds the local compiler JAR first and
fingerprints it to invalidate consumers. Because Toolchain 0.13 cannot express a local
compiler-plugin dependency, the template passes the JAR through `-Xplugin` and disables
incremental compilation for these modules. Run the wrapper from the repository root
with the default `build` directory. Published consumers can configure compiler plugin
ID `io.heapy.komok.runtime` and the `io.heapy.komok:komok-tech-runtime-compiler` artifact
using their build tool's compiler-plugin support. Compiler and Kotlin versions must
match; arbitrary Kotlin versions and IDE integrations are not validated.

The former generated `createXGraph`, `XGraph`, `XKeys`, module fork DSL and mock factories
are replaced by the APIs above. `@GenerateMock` and runtime KSP/all-open configuration
are removed. This is a source/API migration; graph realization, fork, lifecycle and
wrapper behavior are covered by migrated integration tests.

```shell
./kotlin build --platform jvm
./kotlin test --platform jvm
./kotlin check apiCheck klibApiCheck
./kotlin do distTar -m komok-app
```
