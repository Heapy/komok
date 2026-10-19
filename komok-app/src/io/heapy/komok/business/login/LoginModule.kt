package io.heapy.komok.business.login

import io.heapy.komok.business.user.UserDaoModule
import io.heapy.komok.business.user.session.UserSessionDaoModule
import io.heapy.komok.infra.argon2.PasswordHasherModule
import io.heapy.komok.infra.session_token.SessionTokenServiceModule
import io.heapy.komok.infra.totp.TimeBasedOneTimePasswordModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class LoginModule(
    private val userDaoModule: UserDaoModule,
    private val userSessionDaoModule: UserSessionDaoModule,
    private val sessionTokenServiceModule: SessionTokenServiceModule,
    private val timeBasedOneTimePasswordModule: TimeBasedOneTimePasswordModule,
    private val passwordHasherModule: PasswordHasherModule,
) {
    context(_: KomokContext)
    suspend fun loginService(): LoginService =
        LoginService(
            userDao = userDaoModule.userDao(),
            userSessionDao = userSessionDaoModule.userSessionDao(),
            sessionTokenService = sessionTokenServiceModule.sessionTokenService(),
            timeBasedOneTimePasswordService = timeBasedOneTimePasswordModule.timeBasedOneTimePasswordService(),
            passwordHasher = passwordHasherModule.passwordHasher(),
        )

    context(_: KomokContext)
    suspend fun loginRoute(): LoginRoute =
        LoginRoute(
            loginService = loginService(),
        )
}
