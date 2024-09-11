package io.heapy.komok.tech.di.ez.api

/**
 * Functional type that represents a module.
 * It may be a functional interface, but it tends to implement module as classes,
 * which introduces more unnecessary boilerplate.
 */
typealias ModuleBuilder = Binder.() -> Unit
