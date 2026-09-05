package io.heapy.komok.business.user

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module

@Module
open class UserDaoModule(
    private val mongoModule: MongoModule,
) {
    context(_: KomokContext)
    open suspend fun userDao(): UserDao =
        UserDao(
            database = mongoModule.komokDatabase(),
        )
}
