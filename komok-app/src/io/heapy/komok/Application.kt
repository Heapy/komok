@file:JvmName("Application")
package io.heapy.komok

import io.heapy.komok.business.entrypoint.createKomokApplicationModule

fun main() {
    createKomokApplicationModule {}
        .komokApplication
        .run()
}
