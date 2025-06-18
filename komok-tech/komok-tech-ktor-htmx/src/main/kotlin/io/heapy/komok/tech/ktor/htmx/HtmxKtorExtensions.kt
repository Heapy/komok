package io.heapy.komok.tech.ktor.htmx

import io.ktor.server.application.ApplicationCall

/**
 * Checks if the current HTTP request is an HTMX request.
 * HTMX requests include the header [HtmxRequestHeaders.HX_REQUEST] set to "true".
 */
inline val ApplicationCall.isHtmxRequest: Boolean
    get() = request.headers[HtmxRequestHeaders.HX_REQUEST] == "true"
