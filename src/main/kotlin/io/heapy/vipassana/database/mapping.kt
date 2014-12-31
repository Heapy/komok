package io.heapy.vipassana.database

import kotlinx.serialization.DeserializationStrategy
import java.sql.ResultSet

object PostgresJdbc

fun <T> PostgresJdbc.toObject(
    deserializer: DeserializationStrategy<T>,
    resultSet: ResultSet
): T {
    TODO()
}
