package io.heapy.komok.business.login

import io.heapy.komok.TimeSourceContext
import io.heapy.komok.business.user.UserDao
import io.heapy.komok.business.user.session.SessionTokenGenerator

class LoginService(
    private val userDao: UserDao,
    private val sessionTokenGenerator: SessionTokenGenerator
) {
    context(TimeSourceContext)
    suspend fun login(
        email: String,
        password: String,
        totp: String,
    ): String {
        val user = userDao.getUser(email)
            ?: throw IllegalArgumentException("User not found")



        val sessionToken = sessionTokenGenerator.generate()

        return sessionToken
    }
}
