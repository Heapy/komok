package io.heapy.komok.tech.decorators.test

import io.heapy.komok.tech.decorators.plugins.logging.annotations.Log
import io.heapy.komok.tech.decorators.plugins.logging.log2
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun main() {
    val log = LoggerFactory.getLogger("test")
    context(
        log,
        ::testF,
        Log(level = Level.ERROR),
        CallId("123"),
    ) {
        log2(
            1,
            2,
        ) { p1, p2 ->
            testF(
                p1,
                p2,
            )
        }
    }
}

data class DecoratedFunction(
    val name: String,
    val parameters: List<Parameter>,
    val returnType: kotlin.reflect.KType,
) {
    data class Parameter(
        val name: String,
        val type: kotlin.reflect.KType,
    )
}

@JvmInline
value class CallId(val id: String)

fun testF(
    p1: Int,
    p2: Int,
) =
    p1 + p2
