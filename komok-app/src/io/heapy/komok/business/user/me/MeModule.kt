package io.heapy.komok.business.user.me

import io.heapy.komok.business.user.UserDaoModule
import io.heapy.komok.business.user.session.UserSessionDaoModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
open class MeModule(
    private val userDaoModule: UserDaoModule,
    private val userSessionDaoModule: UserSessionDaoModule,
) {
    context(_: KomokContext)
    open suspend fun meService(): MeService =
        MeService(
            userDao = userDaoModule.userDao(),
            userSessionDao = userSessionDaoModule.userSessionDao(),
        )

    context(_: KomokContext)
    open suspend fun meRoute(): MeRoute =
        MeRoute(
            meService = meService(),
        )
}
