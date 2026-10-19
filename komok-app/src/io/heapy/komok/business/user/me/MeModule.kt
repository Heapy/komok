package io.heapy.komok.business.user.me

import io.heapy.komok.business.user.UserDaoModule
import io.heapy.komok.business.user.session.UserSessionDaoModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
class MeModule(
    private val userDaoModule: UserDaoModule,
    private val userSessionDaoModule: UserSessionDaoModule,
) {
    context(_: KomokContext)
    suspend fun meService(): MeService =
        MeService(
            userDao = userDaoModule.userDao(),
            userSessionDao = userSessionDaoModule.userSessionDao(),
        )

    context(_: KomokContext)
    suspend fun meRoute(): MeRoute =
        MeRoute(
            meService = meService(),
        )
}
