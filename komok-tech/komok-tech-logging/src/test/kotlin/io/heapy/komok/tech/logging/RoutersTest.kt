package io.heapy.komok.tech.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class RoutersTest {

    private object TenantId : Mdc.Key<String>("tenantId")

    private fun event(
        loggerName: String = "com.example.Test",
        mdc: Mdc = Mdc.Empty,
    ) = LogEvent(
        level = Level.INFO,
        loggerName = loggerName,
        message = "test",
        throwable = null,
        timestamp = Clock.System.now(),
        mdc = mdc,
    )

    @Test
    fun `LoggerRouter routes by prefix`() {
        val audit = mutableListOf<LogEvent>()
        val general = mutableListOf<LogEvent>()

        val router = LoggerRouter(
            routes = listOf(
                "com.example.audit" to LogHandler { audit.add(it) },
            ),
            fallback = LogHandler { general.add(it) },
        )

        router.handle(event(loggerName = "com.example.audit"))
        router.handle(event(loggerName = "com.example.audit.Detail"))
        router.handle(event(loggerName = "com.example.service"))

        assertEquals(2, audit.size)
        assertEquals(1, general.size)
    }

    @Test
    fun `LoggerRouter uses fallback when no route matches`() {
        val fallback = mutableListOf<LogEvent>()

        val router = LoggerRouter(
            routes = emptyList(),
            fallback = LogHandler { fallback.add(it) },
        )

        router.handle(event())
        assertEquals(1, fallback.size)
    }

    @Test
    fun `MdcRouter routes by MDC value`() {
        val acme = mutableListOf<LogEvent>()
        val other = mutableListOf<LogEvent>()

        val router = MdcRouter(
            key = TenantId,
            routes = mapOf(
                "acme" to LogHandler { acme.add(it) },
            ),
            fallback = LogHandler { other.add(it) },
        )

        router.handle(event(mdc = Mdc.of(TenantId, "acme")))
        router.handle(event(mdc = Mdc.of(TenantId, "globex")))
        router.handle(event())

        assertEquals(1, acme.size)
        assertEquals(2, other.size)
    }

    @Test
    fun `Router with generic discriminator`() {
        val errors = mutableListOf<LogEvent>()
        val other = mutableListOf<LogEvent>()

        val router = Router(
            discriminator = { it.level },
            routes = mapOf(
                Level.ERROR to LogHandler { errors.add(it) },
            ),
            fallback = LogHandler { other.add(it) },
        )

        router.handle(event().let {
            LogEvent(Level.ERROR, it.loggerName, it.message, it.throwable, it.timestamp, it.mdc)
        })
        router.handle(event())

        assertEquals(1, errors.size)
        assertEquals(1, other.size)
    }

    @Test
    fun `Tee dispatches to matching branches`() {
        val errors = mutableListOf<LogEvent>()
        val all = mutableListOf<LogEvent>()

        val tee = Tee(
            branches = listOf(
                { event: LogEvent -> event.level.ordinal >= Level.ERROR.ordinal } to LogHandler { errors.add(it) },
                { _: LogEvent -> true } to LogHandler { all.add(it) },
            ),
        )

        val errorEvent = LogEvent(Level.ERROR, "test", "err", null, Clock.System.now(), Mdc.Empty)
        val infoEvent = event()

        tee.handle(errorEvent)
        tee.handle(infoEvent)

        assertEquals(1, errors.size)
        assertEquals(2, all.size)
    }
}
