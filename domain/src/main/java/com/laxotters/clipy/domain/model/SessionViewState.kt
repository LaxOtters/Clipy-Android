package com.laxotters.clipy.domain.model

import java.time.Instant
import java.util.UUID

data class SessionViewState(
    val sessionId: UUID,
    val lastWebUrl: String?,
    val bottomSheetState: BottomSheetState,
    val lastOpenedAt: Instant,
)

enum class BottomSheetState {
    HIDDEN,
    PEEK,
    EXPANDED,
}
