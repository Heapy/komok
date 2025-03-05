package io.heapy.komok.tech.ktor.htmx

import kotlinx.html.BODY
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.consumers.filter
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML

fun renderBodyComponent(
    prettyPrint: Boolean = false,
    component: BODY.() -> Unit,
): String {
    return createHTML(prettyPrint = prettyPrint)
        .filter {
            when (it.tagName) {
                "body" -> SKIP
                else -> PASS
            }
        }
        .body {
            component()
        }
}

fun renderHtmlComponent(
    prettyPrint: Boolean = false,
    component: HTML.() -> Unit,
): String {
    return createHTML(prettyPrint = prettyPrint)
        .filter {
            when (it.tagName) {
                "html", "body" -> SKIP
                "head" -> DROP
                else -> PASS
            }
        }
        .html {
            component()
        }
}

fun renderHtmlPage(
    prettyPrint: Boolean = false,
    component: HTML.() -> Unit,
): String {
    return buildString {
        append("<!DOCTYPE html>\n")
        appendHTML(prettyPrint = prettyPrint)
            .html(block = component)
    }
}
