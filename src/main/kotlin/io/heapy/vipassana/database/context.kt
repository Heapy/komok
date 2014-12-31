package io.heapy.vipassana.database

import java.sql.Connection

interface TransactionContext : DatabaseContext {
    fun startTransaction()
    fun commitTransaction()
    fun rollbackTransaction()
}

interface DatabaseContext {
    fun <T> execute(block: (Connection) -> T): T
    fun <T> executeInTransaction(block: (Connection) -> T): T
}

interface ConnectionProvider {
    val connection: Connection
}

fun ConnectionProvider.transactionContext(): TransactionContext
    = HikariTransactionContext(this)

fun ConnectionProvider.databaseContext(): DatabaseContext
    = HikariDatabaseContext(this)

internal class HikariTransactionContext(
    private val connectionProvider: ConnectionProvider,
) : TransactionContext, AutoCloseable {
    private var connection: Connection? = null

    override fun startTransaction() {
        connectionProvider.connection.let {
            connection = it
            it.autoCommit = false
        }
    }

    override fun commitTransaction() =
        connection
            ?.commit()
            ?: error("No active transaction")

    override fun rollbackTransaction() =
        connection
            ?.rollback()
            ?: error("No active transaction")

    override fun <T> execute(block: (Connection) -> T): T =
        connection
            ?.let(block)
            ?: error("No active transaction")

    override fun <T> executeInTransaction(block: (Connection) -> T): T =
        connectionProvider.connection.use { connection ->
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                result
            } catch (e: Exception) {
                connection.rollback()
                throw e
            }
        }

    override fun close() {
        connection
            ?.close()
            ?: error("No active transaction")
        connection = null
    }
}

internal class HikariDatabaseContext(
    private val connectionProvider: ConnectionProvider,
) : DatabaseContext {
    override fun <T> execute(block: (Connection) -> T): T =
        connectionProvider.connection.use(block)

    override fun <T> executeInTransaction(block: (Connection) -> T): T =
        connectionProvider.connection.use { connection ->
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                result
            } catch (e: Exception) {
                connection.rollback()
                throw e
            }
        }
}
