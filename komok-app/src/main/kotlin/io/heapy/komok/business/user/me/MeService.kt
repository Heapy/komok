package io.heapy.komok.business.user.me

import io.heapy.komok.business.user.UserDao
import io.heapy.komok.business.user.session.UserSessionDao
import kotlinx.serialization.Serializable

class MeService(
    private val userSessionDao: UserSessionDao,
    private val userDao: UserDao,
) {
    suspend fun getMe(
        ip: String,
        session: String,
    ): Me {
        val userSession = userSessionDao.verifySession(
            token = session,
            ip = ip,
        )

        val user = userDao.getUserById(userSession.userId)
            ?: error("User not found for id: ${userSession.userId}")

        return Me(
            email = user.email,
        )
    }

    @Serializable
    data class Me(
        val email: String,
    )
}
