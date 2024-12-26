package io.heapy.komok.infra.totp

import io.heapy.komok.TestTimeSource
import io.heapy.komok.UnitTest
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

class TimeBasedOneTimePasswordTest {
    @UnitTest
    fun generate() {
        val timeSource = TestTimeSource()
        val module = createTimeBasedOneTimePasswordModule {
            timeSourceModule {
                timeSource {
                    timeSource
                }
            }
        }

        timeSource.reset(
            new = Instant.ofEpochSecond(1720159128),
        )

        val totp = module.timeBasedOneTimePassword
            .generate("JBSWY3DPEHPK3PXP")

        assertEquals(
            "476288",
            totp,
        )
    }

    @UnitTest
    fun validate() {
        val timeSource = TestTimeSource()
        val module = createTimeBasedOneTimePasswordModule {
            timeSourceModule {
                timeSource {
                    timeSource
                }
            }
        }

        timeSource.reset(
            new = Instant.ofEpochSecond(1720159128),
        )

        val decision1 = module.timeBasedOneTimePassword
            .validate(
                secret = "JBSWY3DPEHPK3PXP",
                otp = "476288",
            )

        assertTrue(decision1)

        timeSource.reset(
            new = Instant.ofEpochSecond(1720159158),
        )

        val decision2 = module.timeBasedOneTimePassword
            .validate(
                secret = "JBSWY3DPEHPK3PXP",
                otp = "476288",
            )

        assertFalse(decision2)
    }
}