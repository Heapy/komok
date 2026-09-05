@file:JvmName("Application")
package io.heapy.komok

import io.heapy.komok.business.entrypoint.createKomokApplicationModuleGraph
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        createKomokApplicationModuleGraph()
            .komokApplication()
            .run()
    }
}
