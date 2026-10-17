package io.heapy.komok.tech.di.ez.framework

import io.heapy.komok.tech.di.ez.api.ModuleBuilder
import io.heapy.komok.tech.di.ez.api.createContext
import io.heapy.komok.tech.di.ez.api.get
import io.heapy.komok.tech.di.ez.dsl.module

suspend inline fun <reified T : EntryPoint<R>, R> komok(
    noinline builder: ModuleBuilder,
): R {
    val komokModule by module(builder)
    return komokModule
        .createContext()
        .get<T>()
        .run()
}
