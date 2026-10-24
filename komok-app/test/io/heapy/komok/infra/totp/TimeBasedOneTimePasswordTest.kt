package io.heapy.komok.infra.totp

import io.heapy.kotomi.runtime.*
import io.heapy.komok.UnitTest
import io.heapy.kotomi.time.TimeSourceModule
import io.heapy.kotomi.time.TestTimeSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class TimeBasedOneTimePasswordTest {
    @UnitTest
    fun generate() = runTest {
        val timeSource = TestTimeSource()
        val graph = graph<TimeBasedOneTimePasswordModule>().fork {
            replace(TimeSourceModule.Bindings.timeSource, timeSource)
        }

        timeSource.reset(
            new = Instant.ofEpochSecond(1720159128),
        )

        val totp = graph { timeBasedOneTimePasswordService() }
            .generate("JBSWY3DPEHPK3PXP")

        assertEquals(
            "476288",
            totp,
        )
    }

    @UnitTest
    fun validate() = runTest {
        val timeSource = TestTimeSource()
        val graph = graph<TimeBasedOneTimePasswordModule>().fork {
            replace(TimeSourceModule.Bindings.timeSource, timeSource)
        }

        timeSource.reset(
            new = Instant.ofEpochSecond(1720159128),
        )

        val decision1 = graph { timeBasedOneTimePasswordService() }
            .validate(
                secret = "JBSWY3DPEHPK3PXP",
                totp = "476288",
            )

        assertTrue(decision1)

        timeSource.reset(
            new = Instant.ofEpochSecond(1720159158),
        )

        val decision2 = graph { timeBasedOneTimePasswordService() }
            .validate(
                secret = "JBSWY3DPEHPK3PXP",
                totp = "476288",
            )

        assertFalse(decision2)
    }
}
