package io.heapy.komok.business.login

import com.typesafe.config.ConfigFactory
import io.heapy.komok.KomokBaseTest
import io.heapy.komok.TestTimeSourceContext
import io.heapy.komok.UnitTest
import io.heapy.komok.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class JwtServiceTest : KomokBaseTest {
    context(TestTimeSourceContext)
    @UnitTest
    fun `jwt token generated using configuration`() {
        val module = createJwtModule {
            configurationModule {
                config {
                    ConfigFactory.parseString(
                        """
                            jwt {
                                audience = "audience"
                                issuer = "issuer"
                                expiration = 10 minutes
                                realm = "realm"
                                secret = "3b785e0ba085f91708b64b97c8131b49cc6b31e045a118c78c23bac9c1f04b8d26a6208a9e37ca65b535b6af42866b8e16bee15fb69485ae5dfadc371b731860"
                            }
                        """.trimIndent(),
                    )
                }
            }
        }

        timeSource.reset(
            new = Instant.ofEpochSecond(1720159128),
        )

        val token = module.jwtService.createToken(
            User(id = "1"),
        )

        assertEquals(
            "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhdWRpZW5jZSIsImlzcyI6Imlzc3VlciIsImlkIjoiMSIsImV4cCI6MTcyMDE1OTcyOH0.8cFIbUkrFEDTMEstyUKzMM_Knlictchm1ejqQOwZrAGgyOPuSVx5rJ-IZb7QI1aLBHBRrfVJP_oQ_6TU2_EYFw",
            token,
        )
    }

    @UnitTest
    fun `jwt require minimum secret size`() {
        val module = createJwtModule {
            configurationModule {
                config {
                    ConfigFactory.parseString(
                        """
                            jwt {
                                audience = "audience"
                                issuer = "issuer"
                                expiration = 10 minutes
                                realm = "realm"
                                secret = "3b785e0ba085f91708b64b97c8131b49cc6b31e045a118c78c23bac9c1f04b8d26a6208a9e37ca65b535b6af42866b8e16bee15fb69485ae5dfadc371b73186"
                            }
                        """.trimIndent(),
                    )
                }
            }
        }

        val exception = assertThrows<IllegalArgumentException> {
            module.jwtService
        }

        assertEquals(
            "Secret must be at least 128 characters long",
            exception.message,
        )
    }
}
