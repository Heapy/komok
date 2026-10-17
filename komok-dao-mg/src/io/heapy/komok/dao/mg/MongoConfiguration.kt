package io.heapy.komok.dao.mg

import kotlinx.serialization.Serializable

@Serializable
data class MongoConfiguration(
    val username: String,
    val password: String,
    val host: String,
    val port: Int,
)
