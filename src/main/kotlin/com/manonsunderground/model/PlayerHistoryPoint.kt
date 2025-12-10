package com.manonsunderground.model

import java.time.Instant

/**
 * Represents a data point for player history charts
 */
data class PlayerHistoryPoint(
    val timestamp: Instant,
    val playerCount: Int,
    val maxPlayers: Int
)
