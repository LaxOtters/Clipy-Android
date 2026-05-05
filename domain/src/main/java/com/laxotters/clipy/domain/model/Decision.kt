package com.laxotters.clipy.domain.model

import java.time.Instant
import java.util.UUID

data class Decision(
    val id: UUID,
    val sessionId: UUID,
    val itemId: UUID,
    val decidedAt: Instant,
)
