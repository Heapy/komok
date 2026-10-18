#!/usr/bin/env kotlinr
@file:DependsOn("org.commonmark:commonmark:0.30.1")

import java.io.File
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

// Run from the repository root. Only the landing copy is included in the public site.
val root = File(".").canonicalFile
require(File(root, "docs/landing.md").isFile) { "Run from the Komok repository root." }
val output = File(root, "landing/build/site")
// Recreate generated output so removed assets cannot survive into the Docker image.
check(!output.exists() || output.deleteRecursively()) { "Could not clean generated site: $output" }
check(output.mkdirs()) { "Could not create generated site directory: $output" }
val parser = Parser.builder().build()
val renderer = HtmlRenderer.builder().build()
fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
fun render(markdown: String) = renderer.render(parser.parse(markdown))
fun template(name: String, values: Map<String, String>): String {
    val source = File(root, "landing/$name.html").readText()
    return Regex("\\{\\{([a-zA-Z0-9_.]+)}}").replace(source) {
        values[it.groupValues[1]] ?: error("Missing template value: ${it.groupValues[1]}")
    }
}
val footer = """<footer class="site-footer page-width"><a href="index.html">Komok by Heapy</a><span>A personal information system, in development.</span><a href="https://github.com/Heapy/komok">GitHub</a></footer>"""
val landing = File(root, "docs/landing.md").readText().split(Regex("(?m)^## "))
val copy = mutableMapOf("footer" to footer)
for (section in landing.drop(1)) {
    val key = section.substringBefore('\n').trim().lowercase()
    val paragraphs = section.substringAfter('\n').trim().split("\n\n", limit = 2)
    copy["$key.title"] = escape(paragraphs[0])
    copy["$key.body"] = render(paragraphs.getOrElse(1) { "" })
}
File(output, "index.html").writeText(template("landing", copy))
File(root, "landing/assets").copyRecursively(File(output, "assets"), overwrite = true)
File(root, "landing/style.css").copyTo(File(output, "style.css"), overwrite = true)
File(root, "landing/site.js").copyTo(File(output, "site.js"), overwrite = true)
File(output, "404.html").writeText("""<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Page not found · Komok</title><link rel="stylesheet" href="/style.css"><main class="not-found"><p class="eyebrow">404 / Missing connection</p><h1>This page isn't here.</h1><a href="/index.html">Return to Komok →</a></main></html>""")
println("Built landing and 404 page in ${output.relativeTo(root)}; project documentation stays in docs/.")
