package io.heapy.komok.wip

import io.heapy.komok.TransactionContext
import io.heapy.komok.User
import io.heapy.komok.UserContext
import org.jooq.DSLContext
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

class TransactionContextProvider(
    private val dslContext: DSLContext,
) : ContextProvider<TransactionContext> {
    override fun <R> context(
        body: TransactionContext.() -> R,
    ): R {
        return dslContext.transactionResult { cfg ->
            TransactionContext(cfg.dsl()).body()
        }
    }
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

context(UserContext)
fun testUserInfo() {
    println("Hello, world! I'm ${user.id}!")
}
