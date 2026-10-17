package io.heapy.komok.business.register.auto

import io.heapy.komok.business.user.UserDaoModule
import io.heapy.komok.infra.argon2.PasswordHasherModule
import io.heapy.komok.infra.password.PasswordGeneratorModule
import io.heapy.komok.infra.totp.TimeBasedOneTimePasswordModule
import io.heapy.komok.tech.config.ConfigurationModule
import io.heapy.komok.tech.di.lib.Module
import kotlin.getValue

@Module
open class AutoRegisterStartupTaskModule(
    private val userDaoModule: UserDaoModule,
    private val passwordGeneratorModule: PasswordGeneratorModule,
    private val passwordHasherModule: PasswordHasherModule,
    private val configurationModule: ConfigurationModule,
    private val timeBasedOneTimePasswordModule: TimeBasedOneTimePasswordModule,
) {
    open val autoRegisterStartupTask by lazy {
        AutoRegisterStartupTask(
            userDao = userDaoModule.userDao,
            passwordGenerator = passwordGeneratorModule.passwordGenerator,
            passwordHasher = passwordHasherModule.passwordHasher,
            autoRegisterStartupTaskConfiguration = autoRegisterStartupTaskConfiguration,
            generateTimeBasedOneTimeKeyService = timeBasedOneTimePasswordModule.generateTimeBasedOneTimeKeyService,
        )
    }

    open val autoRegisterStartupTaskConfiguration: AutoRegisterStartupTaskConfiguration by lazy {
        configurationModule
            .config
            .read(AutoRegisterStartupTaskConfiguration.serializer(), "auto-register")
    }
}
