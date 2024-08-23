package io.heapy.komok.business.login

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class JwtConfiguration(
    val audience: String,
    val realm: String,
    val issuer: String,
    val secret: String,
    val expiration: Duration,
)
