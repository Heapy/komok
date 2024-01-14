@file:Suppress("FunctionName")

package io.heapy.komok.database

import kotlinx.serialization.Serializable

@Serializable
data class PostgresConfiguration(
    val user: String,
    val password: String,
    val database: String,
    val host: String,
    val port: Int,
)
