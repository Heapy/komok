package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Key
import io.heapy.komok.tech.di.ez.api.Provider

@Suppress("NOTHING_TO_INLINE")
internal inline fun Key.isProvider(): Boolean {
    return type.classifier == Provider::class
}
