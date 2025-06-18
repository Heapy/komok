package io.heapy.komok.tech.ktor.htmx

import kotlinx.html.BODY
import kotlinx.html.HTML
import kotlinx.html.HTMLTag
import kotlinx.html.body
import kotlinx.html.consumers.filter
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML

private const val SKIP_ATTRIBUTE = "skip-element-komok-htmx-internal-attribute"
private const val SKIP_VALUE = "do-it"

fun HTMLTag.doNotRenderCurrentTag() {
    attributes[SKIP_ATTRIBUTE] = SKIP_VALUE
}

fun renderBodyComponent(
    prettyPrint: Boolean = false,
    component: BODY.() -> Unit,
): String {
    return createHTML(prettyPrint = prettyPrint)
        .filter { tag ->
            if (tag.attributes[SKIP_ATTRIBUTE] == SKIP_VALUE) {
                SKIP
            } else {
                PASS
            }
        }
        .body {
            doNotRenderCurrentTag()
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
