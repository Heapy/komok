package io.heapy.komok.dao.pg

import org.jooq.DSLContext

sealed interface TransactionContext

fun TransactionContext(
    dslContext: DSLContext,
): TransactionContext =
    JooqTransactionContext(
        dslContext = dslContext,
    )

private data class JooqTransactionContext(
    val dslContext: DSLContext,
) : TransactionContext

fun TransactionContext.unwrap(): DSLContext =
    when (this) {
        is JooqTransactionContext -> dslContext
    }

suspend fun <R> TransactionContext.dslContext(
    body: suspend DSLContext.() -> R,
): R =
    unwrap().body()

val DSLContext.dslContext: DSLContext
    get() = this
