package io.heapy.komok.infra.http.server.errors

import kotlinx.serialization.Serializable

@Serializable
sealed interface BadRequestResponse

@Serializable
data class GenericBadRequestResponse(
    val message: String,
) : BadRequestResponse

@Serializable
data class FieldBadRequestResponse(
    val fields: List<Field>,
) : BadRequestResponse {
    @Serializable
    data class Field(
        val jsonPath: String,
        val message: String,
    )
}
