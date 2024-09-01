# To Been Injected

Minimal and simple dependency injection library for Kotlin.
Based on idea of using Kotlin `lazy` delegate:

```kotlin
open class MyModule {
    open val myBean by lazy {
        MyBean()
    }
}
```

But not requiring to extend modules for testing and manually build module tree.

## Installation

Add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.heapy.komok:komok-tech-to-been-injected:1.0.7")
}
```

## Usage

This is a simplified example of multi-module project with dependencies between them.

```kotlin
import io.heapy.komok.tech.di.delegate.bean
import io.heapy.komok.tech.di.delegate.buildModule

// UtilsModule.kt
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

// DaoModule.kt
class DaoModule(
    val utilsModule: UtilsModule,
) {
    val userDao by bean {
        UserDao(
            configuration = utilsModule.configuration.value,
        )
    }
}

// ServiceModule.kt
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

// ControllerModule.kt
class ControllerModule(
    val serviceModule: ServiceModule,
) {
    val userController by bean {
        UserController(
            userService = serviceModule.userService.value,
        )
    }
}

// ApplicationModule.kt
class ApplicationModule(
    val controllerModule: ControllerModule,
) {
    val server by bean {
        Server(
            userController = controllerModule.userController.value,
        )
    }
}

// main.kt
fun main() {
    val app = buildModule<ApplicationModule>()
    app.server.value.start()
}

// UserServiceTest.kt
class UserServiceTest {
    @Test
    fun `test user service`() {
        // Create module with all dependencies
        val module = buildModule<ServiceModule>()

        // Mock UserService dependency
        module.daoModule.userDao.mock {
            mockk {
                every {
                    getById(1)
                } returns User(
                    id = 1,
                    name = "Mocked user",
                )
            }
        }

        // Run service method
        val userService = module.userService.value
        val user = userService.getUser(1)

        // Assert Result
        assertEquals(
            User(
                id = 1,
                name = "Mocked user",
            ),
            user,
        )

        // Verify calls
        verifySequence {
            module.daoModule.userDao.value.getById(1)
        }
    }
}
```
