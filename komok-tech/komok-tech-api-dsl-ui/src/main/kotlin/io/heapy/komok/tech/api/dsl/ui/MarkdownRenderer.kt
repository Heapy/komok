package io.heapy.komok.tech.api.dsl.ui

import kotlinx.html.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Utility for rendering Markdown to HTML.
 *
 * Uses CommonMark library to parse and render markdown content safely.
 */
internal object MarkdownRenderer {

    private val parser = Parser.builder().build()
    private val renderer = HtmlRenderer.builder().build()

    /**
     * Renders markdown text to HTML.
     *
     * @param markdown The markdown text to render
     * @return HTML string
     */
    fun renderMarkdown(markdown: String): String {
        val document = parser.parse(markdown)
        return renderer.render(document)
    }
}

/**
 * Extension function to render markdown content in a FlowContent context.
 */
internal fun FlowContent.markdown(text: String) {
    val html = MarkdownRenderer.renderMarkdown(text)
    consumer.onTagContentUnsafe {
        +html
    }
}
