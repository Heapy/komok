package io.heapy.komok.business.user.session

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module
import io.heapy.kotomi.time.TimeSourceModule

@Module
class UserSessionDaoModule(
    private val mongoModule: MongoModule,
    private val timeSourceModule: TimeSourceModule,
) {
    context(_: KotomiContext)
    suspend fun userSessionDao(): UserSessionDao =
        UserSessionDao(
            timeSource = timeSourceModule.timeSource(),
            database = mongoModule.komokDatabase(),
        )
}
