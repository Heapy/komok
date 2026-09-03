# To Be Injected

Minimal and simple dependency injection library for Kotlin Multiplatform.
Based on idea of using Kotlin `lazy` delegate:

```kotlin
open class MyModule {
    open val myBean by lazy {
        MyBean()
    }
}
```

Modules do not need to inherit framework base types and remain directly usable
in tests.

## Installation

Add the following to the source set that uses the library:

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation("io.heapy.komok:komok-tech-to-be-injected:1.1.0")
    }
}
```

Published targets include JVM, JS, Wasm-JS, Wasm-WASI, Linux, MinGW,
macOS, iOS, tvOS, and watchOS.

## Usage

This is a simplified example of multi-module project with dependencies between them.

```kotlin
import io.heapy.komok.tech.di.delegate.bean
import io.heapy.komok.tech.di.delegate.buildModule
import io.heapy.komok.tech.di.delegate.buildModules
import io.heapy.komok.tech.di.delegate.ModuleGraphBuilder

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
fun applicationGraph(): ModuleGraphBuilder.() -> Unit = {
    module<UtilsModule> {
        UtilsModule()
    }
    module<DaoModule> {
        DaoModule(utilsModule = invoke())
    }
    module<ServiceModule> {
        ServiceModule(
            utilsModule = invoke(),
            daoModule = invoke(),
        )
    }
    module<ControllerModule> {
        ControllerModule(serviceModule = invoke())
    }
    module<ApplicationModule> {
        ApplicationModule(controllerModule = invoke())
    }
}

fun main() {
    val app = buildModule<ApplicationModule>(applicationGraph())
    app.server.value.start()
}
```

Factories are explicit because Kotlin/Native, Kotlin/JS, and Kotlin/Wasm do
not provide JVM constructor reflection. On JVM, the reflective
shortcut remains available:

```kotlin
val app = buildModule<ApplicationModule>()
```

The portable graph can also return a registry when tests or application code
need more than the root module:

```kotlin
val modules = buildModules(applicationGraph())
val app = modules<ApplicationModule>()
val utils = modules<UtilsModule>()
```

## Testing

The same graph works in common tests. Beans typed as interfaces can be replaced
with portable fakes before their first use:

```kotlin
class UserServiceTest {
    @Test
    fun `test user service`() {
        val modules = buildModules(applicationGraph())
        val module = modules<ServiceModule>()

        module.daoModule.userDao.setValue(
            UserDao(configuration = Configuration()),
        )

        val userService = module.userService.value
        val user = userService.getUser(1)

        assertEquals(
            User(
                id = 1,
                name = "User 1",
            ),
            user,
        )

    }
}
```

## License

This project is licensed under the Apache License 2.0 – see the [LICENSE-Apache2](../LICENSE-Apache2) file for details.
