package com.laxotters.clipy.data.session.mapper

import com.laxotters.clipy.data.storage.room.entity.CaptureEntity
import com.laxotters.clipy.data.storage.room.entity.DecisionEntity
import com.laxotters.clipy.data.storage.room.entity.ItemEntity
import com.laxotters.clipy.data.storage.room.entity.SessionEntity
import com.laxotters.clipy.data.storage.room.relation.ItemWithCaptures
import com.laxotters.clipy.data.storage.room.relation.SessionWithItemsAndDecisions
import com.laxotters.clipy.domain.model.ItemIntentState
import com.laxotters.clipy.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionMapperTest {
    @Test
    fun unknownSessionStatus_toDomain_fallsBackToDraft() {
        val sessionWithDetails = sessionWithDetails(
            session = sessionEntity(status = "UNKNOWN"),
        )

        val session = sessionWithDetails.toDomain()

        assertEquals(SessionStatus.DRAFT, session.status)
    }

    @Test
    fun unknownItemIntentState_toDomain_fallsBackToInterested() {
        val sessionWithDetails = sessionWithDetails(
            items = listOf(
                ItemWithCaptures(
                    item = itemEntity(intentState = "UNKNOWN"),
                    captures = emptyList(),
                ),
            ),
        )

        val session = sessionWithDetails.toDomain()

        assertEquals(ItemIntentState.INTERESTED, session.items.single().intentState)
    }

    @Test
    fun localOnlyThumbnailImage_toDomain_keepsImageRef() {
        val sessionWithDetails = sessionWithDetails(
            items = listOf(
                ItemWithCaptures(
                    item = itemEntity(
                        thumbnailRemoteUrl = null,
                        thumbnailLocalPath = "clipy/images/item-1.jpg",
                    ),
                    captures = emptyList(),
                ),
            ),
        )

        val thumbnailImage = sessionWithDetails.toDomain().items.single().thumbnailImage

        assertEquals(null, thumbnailImage?.remoteUrl)
        assertEquals("clipy/images/item-1.jpg", thumbnailImage?.localPath)
    }

    @Test
    fun localOnlyCaptureImage_toDomain_keepsImageRef() {
        val sessionWithDetails = sessionWithDetails(
            items = listOf(
                ItemWithCaptures(
                    item = itemEntity(),
                    captures = listOf(
                        captureEntity(
                            imageRemoteUrl = null,
                            imageLocalPath = "clipy/images/capture-1.jpg",
                        ),
                    ),
                ),
            ),
        )

        val imageRef = sessionWithDetails.toDomain().items.single().captures.single().imageRef

        assertEquals(null, imageRef.remoteUrl)
        assertEquals("clipy/images/capture-1.jpg", imageRef.localPath)
    }

    private fun sessionWithDetails(
        session: SessionEntity = sessionEntity(),
        items: List<ItemWithCaptures> = emptyList(),
        decisions: List<DecisionEntity> = emptyList(),
    ) = SessionWithItemsAndDecisions(
        session = session,
        items = items,
        decisions = decisions,
    )

    private fun sessionEntity(
        status: String = "COLLECTING",
    ) = SessionEntity(
        id = "00000000-0000-0000-0000-000000000001",
        name = "Session 1",
        status = status,
        createdAtMillis = 1_000L,
        updatedAtMillis = 2_000L,
        closedAtMillis = null,
        abandonedAtMillis = null,
    )

    private fun itemEntity(
        intentState: String = "INTERESTED",
        thumbnailRemoteUrl: String? = "https://example.com/item-1.jpg",
        thumbnailLocalPath: String? = "clipy/images/item-1.jpg",
    ) = ItemEntity(
        id = "00000000-0000-0000-0000-000000000011",
        sessionId = "00000000-0000-0000-0000-000000000001",
        sourceUrl = "https://example.com/item-1",
        productName = "Product 1",
        priceAmount = 10_000L,
        priceCurrency = "KRW",
        priceRawText = "10,000원",
        thumbnailRemoteUrl = thumbnailRemoteUrl,
        thumbnailLocalPath = thumbnailLocalPath,
        note = null,
        intentState = intentState,
        createdAtMillis = 1_000L,
        updatedAtMillis = 2_000L,
    )

    private fun captureEntity(
        imageRemoteUrl: String? = "https://example.com/capture-1.jpg",
        imageLocalPath: String? = "clipy/images/capture-1.jpg",
    ) = CaptureEntity(
        id = "00000000-0000-0000-0000-000000000021",
        itemId = "00000000-0000-0000-0000-000000000011",
        imageRemoteUrl = imageRemoteUrl,
        imageLocalPath = imageLocalPath,
        capturedAtMillis = 3_000L,
        memo = null,
    )
}
