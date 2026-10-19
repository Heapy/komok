package io.heapy.komok.business.user.session

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import io.heapy.komok.tech.time.TimeSourceModule

@Module
class UserSessionDaoModule(
    private val mongoModule: MongoModule,
    private val timeSourceModule: TimeSourceModule,
) {
    context(_: KomokContext)
    suspend fun userSessionDao(): UserSessionDao =
        UserSessionDao(
            timeSource = timeSourceModule.timeSource(),
            database = mongoModule.komokDatabase(),
        )
}
