package io.heapy.komok.business.register.auto

import kotlinx.serialization.Serializable

@Serializable
data class AutoRegisterStartupTaskConfiguration(
    val email: String,
)
