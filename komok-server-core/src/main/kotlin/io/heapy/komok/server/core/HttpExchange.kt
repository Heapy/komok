package io.heapy.komok.server.core

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import java.net.URI

interface HttpExchange {
    val method: Method
//    val uri: URI
//    val headers: Headers

//    val ctx: ChannelHandlerContext
//    val request: FullHttpRequest
}

interface Headers {
    /**
     * Return a list of values for the given key or null if the key is not present.
     */
    fun get(key: String): List<String>?
}

class ImmutableHeaders(
    private val map: Map<String, List<String>>,
) : Headers {
    operator fun set(key: String, value: String): ImmutableHeaders {
        val newMap = map.toMutableMap()
        val list = newMap[key]
        if (list == null) {
            newMap[key] = mutableListOf(value)
        } else {
            newMap[key] = list.toMutableList().apply { add(value) }
        }
        return ImmutableHeaders(newMap)
    }

    operator fun set(key: String, value: List<String>): ImmutableHeaders {
        val newMap = map.toMutableMap()
        val list = newMap[key]
        if (list == null) {
            newMap[key] = value
        } else {
            newMap[key] = list.toMutableList().apply { addAll(value) }
        }
        return ImmutableHeaders(newMap)
    }

    override fun get(key: String): List<String>? {
        return map[key]
    }
}

enum class Method {
    GET,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    TRACE,
    PATCH,
    HEAD;
}

