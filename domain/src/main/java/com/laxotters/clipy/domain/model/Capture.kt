package com.laxotters.clipy.domain.model

import java.time.Instant
import java.util.UUID

data class Capture(
    val id: UUID,
    val itemId: UUID,
    val imageRef: ImageRef,
    val capturedAt: Instant,
    val memo: String?,
)
