package io.heapy.komok.business.user.session

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.komok.tech.di.lib.Module
import io.heapy.komok.tech.time.TimeSourceModule

@Module
open class UserSessionDaoModule(
    private val mongoModule: MongoModule,
    private val timeSourceModule: TimeSourceModule,
) {
    open val userSessionDao by lazy {
        UserSessionDao(
            timeSource = timeSourceModule.timeSource,
            database = mongoModule.komokDatabase,
        )
    }
}
