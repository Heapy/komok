# Komok Tech Lazy

This project provides Kotlin-friendly wrappers for Lazy Constants, based on [JEP 526: Lazy Constants](https://openjdk.org/jeps/526).

`lazyConst` property delegate is drop-in replacement for `lazy` delegate, with advertised performance improvements on JDK 26+.
Use when you want final-like performance and safety, but with on-demand initialization.

## Rule of thumb
- If you don't care about JVM constant folding and perf is fine → lazy.
- If the value is read frequently in hot code, or you want startup sliced into on-demand chunks without losing final-like optimizations → lazyConst.

## Quick start

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.heapy.komok:komok-tech-lazy:1.0.13")
}
```

## From the JEP:
> ## Summary
> Introduce an API for lazy constants, which are objects that hold immutable data. Lazy constants are treated as true constants by the JVM, enabling the same performance optimizations that are enabled by declaring a field final. Compared to final fields, however, lazy constants offer greater flexibility as to the timing of their initialization. This is a preview API.
> ## Goals
> - Enable application state to be initialized incrementally, on demand, rather than monolithically, thereby reducing application startup time.
> - Decouple the creation of lazy constants from their initialization, without significant performance penalties.
> - Guarantee that lazy constants are initialized at most once, even in multi-threaded programs.
> - Enable user code to benefit from constant-folding optimizations previously applicable only to JDK-internal code.
> ## Non-goals
> - It is not a goal to enhance the Java programming language with a means to declare lazy fields.
> - It is not a goal to alter the semantics of final fields.


## What this module provides
- Property delegate `lazyConst { ... }` which evaluates the initializer lazily once and then returns the same instance on subsequent access.
- Wrappers around lazy constants for precomputed collections:
  - `lazyList(size, mapper: IntFunction<V>): List<V>` - precomputed list with indices [0, size)
  - `lazyMap(keys: Set<T>, underlying: Function<T, R>): Map<T, R>` - lazy constant map for a fixed key set

## Usage examples

```kotlin
// Lazy constant property
data class Service(val id: String)
class Holder {
    val service by lazyConst { Service("main") }
}

// Precomputed list (indices 0..9)
val list = lazyList(10) { i -> "v$i" }

// Lazy constant map for a fixed key set
val keys = setOf("a", "b", "c")
val map = lazyMap(keys) { it.uppercase() }
```


## Patterns

### A. Replace eager final with deferred lazy constant

```kotlin
// Before (eager)
class C {
    private val logger = Logger.create(C::class.java)
}

// After (deferred, constant-foldable post-init)
class C {
    val logger by lazyConst { Logger.create(C::class.java) }
}
```

### B. Compose lazy constants across layers

```kotlin
object App {
    // static final fields on JVM
    val orders by lazyConst { Orders() }
}

class Orders {
    val logger by lazyConst { Logger.create(Orders::class.java) }
}

fun handle() {
    App.orders.logger.info("start") // theoretically-foldable after warmup
}
```

### C. Lazy constant pool

```kotlin
class Router(
    private val poolSize: Int,
) {
    private val handlers: List<Handler> = lazyList(poolSize) { Handler(it) }

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
