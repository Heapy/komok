package io.heapy.komok.tech.ktor.htmx

import kotlinx.html.HEAD
import kotlinx.html.ScriptType
import kotlinx.html.script

/**
 * Adds the HTMX 2.0.4 script tag to the HEAD element.
 */
fun HEAD.htmxScriptLatest() {
    htmxScript(
        version = "2.0.4",
        integrity = "sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+"
    )
}

/**
 * Adds the HTMX specified script tag to the HEAD element.
 */
fun HEAD.htmxScript(
    version: String,
    integrity: String,
) {
    script(type = ScriptType.textJavaScript) {
        this.src = "https://unpkg.com/htmx.org@$version"
        this.integrity = integrity
        this.attributes["crossorigin"] = "anonymous"
    }
}
