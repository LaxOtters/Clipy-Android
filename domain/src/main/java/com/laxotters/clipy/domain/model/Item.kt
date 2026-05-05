package com.laxotters.clipy.domain.model

import java.time.Instant
import java.util.UUID

enum class ItemIntentState {
    INTERESTED,
    HOLD,
    DROPPED,
}

data class Item(
    val id: UUID,
    val sessionId: UUID,
    val sourceUrl: String,
    val productName: String?,
    val priceSnapshot: MoneySnapshot?,
    val thumbnailImage: ImageRef?,
    val note: String?,
    val intentState: ItemIntentState,
    val captures: List<Capture>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
