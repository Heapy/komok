#!/usr/bin/env kotlinr

// Run from the repository root: kotlinr landing/serve.main.kts [port]
// Uses the JDK 18+ static file server; no external dependencies.
import com.sun.net.httpserver.SimpleFileServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch

require(args.size <= 1) { "Usage: kotlinr landing/serve.main.kts [port]" }
val port = if (args.isEmpty()) 4174 else requireNotNull(args.single().toIntOrNull()) {
    "Port must be an integer from 1 to 65535."
}
require(port in 1..65535) { "Port must be an integer from 1 to 65535." }

val directory = Path.of("landing/build/site").toAbsolutePath().normalize()
require(Files.isRegularFile(directory.resolve("index.html"))) {
    "Run kotlinr landing/build.main.kts from the repository root first."
}

val server = SimpleFileServer.createFileServer(
    InetSocketAddress("127.0.0.1", port),
    directory,
    SimpleFileServer.OutputLevel.INFO,
)
Runtime.getRuntime().addShutdownHook(Thread { server.stop(0) })
server.start()
println("Preview: http://127.0.0.1:$port/ (Ctrl+C to stop)")
CountDownLatch(1).await()
