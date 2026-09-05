# Komok Runtime

A single unified runtime model for Kotlin applications:

- top-level provider functions, module classes, module interfaces and annotated classes with primary constructors — the core of Dependency Injection / IoC
- compile-time metadata and checks plus a runtime dependency graph
- built-in compile-time call decoration, examples: `@Log`, `@Metric`, `@Retry`, `@Cached`, `@Auditable`
- built-in compile-time mock/spy without reflection
- built-in compile-time **persistent fork** (tests/dev)
- support for generating code for any target, i.e KMP-ready

This covers authoring the code and testing it: mocks help with unit testing, and a persistent fork helps with integration testing.

## BOM

Modules in scope:

```
include("komok-tech:komok-tech-runtime")
include("komok-tech:komok-tech-runtime-ksp")
include("komok-tech:komok-tech-runtime-test")
include("komok-tech:komok-tech-runtime-test-decorator")
include("komok-tech:komok-tech-runtime-decorators")
```

More modules may be added for tests, and decorators may be split into per-decorator modules.

---

## Key idea

1. **A binding is a provider function**. Calling a dependency is an ordinary Kotlin call. There are three ways to contribute a binding: a provider function in a `@Module` class, a method on a `@Module` interface, or an annotated `@Service` class/function.
2. **KSP generates a subclass** that overrides each provider and wraps its body in the runtime boundary `rt.binding(id) { super.x() }`: memoization, edge recording, caller-stack.
3. **Self-calls are caught via virtual dispatch.** The body runs through `super.clock()`, and `timeSource()` inside it is `this.timeSource()` on the generated subclass → it hits the override → the edge `clock → timeSource` is recorded. This is **not** the Spring self-invocation problem.
4. **"Decorator" spans several non-mixing layers** — realization/memoization, produced-value decoration, service-method calls (`@Log`/`@Metric`/`@Retry`), and test replacement (`@Mock`/`@Spy`); §Decorators is the authority.
5. **The dependency graph is built at runtime** (KSP does not interpret bodies). This yields an accurate graph for conditional/branchy providers; the cost is that the graph only exists for what actually executed.
6. **Coroutines-first.** Module providers are `suspend` and `binding` is the single primitive; concurrency-safe memoization becomes a real non-blocking `await`. `suspend` is only paid on graph **realization** (bootstrap + lazy service creation); calls on already-built services keep their own color. The only blocking bridge is `runBlocking` in `main`.

---

## User model

```kotlin
@Module
open class UtilsModule {
    context(_: KomokContext)
    open suspend fun timeSource(): TimeSource = SystemTimeSource()

    context(_: KomokContext)
    open suspend fun clock(): Clock = RealClock(timeSource())
}

@Module
open class JdbcModule(
    private val configModule: ConfigModule,
) {
    context(_: KomokContext)
    open suspend fun dataSource(): DataSource = HikariDataSource(configModule.jdbcUrl())
}

@Module
interface MetricsModule {
    context(_: KomokContext)
    suspend fun meterRegistry(
        timeSource: TimeSource, // dependency via parameter, auto-resolved by type in compile-time
        tags: Tags,
    ): MeterRegistry = MeterRegistry(timeSource)

    context(_: KomokContext)
    suspend fun metricTags() = Tags("app" to "komok")
}

// Contribute via a provider function → generated com.foo.bar.BarModule
// package: com.foo.bar
@Service
fun postgresUserDao(dataSource: DataSource): UserDao = PostgresUserDao(dataSource)

// Contribute under an interface, but direct call giving actual type (maybe useful)
@Service(UserDao::class)
fun postgresUserDao(dataSource: DataSource) = PostgresUserDao(dataSource)

// Contribute via an implementation class → generated com.foo.bar.BarModule
// package: com.foo.bar
@Service
class PostgresUserDao(private val dataSource: DataSource) : UserDao

// Contribute under an interface
@Service(UserDao::class)
class PostgresUserDao(private val dataSource: DataSource) : UserDao

// Module composition
@Module
open class AppModule(
    private val utils: UtilsModule,
    private val jdbc: JdbcModule,
    private val metricsModule: MetricsModule,
)

// Decoration is part of DI
@Mock   // generates Mock and Spy in the testFixture source for the module's services if "mocks by default" disabled
@Module
class SessionModule {
    @Log   // logs calls to SessionManager's open methods via a generated decorator
    context(_: KomokContext)
    suspend fun sessionManager(): SessionManager = DatabaseSessionManager()

    @Log     // requires context(_: LoggingContext) on the methods
    @Retry   // chain: Log applied first, then Retry; requires context(_: RetryContext) on the methods
    context(_: KomokContext)
    suspend fun ragService(): RagService = RagService(sessionManager())
}

// Decorating a single method
class PaymentService {
    @Auditable   // like @Log but into the DB; marked @Decorator
    context(_: TransactionalContext)
    suspend fun processTransaction(transaction: Transaction): TransactionResult { /* ... */ }

    @Log   // works with both suspend and non-suspend
    fun checkBalance(user: User): Balance { /* ... */ }
}
```

