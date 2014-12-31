package hi

import io.heapy.vipassana.logger.LoggingConfiguration
import io.heapy.vipassana.logger.LoggingLevel
import io.heapy.vipassana.logger.LoggingPattern
import io.heapy.vipassana.logger.LoggingSystem
import io.heapy.vipassana.logger.VipassanaLogger
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext

@State(Scope.Benchmark)
open class LoggerTest {
//    private var slf4j: Logger? = null
//    private val out = BufferedWriter(OutputStreamWriter(FileOutputStream(FileDescriptor.out), "UTF-8"), 2048)
//    private var logger: VipassanaLogger? = null
//    private var arg1: String? = null
//    private var arg2: String? = null
//    private var arg3: String? = null
//
//    @Setup
//    open fun setup() {
//        arg1 = "FirstArgument"
//        arg2 = "SecondArgument"
//        arg3 = "ThirdArgument"
//        slf4j = LoggerFactory.getLogger(LoggerTest::class.java)
//        logger = LoggingSystem(LoggingConfiguration(
//            level = LoggingLevel.TRACE,
//            pattern = LogbackLikeLoggingPattern,
//        )).getLogger("hi.LoggerTest")
//    }

//    @Benchmark
//    open fun messageOneArgumentInTheEnd() = runBlocking {
//        logger?.log(LoggingLevel.INFO, "Message is printed for this logger and with the argument: ", arg1)
//    }
//
//    @Benchmark
//    open fun messageOneArgumentInTheEndSlf4j() = runBlocking {
//        slf4j?.info("Message is printed for this logger and with the argument: {}", arg1)
//    }
//
//    @Benchmark
//    open fun messageTwoArgumentInTheEnd() = runBlocking {
//        logger?.log(LoggingLevel.INFO, "Message is printed for this logger and with arguments ", arg1, " and ", arg2)
//    }
//
//    @Benchmark
//    open fun simple() = runBlocking {
//        outtt.write("${formatter.format(Instant.now().atOffset(ZoneOffset.UTC))} ${Thread.currentThread().name} hi.LoggerTest INFO Message is printed for this logger and with arguments $arg1 and $arg2\n".toByteArray())
//        outtt.flush()
//    }
//
//    @Benchmark
//    open fun messageTwoArgumentInTheEndSlf4j() = runBlocking {
//        slf4j?.info("Message is printed for this logger and with arguments {} and {}", arg1, arg2)
//    }
}

private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

private val outtt = object : OutputStream() {
    override fun write(b: Int) {
        System.out.write(b)
    }

    override fun write(b: ByteArray) {
        System.out.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        System.out.write(b, off, len)
    }

    override fun flush() {
        System.out.flush()
    }
}

private object LogbackLikeLoggingPattern : LoggingPattern {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    override fun format(
        output: PrintStream,
        loggerName: String,
        level: LoggingLevel,
        vararg message: Any?,
        throwable: Throwable?,
        context: Map<String, Any?>?,
        thread: Thread,
        coroutineContext: CoroutineContext,
        instant: Instant,
    ) {
        output.print(formatter.format(instant.atZone(ZoneId.systemDefault())))
        output.print(" [")
        output.print(thread.name)
        output.print("] ")
        output.print(level.name)
        output.print(" ")
        output.print(loggerName)
        output.print(" - ")
        message.forEach { output.print(it) }
        output.println()
    }
}
