package io.heapy.komok.tech.di.ez.framework

/**
 * Represents entry point of application.
 *
 * Usually this method runs something like web server or desktop application.
 *
 * @author Ruslan Ibrahimau
 */
interface EntryPoint<out R> {
    suspend fun run(): R
}