- The `all-open` plugin opens provider functions and `@Module` classes automatically; the user writes `suspend` and `context(_: KomokContext)` on class providers.
- There is no manual memoization — it lives in the runtime.
- A cross-module dependency is a call like `configModule.jdbcUrl()` on an injected reference; the reference is a generated subclass, so the call is virtual and the edge is recorded across modules.
- **Module identity = module type within a single root graph.** One `@Module` class = one node, one subclass instance, one binding namespace, one lifecycle owner.

---

## Public root API

The user works with neither the raw module, nor the generated subclass, nor `KomokContext` directly. KSP generates a graph handle per module:

```kotlin
suspend fun main() {
    val root: AppModuleGraph = createAppModuleGraph {   // generated builder, type-safe, not suspend (wiring only)
        utilsModule {
            replaceClock { GpsClock(gpsModule.gpsService()) }   // replace a service before start
            replaceTimeSourceUndecorated { MyTimeSource(utilsModule.timeSource()) }   // drop all decorators
        }
    }

    root.komokApplication().run()   // suspend: graph realization happens here
}

// in a test (runTest / runBlocking):
val test: AppModuleGraph = root.fork {
    utilsModule {
        replaceTimeSource { FixedTimeSource(t) }
    }
    userServiceModule {
        mockUserService { /* mock behaviour, decorators on top are kept */ }
        spyOnUserService {}
        mockUserServiceUndecorated { /* drop decorators */ }
    }
}

val testClock = test.modules.utilsModule.clock()   // suspend: graph read
```

Using `replace*` functions is not compile-time safe, so it's not expected to see a lot of such replacing happen for production graph. But, it's super useful for unit tests, and runtime failure in case of unit test is fine.

- `create${X}Graph { }` assembles the runtime, the root handle, and a DAG of unique module instances; it returns a generated `${X}Graph`, not a raw module. A module type seen on several constructor paths is interned to a single instance.
- Public provider calls (`root.modules.utilsModule.clock()`) require no user-visible `KomokContext` — the handle opens the needed context itself.

---

## Two-level graph

| Level | Known when | Source | What it provides |
|---|---|---|---|
| **Module** (module → module) | compile-time | `@Module` constructor parameters, canonicalized by module type | cycle detection, instantiation order, **lifecycle order without realization** |
| **Binding** (function → function) | runtime | edges recorded while providers execute | the precise cone for fork, introspection |

The module graph is a DAG of unique module types (a repeated reference to the same class = the same node), not a tree of constructor paths. The binding graph refines it. **The structural source of truth is the module graph** (complete, compile-time); binding edges are layered on as facts of execution (run-dependent for branchy/lazy code).

---

## Runtime behaviour

`rt.binding(id) { super.x() }` is the only primitive; inside it: record the edge `caller → id`, check for a cycle, memoize, await any parallel computation.

