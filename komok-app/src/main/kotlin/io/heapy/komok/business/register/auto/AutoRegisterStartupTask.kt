package io.heapy.komok.business.register.auto

import io.heapy.komok.business.user.UserDao
import io.heapy.komok.infra.argon2.PasswordHasher
import io.heapy.komok.infra.password.PasswordGenerator
import io.heapy.komok.infra.startup_tasks.StartupTask
import io.heapy.komok.infra.totp.GenerateTimeBasedOneTimeKeyService
import io.heapy.komok.tech.logging.Logger

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
        log.info("AutoRegisterStartupTask start")
        if (userDao.getUserCount() == 0L) {
            val password = passwordGenerator.generate()
            val email = autoRegisterStartupTaskConfiguration.email
            val authenticatorKey = generateTimeBasedOneTimeKeyService
                .generate(GenerateTimeBasedOneTimeKeyService.Algorithm.SHA512)
            log.info(
                """
                No users found, creating default user:
                Email: $email
                Password: $password
                Authenticator Key: $authenticatorKey
                """.trimIndent(),
            )

            userDao.insertUser(
                email = autoRegisterStartupTaskConfiguration.email,
                hash = passwordHasher.hash(password),
                authenticatorKey = authenticatorKey,
            )

            log.info("Default user created")
        } else {
            log.info("Users already found")
        }
        log.info("AutoRegisterStartupTask end")
    }

    private companion object : Logger()
}
