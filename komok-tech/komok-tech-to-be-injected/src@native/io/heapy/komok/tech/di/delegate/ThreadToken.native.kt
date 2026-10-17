package io.heapy.komok.tech.di.delegate

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private var threadToken: Any? = null

internal actual fun currentThreadToken(): Any =
    threadToken
        ?: Any().also { threadToken = it }
