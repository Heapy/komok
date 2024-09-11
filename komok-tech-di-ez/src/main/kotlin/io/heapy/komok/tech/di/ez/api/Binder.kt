package io.heapy.komok.tech.di.ez.api

/**
 * Binder used to collect all [Binding]s and create context from them.
 *
 * @author Ruslan Ibrahimau
 */
interface Binder {
    val source: String

    @ModuleDSL
    fun dependency(module: ModuleProvider)

    @ModuleDSL
    fun contribute(binding: Binding<*>)
}
