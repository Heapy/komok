package io.heapy.komok.tech.runtime.compiler.sample

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class FinalModule {
    var creations: Int = 0
        private set

    context(_: KomokContext)
    suspend fun leaf(): Any {
        creations++
        return Any()
    }

    context(_: KomokContext)
    suspend fun root(): List<Any> = listOf(leaf(), leaf())
}
