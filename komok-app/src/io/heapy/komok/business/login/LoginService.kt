package io.heapy.komok.business.login

import io.heapy.komok.business.user.UserDao
import io.heapy.komok.business.user.session.UserSessionDao
import io.heapy.komok.infra.argon2.PasswordHasher
import io.heapy.komok.infra.http.server.errors.badRequestError
import io.heapy.komok.infra.session_token.SessionTokenService
import io.heapy.komok.infra.totp.TimeBasedOneTimePasswordService
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class LoginService(
    private val userDao: UserDao,
    private val userSessionDao: UserSessionDao,
    private val sessionTokenService: SessionTokenService,
    private val timeBasedOneTimePasswordService: TimeBasedOneTimePasswordService,
    private val passwordHasher: PasswordHasher,
) {
    suspend fun login(
        loginRequest: LoginRequest,
        ip: String,
        userAgent: String,
    ): LoginResponse {
        val user = userDao
            .getUserByEmail(
                email = loginRequest.email,
            )
            ?: badRequestError("email", "User not found")

        val passwordValid = passwordHasher.verify(
            password = loginRequest.password,
            hash = user.hash,
        )

        if (!passwordValid) {
            badRequestError("password", "Invalid password")
        }

        val isTotpValid = timeBasedOneTimePasswordService.validate(
            secret = user.authenticatorKey,
            totp = loginRequest.otp,
        )

        if (!isTotpValid) {
            badRequestError("otp", "Invalid OTP")
        }

        val sessionToken = sessionTokenService.generate()

        userSessionDao.createSession(
            userId = user.id,
            maxAge = sessionTokenMaxAge,
            ip = ip,
            device = userAgent,
            token = sessionToken,
        )

        return LoginResponse(
            sessionToken = sessionToken,
            // Make sure that browser will invalidate session before server
            maxAge = sessionTokenMaxAge - 10.seconds,
        )
    }

    private companion object {
        private val sessionTokenMaxAge = 24.hours
    }
}
