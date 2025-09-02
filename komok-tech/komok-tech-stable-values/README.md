# Komok Tech Stable Values

This project provides thin Stable values wrappers for Kotlin, based on [JEP 502: Stable Values](https://openjdk.org/jeps/502).

`stableValue` property delegate is drop-in replacement for `lazy` delegate, with advertised performance improvements on JDK 25+.
Use when you want final-like performance and safety, but with on-demand initialization.

## Rule of thumb
- If you don’t care about JVM folding and perf is fine → lazy.
- If the value is read frequently in hot code, or you want startup sliced into on-demand chunks without losing final-like optimizations → stableValue.

## Quick start

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.heapy.komok:komok-tech-stable-values:1.0.13")
}
```

## From the JEP:
> ## Summary
> Introduce an API for stable values, which are objects that hold immutable data. Stable values are treated as constants by the JVM, enabling the same performance optimizations that are enabled by declaring a field final. Compared to final fields, however, stable values offer greater flexibility as to the timing of their initialization. This is a preview API.
>
> ## Goals
>  - Improve the startup of Java applications by breaking up the monolithic initialization of application state.
>  - Decouple the creation of stable values from their initialization, without significant performance penalties.
>  - Guarantee that stable values are initialized at most once, even in multi-threaded programs.
>  - Enable user code to safely enjoy constant-folding optimizations previously available only to JDK-internal code.
> ## Non-goals
> - It is not a goal to enhance the Java programming language with a means to declare stable values.
> - It is not a goal to alter the semantics of final fields.


## What this module provides
- Property delegate stableValue { ... } which evaluates the initializer lazily once and then returns the same instance on subsequent access.
- Wrappers around a StableValue for precomputed collections and functions:
  - stableList(size, mapper: IntFunction<V>): List<V>
  - stableMap(keys: Set<T>, underlying: (T) -> R): Map<T, R>
  - stableFunction(domain: Set<T>, underlying: (T) -> R): java.util.function.Function<T, R>
  - stableIntFunction(size: Int, underlying: IntFunction<R>): java.util.function.IntFunction<R>

## Usage examples

```kotlin
// Stable property value
data class Service(val id: String)
class Holder {
    val service by stableValue { Service("main") }
}

// Precomputed list (indices 0..9)
val list = stableList(10) { i -> "v$i" }

// Stable map for a fixed key set
val keys = setOf("a", "b", "c")
val map = stableMap(keys) { it.uppercase() }

// Stable (memoized) function for a fixed domain
val f = stableFunction(setOf(1, 2, 3)) { x -> "x=$x" }
// Calls for 1,2,3 are computed once; repeated calls reuse the value

// Stable IntFunction over a fixed size domain [0, size)
val g = stableIntFunction(4) { i -> i * 10 }
```


## Patterns

### A. Replace eager final with deferred stable supplier

```kotlin
// Before (eager)
class C {
    private val logger = Logger.create(C::class.java)
}

// After (deferred, constant-foldable post-init)
class C {
    val logger by stableValue { Logger.create(C::class.java) }
}
```

### B. Compose stable across layers

```kotlin
object App {
    // static final fields on JVM
    val orders by stableValue { Orders() }
}

class Orders {
    val logger by stableValue { Logger.create(Orders::class.java) }
}

fun handle() {
    App.orders.logger.info("start") // theoretically-foldable after warmup
}
```

### C. Stable pool

```kotlin
class Router(
    private val poolSize: Int,
) {
    private val handlers: List<Handler> = stableList(poolSize) { Handler(it) }

    fun pick(): Handler {
        val idx = (Thread.currentThread().threadId() % poolSize).toInt()
        return handlers[idx]
    }
}
```

## Concurrency & semantics
- At most once: Each slot (single value, list index, map key) runs its initializer once; concurrent callers race and the winner’s result becomes the content.
- Visibility: Once set, reads see the fully initialized value without extra fences.
- Exceptions: If the initializer throws, the slot stays unset and a later call will retry. Make your initializer idempotent if it has side effects.
