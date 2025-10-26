package io.heapy.komok.tech.api.dsl.ui

import kotlinx.html.*
import org.commonmark.node.*
import org.commonmark.parser.Parser

/**
 * Utility for rendering Markdown to kotlinx.html.
 *
 * Uses CommonMark library to parse markdown content and converts the AST
 * to kotlinx.html elements without using unsafe HTML injection.
 */
internal object MarkdownRenderer {

    private val parser = Parser.builder().build()

    /**
     * Renders markdown text to kotlinx.html elements.
     *
     * @param markdown The markdown text to render
     * @param content The FlowContent context to render into
     */
    fun renderMarkdown(markdown: String, content: FlowContent) {
        val document = parser.parse(markdown)
        val visitor = HtmlVisitor(content)
        document.accept(visitor)
    }

    /**
     * Visitor that converts CommonMark nodes to kotlinx.html elements.
     */
    private class HtmlVisitor(
        private val rootContent: FlowContent
    ) : AbstractVisitor() {

        // Stack to track nested contexts
        private val contextStack = ArrayDeque<Any>()

        init {
            contextStack.addLast(rootContent)
        }

        private inline fun <reified T> currentContext(): T? {
            return contextStack.lastOrNull() as? T
        }

        override fun visit(document: Document) {
            visitChildren(document)
        }

        override fun visit(heading: Heading) {
            currentContext<FlowContent>()?.let { flow ->
                when (heading.level) {
                    1 -> flow.h1 { withContext(this) { visitChildren(heading) } }
                    2 -> flow.h2 { withContext(this) { visitChildren(heading) } }
                    3 -> flow.h3 { withContext(this) { visitChildren(heading) } }
                    4 -> flow.h4 { withContext(this) { visitChildren(heading) } }
                    5 -> flow.h5 { withContext(this) { visitChildren(heading) } }
                    6 -> flow.h6 { withContext(this) { visitChildren(heading) } }
                }
            }
        }

        override fun visit(paragraph: Paragraph) {
            currentContext<FlowContent>()?.p {
                withContext(this) { visitChildren(paragraph) }
            }
        }

        override fun visit(text: Text) {
            currentContext<FlowOrPhrasingContent>()?.text(text.literal)
        }

        override fun visit(emphasis: Emphasis) {
            currentContext<FlowOrPhrasingContent>()?.em {
                withContext(this) { visitChildren(emphasis) }
            }
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            currentContext<FlowOrPhrasingContent>()?.strong {
                withContext(this) { visitChildren(strongEmphasis) }
            }
        }

        override fun visit(code: Code) {
            currentContext<FlowOrPhrasingContent>()?.code {
                +code.literal
            }
        }

        override fun visit(indentedCodeBlock: IndentedCodeBlock) {
            currentContext<FlowContent>()?.pre {
                code {
                    +indentedCodeBlock.literal
                }
            }
        }

        override fun visit(fencedCodeBlock: FencedCodeBlock) {
            currentContext<FlowContent>()?.pre {
                code {
                    val info = fencedCodeBlock.info
                    if (!info.isNullOrEmpty()) {
                        classes = setOf("language-$info")
                    }
                    +fencedCodeBlock.literal
                }
            }
        }

        override fun visit(link: Link) {
            currentContext<FlowOrPhrasingContent>()?.a(href = link.destination) {
                link.title?.takeIf { it.isNotEmpty() }?.let { attributes["title"] = it }
                withContext(this) { visitChildren(link) }
            }
        }

        override fun visit(image: Image) {
            currentContext<FlowOrPhrasingContent>()?.img(src = image.destination) {
                image.title?.takeIf { it.isNotEmpty() }?.let { attributes["title"] = it }
                val altText = collectText(image)
                if (altText.isNotEmpty()) {
                    alt = altText
                }
            }
        }

        override fun visit(bulletList: BulletList) {
            currentContext<FlowContent>()?.ul {
                withContext(this) { visitChildren(bulletList) }
            }
        }

        override fun visit(orderedList: OrderedList) {
            currentContext<FlowContent>()?.ol {
                @Suppress("DEPRECATION")
                if (orderedList.startNumber != 1) {
                    @Suppress("DEPRECATION")
                    start = orderedList.startNumber.toString()
                }
                withContext(this) { visitChildren(orderedList) }
            }
        }

        override fun visit(listItem: ListItem) {
            // List items can be in either UL or OL context
            when (val ctx = contextStack.lastOrNull()) {
                is UL -> ctx.li { withContext(this) { visitChildren(listItem) } }
                is OL -> ctx.li { withContext(this) { visitChildren(listItem) } }
            }
        }

        override fun visit(blockQuote: BlockQuote) {
            currentContext<FlowContent>()?.blockQuote {
                withContext(this) { visitChildren(blockQuote) }
            }
        }

        override fun visit(thematicBreak: ThematicBreak) {
            currentContext<FlowContent>()?.hr {}
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            currentContext<FlowOrPhrasingContent>()?.br {}
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            currentContext<FlowOrPhrasingContent>()?.text("\n")
        }

        override fun visit(htmlBlock: HtmlBlock) {
            // For safety, render HTML blocks as preformatted text to avoid XSS
            currentContext<FlowContent>()?.pre {
                +htmlBlock.literal
            }
        }

        override fun visit(htmlInline: HtmlInline) {
            // For safety, render inline HTML as plain text
            currentContext<FlowOrPhrasingContent>()?.text(htmlInline.literal)
        }

        private inline fun withContext(context: Any, block: () -> Unit) {
            contextStack.addLast(context)
            try {
                block()
            } finally {
                contextStack.removeLast()
            }
        }

        private fun collectText(node: Node): String {
            val builder = StringBuilder()
            node.accept(object : AbstractVisitor() {
                override fun visit(text: Text) {
                    builder.append(text.literal)
                    visitChildren(text)
                }
            })
            return builder.toString()
        }
    }
}

/**
 * Extension function to render markdown content in a FlowContent context.
 */
internal fun FlowContent.markdown(text: String) {
    MarkdownRenderer.renderMarkdown(text, this)
}
