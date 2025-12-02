package com.manonsunderground.model

/**
 * Message of the Day response
 */
data class MotdResponse(
    val html: String,
    val servers: Int?,
    val players: Int?
)
