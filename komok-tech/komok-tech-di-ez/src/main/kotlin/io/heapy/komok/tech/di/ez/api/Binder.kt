package io.heapy.komok.tech.di.ez.api

/**
 * Binder used to collect all [Binding]s and create context from them.
 *
 * @author Ruslan Ibrahimau
 */
interface Binder {
    val source: String

    fun dependency(module: ModuleProvider)

    fun contribute(binding: Binding<*>)
}
