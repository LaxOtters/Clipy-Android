package com.laxotters.clipy.data.session.mapper

import com.laxotters.clipy.data.storage.room.entity.SessionViewStateEntity
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.domain.model.SessionViewState
import java.time.Instant
import java.util.UUID

internal fun SessionViewState.toEntity() = SessionViewStateEntity(
    sessionId = sessionId.toString(),
    lastWebUrl = lastWebUrl,
    bottomSheetState = bottomSheetState.name,
    lastOpenedAtMillis = lastOpenedAt.toEpochMilli(),
)

internal fun SessionViewStateEntity.toDomain() = SessionViewState(
    sessionId = UUID.fromString(sessionId),
    lastWebUrl = lastWebUrl,
    bottomSheetState = bottomSheetState.toBottomSheetState(),
    lastOpenedAt = Instant.ofEpochMilli(lastOpenedAtMillis),
)

private fun String.toBottomSheetState(): BottomSheetState =
    runCatching { BottomSheetState.valueOf(this) }
        .getOrDefault(BottomSheetState.HIDDEN)
