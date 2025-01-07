package io.heapy.komok.business.user

import io.heapy.komok.dao.mg.MongoModule
import io.heapy.komok.tech.di.lib.Module

@Module
open class UserDaoModule(
    private val mongoModule: MongoModule,
) {
    open val userDao by lazy {
        UserDao(
            database = mongoModule.komokDatabase,
        )
    }
}