- **Context — one source of truth: only the Kotlin `context` parameter.** `KomokContext` (realm + caller-stack) lives as a context parameter and propagates **lexically** — captured by the lambda passed to `binding`/`launch`/`async`, surviving suspend-resume.
- **Concurrency-safe memoization.** Parallel realization of one binding = one computation; the remaining readers do a non-blocking `await`. The computation runs on the **realm scope** (`SupervisorJob`), not on the caller's coroutine.
- **Cancellation isolation.** Cancelling a reader cancels only its `await`, not the computation itself (it continues for the others). You cannot time out the creation of a binding from a single request; aborting a stuck realization requires closing the realm. A single binding's failure does not cascade to its siblings (`SupervisorJob`).
- **Failure is memoized terminally** (determinism): the exception is stored in the cell and re-thrown to all current and future readers. By design, realization is non-fallible with respect to recoverable I/O — recoverable I/O lives *inside* the produced value (a self-healing pool/client) or in a method-level `@Retry`, not in realization itself. A realization failure is therefore either a deterministic error (config/code) or a startup infra failure (fail-fast → restarted by a supervisor).
- **The realization context is framework-owned.** The realm scope is built by the runtime with an explicit `CoroutineContext`: a dispatcher (default `Dispatchers.IO`, overridable at runtime creation) + an always-present root `LoggingContext` (context parameter to most of the functions, since this is how komok-logging works). A provider body runs under this context, **not** the caller's: a binding is realized once and shared by N readers, so per-request MDC/trace is **not** carried into the provider body — realization logs/traces belong to "the binding's realization", not to the first reader. The root `LoggingContext` is mandatory (otherwise a logging provider would fail).
- **`BindingId`** = `(moduleFqn, providerSignature)` within a given runtime; for an exported `/graph` it is `(rootGraphId, moduleFqn, providerSignature)`. `providerSignature` = name + value-params descriptor, **without** the return type: the return type is binding metadata, and widening the result type (`HikariDataSource` → `DataSource`) must not silently change the address and break fork/`/graph`. Call-wired module-class providers take no value parameters (dependencies are virtual self-calls); param-wired providers (interface methods, `@Service` functions/classes) carry their dependencies as value parameters, which are part of the signature.
- **Cycles:** a module cycle → compile-time error (topological sort); a binding cycle (a reentrant read in the same logical context) → `KomokCycleException(path)` at runtime. A read from another coroutine/context is a wait, not a cycle.

---

## Generated code

```kotlin
class __KOMOK__UtilsModule__(private val rt: KomokRuntime) : UtilsModule() {
    context(_: KomokContext)
    override suspend fun clock(): Clock =
        rt.binding(UtilsModuleKeys.clock) { super.clock() }
}
```

- Per `@Module`, KSP generates: a subclass `__KOMOK__${Module}__`, an `object ${Module}Keys`, a fork scope, and a runtime registration.
- `private`/`final` providers are helpers: not bindings, not memoized, no `BindingId` (but virtual calls inside them still record edges).
- Generated `__KOMOK__*__` and DAG containers are an internal implementation detail; using them directly = deliberately bypassing the runtime API.

---

## Decorators (`@Log` / `@Metric` / `@Retry` / etc)

The term **decorator** denotes three distinct layers:

| Layer                   | What it wraps                         | Examples                                            | Where it runs                |
|-------------------------|---------------------------------------|-----------------------------------------------------|------------------------------|
| **Binding execution**   | a provider-body realization           | `rt.binding`, a future provider-level retry/metric  | during graph realization     |
| **Produced value**      | the value returned by a provider      | generated `UserService_KomokDecorated(delegate, …)` | once, at binding realization |
| **Service method call** | a concrete call on the produced value | `@Log`, `@Metric`, `@Retry`                         | on every service method call |

`@Log`/`@Metric`/`@Retry` are **service method-call decorators**. Inside `binding`, the runtime gets the raw value and wraps it in a generated value decorator:

```kotlin
override suspend fun userService(): UserService =
    rt
        .binding(
            key = Keys.userService,
            decorator = UserService_KomokDecorated.DESCRIPTOR, // actual access method and shape TBD
        ) {
            super.userService()
        }

// the decorated method declares its decorators' contexts; the decorator reproduces them:
interface UserService {
    @Log @Metric
    context(_: LoggingContext, _: MeterRegistry)
    suspend fun login(request: LoginRequest): LoginResponse
}
```

