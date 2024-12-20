@file:JvmName("Application")
package io.heapy.komok

fun main() {
    createKomokApplicationModule {}
        .komokApplication
        .run()
}
