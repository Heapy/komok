package io.heapy.komok.dao.pg

import kotlinx.serialization.Serializable

@Serializable
data class PostgresConfiguration(
    val user: String,
    val password: String,
    val database: String,
    val host: String,
    val port: Int,
)
