package io.heapy.komok.tech.logging

import java.io.BufferedWriter
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/**
 * Output target for [ConsoleAppender].
 */
enum class ConsoleTarget {
    STDOUT,
    STDERR,
}

/**
 * Writes encoded log events to stdout or stderr.
 */
class ConsoleAppender(
    private val encoder: Encoder,
    private val target: ConsoleTarget = ConsoleTarget.STDOUT,
) : LogHandler {

    private val stream: PrintStream
        get() = when (target) {
            ConsoleTarget.STDOUT -> System.out
            ConsoleTarget.STDERR -> System.err
        }

    override fun handle(event: LogEvent) {
        stream.println(encoder.encode(event))
    }

    override fun toString(): String = "ConsoleAppender($target)"
}

/**
 * Rolling policy for [FileAppender].
 */
sealed interface RollingPolicy {
    fun shouldRoll(path: Path, timestamp: Instant): Boolean
    fun roll(path: Path, timestamp: Instant)
}

/**
 * Roll when file exceeds a size threshold.
 */
class SizeBasedRolling(
    private val maxBytes: Long,
    private val maxHistory: Int = 5,
) : RollingPolicy {

    override fun shouldRoll(path: Path, timestamp: Instant): Boolean =
        Files.exists(path) && Files.size(path) >= maxBytes

    override fun roll(path: Path, timestamp: Instant) {
        for (i in maxHistory downTo 1) {
            val from = path.resolveSibling("${path.fileName}.$i")
            val to = path.resolveSibling("${path.fileName}.${i + 1}")
            if (Files.exists(from)) {
                if (i == maxHistory) Files.delete(from)
                else Files.move(from, to)
            }
        }
        Files.move(path, path.resolveSibling("${path.fileName}.1"))
    }
}

/**
 * Roll daily, on the UTC day boundary of the event timestamp.
 */
class TimeBasedRolling(
    private val datePattern: String = "yyyy-MM-dd",
) : RollingPolicy {

    private val formatter = DateTimeFormatter.ofPattern(datePattern)
    @Volatile private var lastDate: String? = null

    override fun shouldRoll(path: Path, timestamp: Instant): Boolean {
        val current = dateOf(timestamp)
        val previous = lastDate
        if (previous == null) {
            lastDate = current
            return false
        }
        return current != previous
    }

    override fun roll(path: Path, timestamp: Instant) {
        val date = lastDate ?: dateOf(timestamp)
        lastDate = dateOf(timestamp)
        val ext = path.fileName.toString().substringAfterLast('.', "")
        val base = path.fileName.toString().substringBeforeLast('.')
        val rolledName = "$base.$date.$ext"
        val rolledPath = path.resolveSibling(rolledName)
        if (Files.exists(path)) {
            Files.move(path, rolledPath)
        }
    }

    private fun dateOf(timestamp: Instant): String =
        LocalDate.ofInstant(timestamp.toJavaInstant(), ZoneOffset.UTC).format(formatter)
}

/**
 * Compression type for rolled files.
 */
sealed interface Compression {
    fun compress(path: Path)

    data object Gzip : Compression {
        override fun compress(path: Path) {
            val gzPath = path.resolveSibling("${path.fileName}.gz")
            Files.newInputStream(path).use { input ->
                GZIPOutputStream(Files.newOutputStream(gzPath)).use { output ->
                    input.copyTo(output)
                }
            }
            Files.delete(path)
        }
    }
}

/**
 * Writes encoded log events to a file.
 * Supports optional rolling and compression.
 */
class FileAppender(
    private val encoder: Encoder,
    private val path: Path,
    private val rolling: RollingPolicy? = null,
    private val compression: Compression? = null,
) : LogHandler, AutoCloseable {

    private val lock = Any()
    private var writer: BufferedWriter = openWriter()

    override fun handle(event: LogEvent) {
        synchronized(lock) {
            if (rolling != null && rolling.shouldRoll(path, event.timestamp)) {
                writer.close()
                rolling.roll(path, event.timestamp)
                if (compression != null) {
                    compressRolledFiles()
                }
                writer = openWriter()
            }
            writer.write(encoder.encode(event))
            writer.newLine()
            writer.flush()
        }
    }

    override fun close() {
        synchronized(lock) {
            writer.close()
        }
    }

    private fun openWriter(): BufferedWriter {
        Files.createDirectories(path.parent)
        return Files.newBufferedWriter(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }

    private fun compressRolledFiles() {
        val dir = path.parent ?: return
        val baseName = path.fileName.toString()
        Files.list(dir).use { stream ->
            stream
                .filter { file ->
                    val name = file.fileName.toString()
                    name.startsWith(baseName) && name != baseName && !name.endsWith(".gz")
                }
                .forEach { file -> compression?.compress(file) }
        }
    }

    override fun toString(): String = "FileAppender($path)"
}
