package io.heapy.komok.business.login

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class LoginResponse(
    val sessionToken: String,
    val maxAge: Duration,
)