- **The decorator's context is the decorated method's context parameters, supplied by the caller** — not static, not from the graph (exactly as providers receive `KomokContext`). The canonical context-parameter order = the chain order (`@Log` → `LoggingContext`, `@Metric` → `MeterRegistry`). Only the **identity** stays static — `logger(fqn)`: that is data, not context. Consequence: context is not part of the binding graph and has no cone effects (swapping a logger/registry is an argument change at the call site, not a fork).
- **Method-call decorator order is deterministic, outer → inner:** `@Log` → `@Metric` → `@Retry` → `delegate.method(...)`. One public call is logged once; the metric measures the full latency including retries; `@Retry` repeats only the delegate call.
- **No runtime reflection — anywhere.** No generated code (decorators, mocks, metadata) uses `kotlin.reflect`/`KCallable`/`KFunction`/`KClass`/ByteBuddy. Method/parameter metadata is a generated static object (`MethodMeta`: owner, name, parameters, returnType, isSuspend) passed to decorator helpers. Service methods can be `suspend` → suspend variants of the decorators are generated.
- **Decorator kinds are plugins (`DecoratorPlugin` SPI), including the built-ins.** The KSP core knows no concrete decorator: each kind is a `DecoratorPlugin` (annotationFqn, `order`, contextParameter, decorator properties, helper call, validate), discovered via `ServiceLoader` on the ksp classpath. The built-in `@Log` (order 100) / `@Metric` (200) / `@Retry` (300) are registered through the same mechanism with no special status. A new decorator = an annotation + a runtime helper on the compile classpath + a plugin jar in `ksp(...)`, **with no core changes**. Chain order = ascending `order`; two plugins claiming one annotation is a KSP error. Helper contract: `context(C)? suspend fun <T> helper(...config, call: suspend () -> T): T`.
- **Decoratable = mockable:** the type must be an interface or an open class with an overridable API. Not decoratable without a compiler plugin: `final` classes, `final`/`private`/`inline`/`reified` methods. Codegen must reproduce exactly: `vararg`, default values, generic parameters and variance, receiver and `context(...)` parameters of the methods, nullable/platform types.

---

## Mock / Spy

