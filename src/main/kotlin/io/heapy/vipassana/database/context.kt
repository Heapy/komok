package io.heapy.vipassana.database

import com.zaxxer.hikari.HikariDataSource
import java.sql.*

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

class HikariConnectionProvider(
    private val hikariDataSource: HikariDataSource,
) : ConnectionProvider {
    override val connection: Connection
        get() = hikariDataSource.connection
}

fun ConnectionProvider.transactionContext(): TransactionContext = HikariTransactionContext(this)

fun ConnectionProvider.databaseContext(): DatabaseContext = HikariDatabaseContext(this)

internal class HikariTransactionContext(
    private val connectionProvider: ConnectionProvider,
) : TransactionContext, AutoCloseable {
    private var _connection: Connection? = null

    override fun startTransaction() =
        connectionProvider.connection.let { retrievedConnection ->
            _connection = retrievedConnection
            retrievedConnection.autoCommit = false
        }

    override fun commitTransaction() =
        connection.commit()

    override fun rollbackTransaction() =
        connection.rollback()

    override fun <T> execute(block: (Connection) -> T): T =
        block(connection)

    override fun <T> executeInTransaction(block: (Connection) -> T): T =
        execute(block)

    override fun close() =
        connection
            .let { connectionHeld ->
                _connection = null
                connectionHeld.close()
            }

    private inline val connection: Connection
        get() = _connection ?: error("No active transaction")
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
