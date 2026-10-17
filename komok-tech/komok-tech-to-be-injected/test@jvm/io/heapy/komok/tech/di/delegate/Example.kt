package io.heapy.komok.tech.di.delegate

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Configuration

class HttpClient(
    val configuration: Configuration,
)

class UserDao(
    val configuration: Configuration,
) {
    fun getById(id: Long) = User(id, "User $id")
}

data class User(
    val id: Long,
    val name: String,
)

class UserService(
    val userDao: UserDao,
    val httpClient: HttpClient,
) {
    fun getUser(id: Long) = userDao.getById(id)
}

class UserController(
    val userService: UserService,
)

class Server(
    val userController: UserController,
) {
    fun start() {
        println("Server started")
    }
}

class UtilsModule {
    val configuration by bean {
        Configuration()
    }

    val httpClient by bean {
        HttpClient(
            configuration = configuration.value,
        )
    }
}

class DaoModule(
    val utilsModule: UtilsModule,
) {
    val userDao by bean {
        UserDao(
            configuration = utilsModule.configuration.value,
        )
    }
}

class ServiceModule(
    val utilsModule: UtilsModule,
    val daoModule: DaoModule,
) {
    val userService by bean {
        UserService(
            userDao = daoModule.userDao.value,
            httpClient = utilsModule.httpClient.value,
        )
    }
}

class ControllerModule(
    val serviceModule: ServiceModule,
) {
    val userController by bean {
        UserController(
            userService = serviceModule.userService.value,
        )
    }
}

class ApplicationModule(
    val controllerModule: ControllerModule,
) {
    val server by bean {
        Server(
            userController = controllerModule.userController.value,
        )
    }
}

fun main() {
    val app = buildModule<ApplicationModule>()
    app.server.value.start()
}

class UserServiceTest {
    @Test
    fun `test user service`() {
        val module = buildModule<ServiceModule>()

        val userDao = module.daoModule.userDao.mock {
            mockk {
                every {
                    getById(1)
                } returns User(
                    id = 1,
                    name = "Mocked user",
                )
            }
        }

        val userService = module.userService.value
        val user = userService.getUser(1)

        assertEquals(
            User(
                id = 1,
                name = "Mocked user",
            ),
            user,
        )

        verifySequence {
            val _ = userDao.getById(1)
        }
    }
}
