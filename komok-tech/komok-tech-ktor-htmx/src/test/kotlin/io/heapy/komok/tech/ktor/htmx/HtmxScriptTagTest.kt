package io.heapy.komok.tech.ktor.htmx
import kotlinx.html.head
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HtmxScriptTagTest {
    @Test
    fun `htmxScriptLatest should add HTMX script with version 2_0_4 to HEAD`() {
        val htmlString = renderHtmlPage(prettyPrint = true) {
            head {
                htmxScriptLatest()
            }
        }

        @Language("HTML")
        val expectedHtml = """
            <!DOCTYPE html>
            <html>
              <head>
                <script type="text/javascript" src="https://unpkg.com/htmx.org@2.0.4" integrity="sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+" crossorigin="anonymous"></script>
              </head>
            </html>

        """.trimIndent()

        assertEquals(expectedHtml, htmlString)
    }

    @Test
    fun `htmxScript should add HTMX script with specified version and integrity to HEAD`() {
        val testVersion = "1.9.0"
        val testIntegrity = "sha384-testIntegrityValue"

        val htmlString = renderHtmlPage(prettyPrint = true) {
            head {
                htmxScript(
                    version = testVersion,
                    integrity = testIntegrity
                )
            }
        }

        @Language("HTML")
        val expectedHtml = """
            <!DOCTYPE html>
            <html>
              <head>
                <script type="text/javascript" src="https://unpkg.com/htmx.org@1.9.0" integrity="sha384-testIntegrityValue" crossorigin="anonymous"></script>
              </head>
            </html>

        """.trimIndent()

        assertEquals(expectedHtml, htmlString)
    }

    @Test
    fun `multiple calls to htmxScript should add multiple script tags`() {
        val htmlString = renderHtmlPage(prettyPrint = true) {
            head {
                htmxScript(
                    version = "2.0.4",
                    integrity = "sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+"
                )
                htmxScript(
                    version = "1.9.0",
                    integrity = "sha384-differentIntegrityValue"
                )
            }
        }

        @Language("HTML")
        val expectedHtml = """
            <!DOCTYPE html>
            <html>
              <head>
                <script type="text/javascript" src="https://unpkg.com/htmx.org@2.0.4" integrity="sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+" crossorigin="anonymous"></script>
                <script type="text/javascript" src="https://unpkg.com/htmx.org@1.9.0" integrity="sha384-differentIntegrityValue" crossorigin="anonymous"></script>
              </head>
            </html>

        """.trimIndent()

        assertEquals(expectedHtml, htmlString)
    }
}
