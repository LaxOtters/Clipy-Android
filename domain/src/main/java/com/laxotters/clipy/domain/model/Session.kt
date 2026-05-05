package com.laxotters.clipy.domain.model

import java.time.Instant
import java.util.UUID

enum class SessionStatus {
    DRAFT,
    COLLECTING,
    PENDING,
    DECIDED,
    ABANDONED,
}

data class Session(
    val id: UUID,
    val name: String?,
    val status: SessionStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val closedAt: Instant?,
    val abandonedAt: Instant?,
    val items: List<Item>,
    val decisions: List<Decision>,
)
