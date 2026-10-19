@file:JvmName("Application")
package io.heapy.komok

import io.heapy.komok.tech.runtime.*
import io.heapy.komok.business.entrypoint.KomokApplicationModule
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val graph = graph<KomokApplicationModule>()
        try {
            graph { komokApplication() }.run()
        } finally {
            graph.stop()
        }
    }
}
