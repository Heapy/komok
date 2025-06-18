package io.heapy.komok.tech.ktor.htmx

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HtmxKtorExtensionsTest {
    @Test
    fun `should return true when HX-Request header is set to true`() {
        val mockHeaders = mockk<Headers> {
            val mock = this
            every {
                mock["HX-Request"]
            } returns "true"
        }
        val mockRequest = mockk<ApplicationRequest> {
            every {
                headers
            } returns mockHeaders
        }
        val mockCall = mockk<ApplicationCall> {
            every {
                request
            } returns mockRequest
        }

        assertTrue(mockCall.isHtmxRequest)

        verifySequence {
            mockCall.request
            mockRequest.headers
            mockHeaders["HX-Request"]
        }
        confirmVerified(mockCall, mockRequest, mockHeaders)
    }

    @Test
    fun `should return false when HX-Request header is not present`() {
        val mockHeaders = mockk<Headers> {
            val mock = this
            every {
                mock["HX-Request"]
            } returns null
        }
        val mockRequest = mockk<ApplicationRequest> {
            every {
                headers
            } returns mockHeaders
        }
        val mockCall = mockk<ApplicationCall> {
            every {
                request
            } returns mockRequest
        }

        assertFalse(mockCall.isHtmxRequest)

        verifySequence {
            mockCall.request
            mockRequest.headers
            mockHeaders["HX-Request"]
        }
        confirmVerified(mockCall, mockRequest, mockHeaders)
    }

    @Test
    fun `should return false when HX-Request header has value other than true`() {
        val mockHeaders = mockk<Headers> {
            val mock = this
            every {
                mock["HX-Request"]
            } returns "1"
        }
        val mockRequest = mockk<ApplicationRequest> {
            every {
                headers
            } returns mockHeaders
        }
        val mockCall = mockk<ApplicationCall> {
            every {
                request
            } returns mockRequest
        }

        assertFalse(mockCall.isHtmxRequest)

        verifySequence {
            mockCall.request
            mockRequest.headers
            mockHeaders["HX-Request"]
        }
        confirmVerified(mockCall, mockRequest, mockHeaders)
    }

    @Test
    fun `should be case sensitive and only accept lowercase true`() {
        val mockHeaders = mockk<Headers> {
            val mock = this
            every {
                mock["HX-Request"]
            } returns "TRUE"
        }
        val mockRequest = mockk<ApplicationRequest> {
            every {
                headers
            } returns mockHeaders
        }
        val mockCall = mockk<ApplicationCall> {
            every {
                request
            } returns mockRequest
        }

        assertFalse(mockCall.isHtmxRequest)

        verifySequence {
            mockCall.request
            mockRequest.headers
            mockHeaders["HX-Request"]
        }
        confirmVerified(mockCall, mockRequest, mockHeaders)
    }
}
