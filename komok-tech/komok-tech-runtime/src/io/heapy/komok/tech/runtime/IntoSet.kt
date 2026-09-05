package io.heapy.komok.tech.runtime

/**
 * Marks a `@Service` function as a contribution to the generated `Set<T>` binding.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IntoSet
