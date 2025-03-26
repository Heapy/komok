# Komok DI

## Not covered use-cases

### Reference modules in module customizers

Currently, it's not possible to reference future value of service from another module (or even the same module) in module customizer:

```kotlin
val yModule = createYModule {
    yService {
        YService(
            "Y1",
            TODO("Service from a different module can't be reached, to use as dependency")
        )
    }
}
```

And in case if only one argument need to be replaced, all arguments should be passed.
Something like auto-generated `copy` method for data classes?

### Mock single dependency without re-creating the whole tree

Having class like this:

```kotlin
class UserDao(
    private val timeSource: TimeSource,
) {
    fun updateName(
        name: String,
    ) {
        val currentTime = timeSource.currentTime()

        // save to db and use currentTime
    }
}
```

And test that trying to verify user state after calling `updateName`:

```kotlin
@Test
fun `updateName should update user name`() {
    val timeSource = TestTimeSource()
    val userDao = ... // get UserDao with TestTimeSource

    userDao.updateName("new name")

    timeSource.resetTime()

    assertEquals(
        User(
            name = "new name",
            lastUpdated = timeSource.currentTime()
        ),
        userDao.getUser()
    )
}
```

#### Approach 1: re-create affected parts of the three:

How to tweak model to allow such methods for testing?

```kotlin
@Test
fun `updateName should update user name`(
    applicationModuleFlatten: ApplicationModuleFlatten,
) {
    val timeSource = TestTimeSource()
    val derivedModule = applicationModuleFlatten.timeSourceModule.replaceTimeSource(timeSource)
    val userDao = derivedModule.daoModule.userDao // uses TestTimeSource

    userDao.updateName("new name")

    timeSource.resetTime()

    assertEquals(
        User(
            name = "new name",
            lastUpdated = timeSource.currentTime()
        ),
        userDao.getUser()
    )
}
```

Pros:

All affected parts are re-created. This is the most reliable way to test, ensuring correctness.
Changes isolated to current test, no requirements for locks.

Cons:

Some common services widely used can trigger almost full tree re-creation.
Example: object mapper, time source, etc. Too many re-creations can lead to slow tests.

#### Approach 2: re-create affected bean only (manual):

```kotlin
@Test
fun `updateName should update user name`(
    applicationModuleFlatten: ApplicationModuleFlatten,
) {
    val timeSource = TestTimeSource()
    val userDao = UserDao(
        datasource = applicationModuleFlatten.daoModule.datasource,
        timeSource = timeSource,
    )

    userDao.updateName("new name")

    timeSource.resetTime()

    assertEquals(
        User(
            name = "new name",
            lastUpdated = timeSource.currentTime()
        ),
        userDao.getUser()
    )
}
```

Pros:

Manually re-create only affected bean, additional boilerplate.

All affected parts are re-created. This is the most reliable way to test, ensuring correctness.
Changes isolated to current test, no requirements for locks.

Cons:

Some common services widely used can trigger almost full tree re-creation.
Example: object mapper, time source, etc. Too many re-creations can lead to slow tests.

#### Approach 3: Use contexts

Instead of using DI for all dependencies, use contexts for some of them:

```kotlin
class UserDao {
    context(tsc: TimeSourceContext)
    fun updateName(
        name: String,
    ) {
        val currentTime = tsc.timeSource.currentTime()

        // save to db and use currentTime
    }
}
```

And in test:

```kotlin
context(tsc: TestTimeSourceContext)
@Test
fun `updateName should update user name`() {
    val userDao = UserDao()

    userDao.updateName("new name")

    tsc.timeSource.resetTime()

    assertEquals(
        User(
            name = "new name",
            lastUpdated = tsc.timeSource.currentTime()
        ),
        userDao.getUser()
    )
}
```

While it's definitely solved the problem, it seems that this is not correct use of context.
Context is not DI replacement but a way to provide additional information to method based on caller knowledge.

Good examples of context:
- userContext (who is current user in request scope)
- transactionContext (current transaction running)

#### Approach 4: Add copy functions to modules

In test, be able to create a deep copy of modules, but be able to replace a single bean:

```kotlin
@Test
fun `updateName should update user name`(
    applicationModuleFlatten: ApplicationModuleFlatten,
) {
    val timeSource = TestTimeSource()
    val userDao = applicationModuleFlatten.daoModule.copy(
        timeSource = timeSource,
    ).userDao

    userDao.updateName("new name")

    timeSource.resetTime()

    assertEquals(
        User(
            name = "new name",
            lastUpdated = timeSource.currentTime()
        ),
        userDao.getUser()
    )
}
```
