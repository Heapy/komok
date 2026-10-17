package io.heapy.komok.tech.decorators.test

import io.heapy.komok.tech.decorators.plugins.logging.annotations.Log
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface UserService {
    @Log
    fun getUser(id: Long, logger: Logger): String

    @Log
    fun createUser(name: String, email: String, logger: Logger): Long
}

open class UserServiceImpl : UserService {
    override fun getUser(id: Long, logger: Logger): String {
        return "User $id"
    }

    override fun createUser(name: String, email: String, logger: Logger): Long {
        return System.currentTimeMillis()
    }
}

fun main() {
    val logger = LoggerFactory.getLogger("TestService")
    val userService = UserServiceImpl()

    println(userService.getUser(123, logger))
    println(userService.createUser("John", "john@example.com", logger))
}
