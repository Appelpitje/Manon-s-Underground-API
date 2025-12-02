package com.manonsunderground.model

/**
 * API error response
 */
data class ApiError(
    val error: Int,
    val `in`: String,
    val internal: String? = null,
    val options: Map<String, Any>? = null,
    val ip: String? = null,
    val port: String? = null
)

/**
 * Exception thrown when API returns an error
 */
class NetworksApiException(
    val apiError: ApiError,
    message: String = "333networks API error: ${apiError.`in`}"
) : Exception(message)
