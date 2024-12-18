package io.heapy.komok.tech.di.ez.test

import io.heapy.komok.tech.di.ez.lib.Ez

@Ez
fun nameProvider(age: Int): String {
    return "Ruslan $age"
}

@Ez
fun ageProvider(): Int {
    return 32
}

@Ez
class ImplicitConstructor {
    fun foo(): String {
        return "foo"
    }
}

@Ez
class TestService(
    private val string: String,
    private val int: Int
) {
    fun hello() {
        println(string + int)
    }
}
