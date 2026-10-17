package io.heapy.komok.tech.di.ez.impl

import kotlin.reflect.KFunction

@PublishedApi
internal fun <T> T.asMethodReference(): KFunction<T> {
    class InstanceHolder(val value: T) {
        fun getInstance(): T = value
    }
    return InstanceHolder(this)::getInstance
}
