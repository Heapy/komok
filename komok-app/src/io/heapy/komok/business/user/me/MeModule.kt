package io.heapy.komok.business.user.me

import io.heapy.komok.business.user.UserDaoModule
import io.heapy.komok.business.user.session.UserSessionDaoModule
import io.heapy.komok.tech.di.lib.Module

@Module
open class MeModule(
    private val userDaoModule: UserDaoModule,
    private val userSessionDaoModule: UserSessionDaoModule,
) {
    open val meService by lazy {
        MeService(
            userDao = userDaoModule.userDao,
            userSessionDao = userSessionDaoModule.userSessionDao,
        )
    }

    open val meRoute by lazy {
        MeRoute(
            meService = meService,
        )
    }
}
