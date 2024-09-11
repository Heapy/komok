package io.heapy.komok.tech.di.ez.api

interface Module {
    /**
     * Source of the module (class fqn, or file and variable name).
     * Library will enforce uniqueness
     */
    val source: String

    /**
     * Dependencies of this module
     */
    val dependencies: List<ModuleProvider>

    /**
     * Bindings defined in this module
     */
    val bindings: List<Binding<*>>
}