- Generated ahead of time by KSP for mockable service types into `testFixtureClasspath`: `FooMock : Foo` (Spy and Mock same class, it's just different default answer strategy).
- **Service type** = a provider return type that is an interface or an open class with an overridable public API; optionally `@Mockable` to opt in explicitly.
- **Spy** = a decorator over a real delegate (default `proceed`); **Mock** = no delegate (stub/default). This is the "conditional/never proceed" edge of the decorator spectrum.
- **External types** (from dependencies): KSP cannot see them, so they need an explicit opt-in — `@GenerateMock(Connection::class)` on a test element, and KSP generates a double for the resolvable `KSType`. `final`/sealed external types and types with no overridable members are not doubled in the no-reflection model — a deliberate limit.
- **Reflection-free DSL:** type-specific factories `fooMock()` / `fooSpy(real)` and typed factory objects `mock(FooMockFactory)`, with no `T::class`/`KClass` lookup. Stub/verification state is per-instance and parallel-safe (`every { } returns`, `verify { }`); for `suspend` methods, suspend stubs (`coEvery`-style).
- **Mock/Spy is a value for fork replacement:** `fork { fooModule { replaceFoo { fooMock() } } }`. Creating a double and overriding it in the graph are unified.
- `replaceYUndecorated { value }` is a terminal value override: no production value decorators are layered over an explicit mock/spy. `replaceY { ... }` replaces the raw provider body and then goes through normal production decoration. `spy(real)` is itself the outermost test decorator over the `real` passed in.

---

## Persistent fork

```kotlin
val test = root.fork {
    utilsModule { replaceTimeSourceUndecorated { FixedTimeSource(t) } }
    jdbcModule { replaceDataSource { TestDataSource(configModule.config()) } }
}
```

- **The fork scope is generated** and statically typed by the unique module types: per `@Module` a `${X}ForkScope` with `replaceYWith { }` / `replaceYUndecorated { }`. A replacement is keyed by the binding's `BindingId`, not the constructor path (paths are display aliases only).
- **`fork { ... }` is not suspend and does not read the graph** — the configure lambda only collects override descriptors.
- `replaceYWith { }` sets a new suspend body that reads other (forked) bindings through a generated provider scope, recording runtime edges while the override realizes. The provider scope calls generated module instances directly under the current `KomokContext`, so `configModule.config()` inside the replacement sees the fork realm and records the edge `JdbcModule.dataSource → ConfigModule.config`.
- **Default: Partial-share from a partially realized parent, fork never touches root**. In fork, when replacement happens, we need to invalidate all graph nodes that dependent on replacement and re-calculate. All parent-registered lifecycle methods removed, and only new realised lifecycle methods registered.
- **Correctness invariant (hard):** memoization is strictly per-realm; a parent value is shared **only** if it is completed and the binding is not in the cone. Never serve a parent value for a cone node; never compute a parent miss from the child realm.
- **The cone is fixed by the edge set at fork time.** If the parent later lazily realizes new edges, the fork's cone may go stale — this is **accepted** (forks are short-lived, test/request); there is no separate freeze/recompute.
- **Closing a realm** runs shutdown/close only for values realized in that realm's overlay — not for parent-shared (by-reference) values. `fork.close()` closes resources in the fork's cone (e.g. a re-realized `DataSource`) but does not touch a parent-owned resource.

### Two correctness preconditions (not enforceable by code shape)

1. **The model condition:** a provider is deterministic with respect to graph reads, and all graph reads happen during the realization of its body. External state that affects a branch/dependency choice must be its own binding (`config()`, `env()`, `clock()`, feature flags) — otherwise the runtime cannot know what to invalidate.
2. **No late graph read:** service values do not hold module refs, provider lambdas, a `RealmHandle`, or a `KomokContext` to read the graph later. All dependencies are materialized inside the provider body: `UserService(config(), clock())` is correct; `UserService(configModule)` or `UserService { configModule.config() }` violates the model (the fork won't see the edge at realization time).

These are body semantics — KSP/lint cannot prove them for an arbitrary body. They rest on **discipline + code review (incl. LLM)**. **The target structural enforcement is Kotlin Local Lifetimes:** a `local` modifier on `KomokContext`/`RealmHandle` guarantees the value cannot escape the provider body, making "no late graph read" a compiler guarantee (a module ref captured without a context is inert — it cannot call a `context(_: KomokContext)` provider). The feature is research-stage; until it ships — discipline + review, with a KSP/detekt type check as an optional temporary bridge.

---

## Lifecycle (`@Startup` / `@Shutdown`)


- Each hook is registered together with its canonical `ModuleId`, `phase`, and function reference, so order is computed **without realizing** providers: the registry sorts hooks by the static module DAG + `phase`.
- `@Startup`/`@Shutdown` are `suspend` functions and run as suspend passes on the realm scope.
- **Startup:** forced realization of the needed providers, then hooks in `(module-topo dependencies-first, phase)` order. **Partial failure is compensated:** a failed hook → the already-run `@Startup` hooks are rolled back by a reverse `@Shutdown` pass, then boot fails (fail-fast).
- **Shutdown:** strictly reverse order, graceful, with a `join` on each hook. The root `runBlocking` in `main` spans both startup and the wait for shutdown.

---

## Introspection

- Runtime API on `Realm`/`KomokRuntime`: `modules()`, `bindings()`, `edges()`, `decoratorsOf(id)`, `isShared(id, otherRealm)`. The full structure comes from the static module graph; binding edges are facts of execution.
- A `/graph` endpoint next to `/metrics` feeds `komok-tech-di-ez-graph-visualizer`.

---

## Free-floating providers and services (param-wiring)

`@Module` classes with call-wiring are one style; free-floating providers are a second, complementary style over the **same** runtime (everything reduces to `rt.binding(id) {}`; memoization/fork/cone/introspection are unchanged).

### Two wiring styles

| | Module providers (call-wiring) | Free-floating (param-wiring) |
|---|---|---|
| Dependencies | plain calls `timeSource()` — virtual dispatch | function/constructor parameters — codegen makes the calls |
| Dynamism | branchy/lazy (edges = facts of execution) | static: all parameters realized before the body |
| Ceremony | `open class` + `context(_: KomokContext)` + `suspend` | none: an ordinary top-level function / class |
| No late graph read (#6) | discipline + review | **by construction** — the body has no access to the graph |

Call-wiring between top-level functions is impossible in principle: a top-level function cannot be overridden, and calling a neighbour would bypass the binding boundary — the very self-invocation hole the class model was chosen to close.

```kotlin
@Service(DataSource::class)   // optional: when a function returns a concrete type but should bind under an interface
fun dataSource(config: JdbcConfig): DataSource = HikariDataSource(config.url)  // suspend optional

@Service
class UserService(private val db: DataSource, private val clock: Clock)

@Service(bind = UserApi::class)   // bound under an interface → decoration applies as usual
class RealUserApi(/* deps */) : UserApi
```

- `@Service` on a **function**: the function name = the binding name; `suspend` allowed; no `context`/`KomokContext`; value parameters = dependencies.
- `@Service` on a **class**: the constructor = dependencies; the binding name = the decapitalized simple name; `bind`/`@Service(Iface::class)` raises the binding type to an interface.

### Interface modules (a third source of param providers)

```kotlin
@Module
interface TimeSourceModule {
    context(_: KomokContext)
    suspend fun clock(): Clock = Clock.systemDefaultZone()               // no-arg ⇒ leaf
    context(_: KomokContext)
    suspend fun timeSource(clock: Clock): TimeSource = TimeSource(clock)  // clock — a param dependency
}
```

- **Interface-module methods are always param-wired.** A no-arg method = a leaf provider, **not** a call-wired self-call (call-wiring stays exclusive to `@Module open class`). This removes the ambiguity between "calling a neighbour" and "declaring a dependency".
- **Ceremony like class modules:** each method declares `context(_: KomokContext)` and `suspend`.
- **Identity:** the module part of the `BindingId` is the interface FQN; an interface has no constructor → codegen creates `object : M {}` to invoke default bodies.
- **Resolution — the same exact-type index as `@Service`:** a `Clock` parameter resolves to any `Clock` binding in the graph (including a neighbouring method of the same interface).

### Synthetic module

`@Service` functions and `@Service` classes are realized through one **synthetic module per compilation unit**:

- The FQN is set by the KSP arg `komok.providers.module=com.example.AppServices` (required when free-floating declarations exist) and is the module part of every free binding's `BindingId`, so moving a declaration between files does not change identity.
- It participates as an ordinary module: `${X}Keys`, `${X}GraphApi`, `${X}ForkScope` (`fork { appServices { replaceUserService(mock) } }`), a module descriptor in `/graph`, interning. Its constructor dependencies are **inferred** from parameter types.
- `@Startup`/`@Shutdown` on `@Service` class methods are hooks of the synthetic module; order is topological by the param graph, then declaration order.

### Type resolution (compile-time, exact-match)

- Type index: `{canonical produced type → binding address}`. Canonicalization: nullability, expanded typealiases, whole generics (`List<String>` ≠ `List<Int>`).
- **Exact match only**, no subtype selection: contributing under a supertype = declare the supertype as the return type (or `bind=`). No qualifiers: "two bindings of one type = declare distinct types".
- 0 candidates → an error with the dependency chain; ≥2 → an error with both addresses.

### Cross-compilation resolution (binding index)

KSP discovery is compilation-local (it does not scan jars), so every unit where our KSP ran **publishes an index** — a generated carrier class `__KOMOK__Index__` in the well-known package `komok.index`, carrying a `@KomokBindingIndex` annotation (bindings / setContributions / setConsumers).

- Retention **BINARY**; downstream reads `Resolver.getDeclarationsFromPackage("komok.index")` — the only KSP API that sees declarations from jars without a source reference (precedents: Hilt, Anvil, kotlin-inject-anvil).
- The merged index = classpath indices + the current compilation; free-floating declarations' dependencies are taken from anywhere on the classpath where our KSP ran.
- Consequence — **implicit graph enlargement:** the synthetic module's inferred refs pull upstream modules into the graph even if the root never named them. `/graph` shows this honestly.
- The `komok.index` package and the annotation format are a **binary contract** (like the `BindingId` format): change only with a migration.

### Multi-round processor

- A symbol that hits an error type (a reference to something not yet generated) is not a round error but **deferred**: handled in the next round once the generated code is visible. Each validation distinguishes "invalid" from "not-yet-resolvable" (`KSType.isError`).
- Generation is idempotent across rounds; the final round reports unresolved symbols with a human-readable error.
- This lifts the restriction: `@Module open class AppModule(services: AppServices)` is valid in the same compilation where `AppServices` is generated.

---

## Multibindings — `Set<T>`

```kotlin
@Service @IntoSet
fun jsonCodec(): Codec = JsonCodec()                 // free contribution

@Module
open class HttpModule {
    @IntoSet
    context(_: KomokContext)
    open suspend fun httpCodec(): Codec = HttpCodec() // module contribution
}

@Service
class CodecRegistry(codecs: Set<Codec>)              // consumer — param-wired
```

- **Contribution** — `@IntoSet` on any provider (module or free). The element type = the declared return type (exact). A contribution stays an ordinary binding: its own identity, memoization, and individual replacement in a fork.
- **Consumption** — a `Set<T>` parameter in a param-wired declaration. Resolution: an exact `Set<T>` binding AND contributions → an error; only a binding → ordinary resolution; only contributions → an aggregate. Module providers (call-wired) cannot consume aggregates — they have no name to call.
- **Aggregation is per-root** (extension-point semantics): the set "closes" when the root graph is generated against the final application's merged index. A library declares a `Set<Codec>` consumer; the application adds contributions without rebuilding the library.
- **Decoupled from the root:** a compilation unit with Set consumers generates `interface ${X}Aggregates`; the synthetic module receives it via the constructor. Root codegen generates `__KOMOK__${Root}Aggregates__` implementing the needed interfaces of the reachable set.
- **Cone/fork for free:** an aggregate calls the element providers → edges `set → element` are recorded → replacing an element in a fork recomputes the set and its consumers; `replaceCodecSet(value)` for the whole set is an ordinary ForkScope.
- Determinism: element order by binding address; the empty set is allowed; duplicates by `equals` collapse via `Set` semantics. The aggregate's BindingId is **per-root identity**.
- `Map<K, V>` is a symmetric extension for later.

---

## Future decorators ideas

We don't need to implement these decorators at the moment, but keep in mind that our infrastructure is designed to support them in the future:

1. Client generation. Feign/Retrofit like annotations, and komok-runtime decorator that generates an actual client and wires it into DI
2. Method security. Support different annotations like `@RequireRole`, `@RequirePermission`, etc. to move permission checks into the wrapper, removing the need to have manual check in code.
   ```kotlin
   @RequireRole(ADMIN)
   context(_: SecurityContext)
   fun deleteUser(user: UserId): Result {}
   ```
3. `@CircuitBreaker` / `@Retry` / `@RateLimiter` / `@Bulkhead` / `@TimeLimiter`

---

## Invariants

1. **Providers are overridable** — `all-open` over `@Module` and over classes carrying decorator annotations (otherwise the methods can't be overridden). Binding detection: a public provider-shape function = a binding; private/non-shape = a helper (no `BindingId`).
2. **A raw module instance is not the runtime API.** Use the generated `${X}Graph` / module API handles; the generated subclass and DAG containers are internal. `UtilsModule().clock()` with your own `KomokContext` = a deliberate bypass.
3. **`super.provider()` is forbidden in user bodies** (it bypasses the override). Not checked automatically (discipline + review); detekt/K2-plugin deferred.
4. **Do not call providers from a constructor/init/property initializer** — a virtual call from a constructor is dangerous, and `KomokContext` is unavailable there (a natural barrier).
5. **A graph read inside a provider — only via a virtual provider call** (helpers are fine); outside — via a generated handle that opens the needed `KomokContext`.
6. **Service values hold no graph access** (see "Two correctness preconditions"): they keep no `@Module` refs, generated subclass, provider lambdas, `RealmHandle`, or `KomokContext`. All dependencies are materialized in the provider body. Currently discipline + review; target enforcement = Local Lifetimes.
7. **Provider shapes.** A public provider takes one of these forms; the binding accessor is `suspend` in every case:
   - `@Module open class` (call-wiring): `context(_: KomokContext) suspend fun`, no value parameters, explicit return type; dependencies are virtual self-calls.
   - `@Module interface` (param-wiring): `context(_: KomokContext) suspend fun`; dependencies are value parameters.
   - `@Service` function (param-wiring): a plain function, `suspend` optional, no `context(_: KomokContext)`; dependencies are value parameters.
   - `@Service` class (param-wiring): dependencies are constructor parameters.

---

## Verification

Acceptance tests (conventions in `.agents/TESTING.md`). One line = one observable criterion:

1. **Memoization**: a second `clock()` → the same instance; the provider body runs once.
2. **Edge recording**: `UtilsModule.clock → UtilsModule.timeSource` after realization.
3. **Branchy provider**: only the taken branch's edges are recorded.
4. **Cross-module edge**: `JdbcModule.dataSource → ConfigModule.jdbcUrl`.
5. **Root API**: `createAppModuleGraph { }` returns `AppModuleGraph`; provider calls record edges with no user-visible `KomokContext`.
6. **Fork API**: `root.fork { }` returns `AppModuleGraph` with a child `RealmHandle`; root and fork share no per-realm cone values.
7. **Module identity**: two dependents of one `ConfigModule` → the same instance / one `BindingId`.
8. **Provider replacement**: `replaceDataSourceWith { … configModule.config() … }` runs in the child realm on first read and records the edge.
9. **Fork semantics**: after parent realize, `replaceTimeSource` → cone recomputed (`test.utils.clock !== root.utils.clock`), out-of-cone shared (`test.jdbc.dataSource === root.jdbc.dataSource`), original unchanged.
10. **No late graph read**: `UserService(config(), clock())` passes; `UserService(configModule)`, `UserService { configModule.config() }`, `UserService(currentKomokContext())` are pinned known-bad samples (no auto-diagnostic).
11. **Cycles**: module cycle → compile error; binding cycle → `KomokCycleException(path)` at runtime.
12. **Concurrency**: parallel realization of one binding → one computation, others non-blocking `await`.
13. **Mock/Spy**: generated `FooMock`/`FooSpy` + factories; `replaceFoo(fooMock())`, `every`/`verify`; a spy delegates except where stubbed.
14. **Lifecycle**: `@Startup` order = `(module-topo deps-first, phase)`; `@Shutdown` strictly reverse, awaiting each.
15. **Decorators**: produced-value decorator + static `MethodMeta`, order outer → inner, no `kotlin.reflect` import, context from the decorated method's parameters.
16. **No double-instantiation**: realize `dataSource`, then fork-replace `TimeSource` → `root`/`test` `dataSource` identical (`===`); fork `close()` does not close it.
17. **Cancellation isolation**: cancelling a reader doesn't cancel the binding; realm `close()` cancels in-flight bindings; a sibling failure is contained; failure memoized and re-thrown.
18. **Startup compensation**: `@Startup` #N throws → 1..N-1 rolled back via reverse `@Shutdown` (join each), boot fails, N+1.. skipped.
19. **Per-realm close**: `fork.close()` closes only overlay values, not parent-shared ones.
20. **Param-wiring**: a `@Service` function/class is realized via the synthetic module, with edges + memoization as usual and replaceable in its `ForkScope`.
21. **Cross-compilation**: a free provider in compilation B resolves from compilation A via `komok.index`, with no mention of A in B.
22. **Resolution diagnostics**: 0 candidates → error with the chain; ≥2 → error with both addresses; `List<String>` ≠ `List<Int>`.
23. **Multi-round**: a `@Module` referencing its own compilation's synthetic module builds (round 2); unresolved-in-final-round → human-readable error.
24. **Set aggregation**: module + free `@IntoSet` collect into an ordered `Set<T>` with `set → element` edges; replacing an element recomputes set + consumer; `replaceXSet(value)` replaces the whole set.
25. **Extension point**: a contribution from another compilation lands in the root set without rebuilding the consumer; no `Set<T>` consumer → no aggregates generated.
26. **Implicit enlargement**: a module pulled in by an inferred synthetic ref appears in `/graph`.
27. **`@ProvidersFrom`**: a KSP-less jar's bindings enter the index after one explicit mention.
28. **Service lifecycle**: `@Service`-class `@Startup`/`@Shutdown` run in synthetic-module topological order.

**E2E**: `AppModule(UtilsModule, JdbcModule(ConfigModule), WorkerModule(ConfigModule))` — `ConfigModule` singleton identity, a fork replacing `TimeSource`, `dataSource` shared out-of-cone, plus `/graph` + `/metrics`.
