package io.heapy.komok.infra.http.server.errors

class BadRequestException(
    val response: BadRequestResponse,
) : RuntimeException()

@Suppress("NOTHING_TO_INLINE")
inline fun badRequestError(
    message: String,
): Nothing {
    throw BadRequestException(
        response = GenericBadRequestResponse(
            message = message,
        ),
    )
}

@Suppress("NOTHING_TO_INLINE")
inline fun badRequestError(
    jsonPath: String,
    message: String,
): Nothing {
    throw BadRequestException(
        response = FieldBadRequestResponse(
            fields = listOf(
                FieldBadRequestResponse.Field(
                    jsonPath = jsonPath,
                    message = message,
                ),
            ),
        ),
    )
}

@Suppress("NOTHING_TO_INLINE")
inline fun badRequestError(
    fields: List<FieldBadRequestResponse.Field>,
): Nothing {
    throw BadRequestException(
        response = FieldBadRequestResponse(
            fields = fields,
        ),
    )
}
