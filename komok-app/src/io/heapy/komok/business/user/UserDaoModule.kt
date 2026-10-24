package io.heapy.komok.business.user

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module

@Module
class UserDaoModule(
    private val mongoModule: MongoModule,
) {
    context(_: KotomiContext)
    suspend fun userDao(): UserDao =
        UserDao(
            database = mongoModule.komokDatabase(),
        )
}
