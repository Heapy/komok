#!/usr/bin/env kotlinr
import java.io.File
import java.net.URI

val output = File("landing/build/site").canonicalFile
require(File(output, "index.html").isFile) { "Build first: kotlinr landing/build.main.kts" }
val html = output.listFiles()!!.filter { it.extension == "html" }.associateWith { it.readText() }
require(html.keys.map { it.name }.toSet() == setOf("index.html", "404.html")) {
    "The public site must contain only the landing and 404 page, with no stale guide chapters."
}
val ids = html.mapValues { (_, source) ->
    val values = Regex("""\bid="([^"]+)"""").findAll(source).map { it.groupValues[1] }.toList()
    require(values.size == values.toSet().size) { "Duplicate HTML IDs" }
    values.toSet()
}
for ((file, source) in html) {
    require(!Regex("\\{\\{[a-zA-Z0-9_.]+}}").containsMatchIn(source)) { "Unresolved template in $file" }
    for (match in Regex("""\b(?:href|src)="([^"]+)"""").findAll(source)) {
        val uri = URI(match.groupValues[1])
        if (uri.isAbsolute) continue
        val target = when {
            uri.path.isNullOrEmpty() -> file
            uri.path.startsWith("/") -> File(output, uri.path.removePrefix("/"))
            else -> File(file.parentFile, uri.path)
        }.canonicalFile
        require(target.toPath().startsWith(output.toPath()) && target.isFile) { "Missing local target in ${file.name}: $uri" }
        if (!uri.fragment.isNullOrEmpty()) require(uri.fragment in ids.getValue(target)) { "Missing anchor in ${file.name}: $uri" }
    }
}
val features = Regex("(?m)^### ([A-Z]+-\\d+) ·").findAll(File("docs/features.md").readText()).map { it.groupValues[1] }.toList()
require(features.isNotEmpty() && features.size == features.toSet().size) { "Missing or duplicate feature IDs" }
println("Verified ${html.size} pages, local links/assets/anchors and ${features.size} unique features.")
