package io.heapy.komok.tech.runtime

/**
 * Declares a method decorator supplied by an ordinary library. [helper] is the fully
 * qualified name of a suspend helper with `meta`, `call`, and optional annotation
 * arguments. Its context parameters must also be present on the decorated method.
 * Lower [order] values wrap higher values.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class CallWrapper(val helper: String, val order: Int)
