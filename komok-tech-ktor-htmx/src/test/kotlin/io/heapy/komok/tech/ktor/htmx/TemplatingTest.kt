package io.heapy.komok.tech.ktor.htmx

import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.title
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TemplatingTest {
    @Test
    fun `renderBodyComponent should render body content without html and body tags`() {
        val result = renderBodyComponent {
            div {
                p { +"Hello World" }
            }
        }

        assertEquals(
            // language=HTML
            "<div><p>Hello World</p></div>",
            result,
        )
    }

    @Test
    fun `renderBodyComponent with prettyPrint true should render with extra whitespace`() {
        val result = renderBodyComponent(prettyPrint = true) {
            div {
                p { +"Hello World" }
            }
        }

        assertEquals(
            // language=HTML
            """
            <div>
              <p>Hello World</p>
            </div>

            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `renderHtmlComponent should render html content without html and body tags`() {
        val result = renderHtmlComponent {
            body {
                div {
                    p { +"Hello World" }
                }
            }
        }

        assertEquals(
            // language=HTML
            "<div><p>Hello World</p></div>",
            result,
        )
    }

    @Test
    fun `renderHtmlComponent should skip head tag and its contents`() {
        val result = renderHtmlComponent {
            head {
                title { +"Page Title" }
            }
            body {
                div { +"Content" }
            }
        }

        assertEquals(
            // language=HTML
            "<div>Content</div>",
            result,
        )
    }

    @Test
    fun `renderHtmlComponent with prettyPrint true should render with extra whitespace`() {
        val result = renderHtmlComponent(prettyPrint = true) {
            body {
                div {
                    p { +"Hello World" }
                }
            }
        }

        assertEquals(
            // language=HTML
            """
                <div>
                  <p>Hello World</p>
                </div>

            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `renderHtmlPage should include DOCTYPE and html structure`() {
        val result = renderHtmlPage {
            body {
                div { +"Content" }
            }
        }

        // language=HTML
        val expected = """
            <!DOCTYPE html>
            <html><body><div>Content</div></body></html>
        """.trimIndent()

        assertEquals(
            expected,
            result,
        )
    }

    @Test
    fun `renderHtmlPage with prettyPrint true should format output`() {
        val result = renderHtmlPage(prettyPrint = true) {
            body {
                div { +"Content" }
            }
        }

        // language=HTML
        val expected = """
            <!DOCTYPE html>
            <html>
              <body>
                <div>Content</div>
              </body>
            </html>

        """.trimIndent()

        assertEquals(
            expected,
            result,
        )
    }
}
