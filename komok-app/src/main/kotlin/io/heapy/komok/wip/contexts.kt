package io.heapy.komok.wip

import io.heapy.komok.auth.common.User
import io.heapy.komok.auth.common.UserContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface ContextProviders {
    val providers: Map<KType, ContextProvider<*>>
}

data class DefaultContextProviders(
    override val providers: Map<KType, ContextProvider<*>>
) : ContextProviders

inline operator fun <reified T> ContextProviders.plus(
    other: ContextProvider<T>,
): DefaultContextProviders {
    return DefaultContextProviders(
        providers = providers + (typeOf<T>() to other),
    )
}

interface ContextProvider<T> {
    fun <R> context(body: T.() -> R): R
}

inline fun <reified T : Any, R> ContextProviders.runWithContexts(
    noinline body: T.() -> R,
): R {
    @Suppress("UNCHECKED_CAST")
    val provider = providers[typeOf<T>()] as ContextProvider<T>
    return provider.context(body)
}

inline fun <reified T> ContextProvider<T>.asProviders(): DefaultContextProviders {
    return DefaultContextProviders(
        providers = mapOf(typeOf<T>() to this),
    )
}

class UserContextProvider(
    private val user: User,
) : ContextProvider<UserContext> {
    override fun <R> context(
        body: UserContext.() -> R,
    ): R {
        return UserContext(user).body()
    }
}

fun main() {
    val providers = UserContextProvider(
        user = User(id = "1"),
    ).asProviders()

    providers.runWithContexts<UserContext, Unit> {
        testUserInfo()
    }
}

context(uc: UserContext)
fun testUserInfo() {
    println("Hello, world! I'm ${uc.user.id}!")
}
