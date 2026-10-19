package io.heapy.komok.business.user.me

import io.heapy.komok.business.user.UserDaoModule
import io.heapy.komok.business.user.session.UserSessionDaoModule
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class MeModule(
    private val userDaoModule: UserDaoModule,
    private val userSessionDaoModule: UserSessionDaoModule,
) {
    context(_: KotomiContext)
    suspend fun meService(): MeService =
        MeService(
            userDao = userDaoModule.userDao(),
            userSessionDao = userSessionDaoModule.userSessionDao(),
        )

    context(_: KotomiContext)
    suspend fun meRoute(): MeRoute =
        MeRoute(
            meService = meService(),
        )
}
