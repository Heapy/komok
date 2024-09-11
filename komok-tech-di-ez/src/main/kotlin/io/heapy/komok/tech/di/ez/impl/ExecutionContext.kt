package io.heapy.komok.tech.di.ez.impl

import io.heapy.komok.tech.di.ez.api.Binding
import io.heapy.komok.tech.di.ez.api.Key

internal interface ExecutionContext {
    val definitions: Map<Key, Binding<*>>
    val stack: MutableList<Key>
    val instances: MutableMap<Key, Any?>
}
