package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Instant

class AppendersTest {

    private val timestamp = Instant.parse("2025-01-15T10:30:00Z")

    private fun event(message: String = "test message") = LogEvent(
        level = Level.INFO,
        loggerName = "com.example.Test",
        message = message,
        throwable = null,
        timestamp = timestamp,
        mdc = Mdc.Empty,
    )

    @Test
    fun `ConsoleAppender writes to stdout`() {
        val encoder = Encoder { "formatted: ${it.message}" }
        val appender = ConsoleAppender(encoder, ConsoleTarget.STDOUT)

        appender.handle(event())
    }

    @Test
    fun `FileAppender writes to file`(@TempDir tempDir: Path) {
        val logFile = tempDir.resolve("test.log")
        val encoder = Encoder { it.message }

        val appender = FileAppender(encoder, logFile)
        appender.handle(event("line1"))
        appender.handle(event("line2"))
        appender.close()

        val lines = Files.readAllLines(logFile)
        assertEquals(listOf("line1", "line2"), lines)
    }

    @Test
    fun `FileAppender creates parent directories`(@TempDir tempDir: Path) {
        val logFile = tempDir.resolve("sub/dir/test.log")
        val encoder = Encoder { it.message }

        val appender = FileAppender(encoder, logFile)
        appender.handle(event("hello"))
        appender.close()

        assertTrue(Files.exists(logFile))
        assertEquals(listOf("hello"), Files.readAllLines(logFile))
    }

    @Test
    fun `FileAppender with size-based rolling`(@TempDir tempDir: Path) {
        val logFile = tempDir.resolve("test.log")
        val encoder = Encoder { it.message }

        val appender = FileAppender(
            encoder = encoder,
            path = logFile,
            rolling = SizeBasedRolling(maxBytes = 10, maxHistory = 2),
        )

        appender.handle(event("12345678901"))
        appender.handle(event("second"))
        appender.close()

        assertTrue(Files.exists(logFile))
        assertTrue(Files.exists(tempDir.resolve("test.log.1")))
    }

    @Test
    fun `FileAppender with gzip compression`(@TempDir tempDir: Path) {
        val logFile = tempDir.resolve("test.log")
        val encoder = Encoder { it.message }

        val appender = FileAppender(
            encoder = encoder,
            path = logFile,
            rolling = SizeBasedRolling(maxBytes = 10, maxHistory = 2),
            compression = Compression.Gzip,
        )

        appender.handle(event("12345678901"))
        appender.handle(event("second"))
        appender.close()

        assertTrue(Files.exists(logFile))
        assertTrue(
            Files.list(tempDir).use { stream ->
                stream.anyMatch { it.fileName.toString().endsWith(".gz") }
            },
        )
    }
}
