package com.laxotters.clipy.data.session.mapper

import com.laxotters.clipy.data.storage.room.entity.SessionViewStateEntity
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.domain.model.SessionViewState
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionViewStateMapperTest {
    @Test
    fun sessionViewState_toEntity_convertsStorageTypes() {
        val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val sessionViewState = SessionViewState(
            sessionId = sessionId,
            lastWebUrl = "https://example.com/session",
            bottomSheetState = BottomSheetState.EXPANDED,
            lastOpenedAt = Instant.ofEpochMilli(1_000L),
        )

        val entity = sessionViewState.toEntity()

        assertEquals("00000000-0000-0000-0000-000000000001", entity.sessionId)
        assertEquals("EXPANDED", entity.bottomSheetState)
        assertEquals(1_000L, entity.lastOpenedAtMillis)
    }

    @Test
    fun nullLastWebUrl_toEntity_keepsNullLastWebUrl() {
        val sessionViewState = SessionViewState(
            sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            lastWebUrl = null,
            bottomSheetState = BottomSheetState.HIDDEN,
            lastOpenedAt = Instant.ofEpochMilli(1_000L),
        )

        val entity = sessionViewState.toEntity()

        assertEquals(null, entity.lastWebUrl)
    }

    @Test
    fun sessionViewStateEntity_toDomain_restoresDomainTypes() {
        val entity = SessionViewStateEntity(
            sessionId = "00000000-0000-0000-0000-000000000001",
            lastWebUrl = "https://example.com/session",
            bottomSheetState = "PEEK",
            lastOpenedAtMillis = 1_000L,
        )

        val sessionViewState = entity.toDomain()

        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), sessionViewState.sessionId)
        assertEquals(BottomSheetState.PEEK, sessionViewState.bottomSheetState)
        assertEquals(Instant.ofEpochMilli(1_000L), sessionViewState.lastOpenedAt)
    }

    @Test
    fun nullLastWebUrl_toDomain_keepsNullLastWebUrl() {
        val entity = SessionViewStateEntity(
            sessionId = "00000000-0000-0000-0000-000000000001",
            lastWebUrl = null,
            bottomSheetState = "HIDDEN",
            lastOpenedAtMillis = 1_000L,
        )

        val sessionViewState = entity.toDomain()

        assertEquals(null, sessionViewState.lastWebUrl)
    }

    @Test
    fun unknownBottomSheetState_toDomain_fallsBackToHidden() {
        val entity = SessionViewStateEntity(
            sessionId = "00000000-0000-0000-0000-000000000001",
            lastWebUrl = null,
            bottomSheetState = "UNKNOWN",
            lastOpenedAtMillis = 1_000L,
        )

        val sessionViewState = entity.toDomain()

        assertEquals(BottomSheetState.HIDDEN, sessionViewState.bottomSheetState)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidSessionId_toDomain_throwsIllegalArgumentException() {
        val entity = SessionViewStateEntity(
            sessionId = "invalid-session-id",
            lastWebUrl = null,
            bottomSheetState = "HIDDEN",
            lastOpenedAtMillis = 1_000L,
        )

        entity.toDomain()
    }
}
