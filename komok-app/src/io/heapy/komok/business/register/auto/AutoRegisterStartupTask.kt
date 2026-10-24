package io.heapy.komok.business.register.auto

import io.heapy.komok.business.user.UserDao
import io.heapy.komok.infra.argon2.PasswordHasher
import io.heapy.komok.infra.password.PasswordGenerator
import io.heapy.komok.infra.startup_tasks.StartupTask
import io.heapy.komok.infra.totp.GenerateTimeBasedOneTimeKeyService

/**
 * This task creates a new user if no users found in the database
 */
class AutoRegisterStartupTask(
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher,
    private val passwordGenerator: PasswordGenerator,
    private val generateTimeBasedOneTimeKeyService: GenerateTimeBasedOneTimeKeyService,
    private val autoRegisterStartupTaskConfiguration: AutoRegisterStartupTaskConfiguration,
) : StartupTask {
    override suspend fun execute() {
        println("AutoRegisterStartupTask start")
        if (userDao.getUserCount() == 0L) {
            val password = passwordGenerator.generate()
            val email = autoRegisterStartupTaskConfiguration.email
            val authenticatorKey = generateTimeBasedOneTimeKeyService
                .generate(GenerateTimeBasedOneTimeKeyService.Algorithm.SHA512)
            println(
                """
                No users found, creating default user:
                Email: $email
                Password: $password
                Authenticator Key: $authenticatorKey
                """.trimIndent(),
            )

            val id = userDao.insertUser(
                email = autoRegisterStartupTaskConfiguration.email,
                hash = passwordHasher.hash(password),
                authenticatorKey = authenticatorKey,
            )

            println("Default user with id=$id created")
        } else {
            println("Users already found")
        }
        println("AutoRegisterStartupTask end")
    }
}
