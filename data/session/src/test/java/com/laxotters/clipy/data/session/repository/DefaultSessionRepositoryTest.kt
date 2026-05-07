package com.laxotters.clipy.data.session.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.laxotters.clipy.data.storage.room.ClipyDatabase
import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.domain.model.Capture
import com.laxotters.clipy.domain.model.Decision
import com.laxotters.clipy.domain.model.ImageRef
import com.laxotters.clipy.domain.model.Item
import com.laxotters.clipy.domain.model.ItemIntentState
import com.laxotters.clipy.domain.model.MoneySnapshot
import com.laxotters.clipy.domain.model.Session
import com.laxotters.clipy.domain.model.SessionSnapshot
import com.laxotters.clipy.domain.model.SessionStatus
import com.laxotters.clipy.domain.model.SessionViewState
import com.laxotters.clipy.domain.repository.SessionRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultSessionRepositoryTest {
    private lateinit var database: ClipyDatabase
    private lateinit var sessionRepository: SessionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room
            .inMemoryDatabaseBuilder(context, ClipyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionRepository = DefaultSessionRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sessionSnapshot_saveSessionSnapshot_restoresSessionSnapshot() = runBlocking {
        val sessionSnapshot = sessionSnapshot()

        sessionRepository.saveSessionSnapshot(sessionSnapshot)

        assertEquals(sessionSnapshot, sessionRepository.getSessionSnapshot(sessionSnapshot.session.id))
    }

    @Test
    fun emptySessionSnapshot_saveSessionSnapshot_restoresEmptySessionSnapshot() = runBlocking {
        val sessionSnapshot = sessionSnapshot(
            session = session(
                status = SessionStatus.DRAFT,
                items = emptyList(),
                decisions = emptyList(),
            ),
        )

        sessionRepository.saveSessionSnapshot(sessionSnapshot)

        assertEquals(sessionSnapshot, sessionRepository.getSessionSnapshot(sessionSnapshot.session.id))
    }

    @Test
    fun unknownSessionId_getSessionSnapshot_returnsNull() = runBlocking {
        val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000099")

        val sessionSnapshot = sessionRepository.getSessionSnapshot(sessionId)

        assertNull(sessionSnapshot)
    }

    @Test
    fun savedSessionSnapshot_deleteSession_removesSavedSessionSnapshot() = runBlocking {
        val sessionSnapshot = sessionSnapshot()
        sessionRepository.saveSessionSnapshot(sessionSnapshot)

        sessionRepository.deleteSession(sessionSnapshot.session.id)

        assertNull(sessionRepository.getSessionSnapshot(sessionSnapshot.session.id))
    }

    @Test
    fun savedSessionSnapshot_deleteItem_removesItemFromRestoredSessionSnapshot() = runBlocking {
        val sessionSnapshot = sessionSnapshot(
            session = session(
                items = listOf(
                    item(id = itemId1),
                    item(id = itemId2),
                ),
                decisions = listOf(decision(itemId = itemId1)),
            ),
        )
        sessionRepository.saveSessionSnapshot(sessionSnapshot)

        sessionRepository.deleteItem(itemId1)

        val restoredSnapshot = requireNotNull(sessionRepository.getSessionSnapshot(sessionSnapshot.session.id))
        val restoredSession = restoredSnapshot.session
        assertEquals(listOf(itemId2), restoredSession.items.map { it.id })
        assertEquals(emptyList<Decision>(), restoredSession.decisions)
        assertEquals(sessionSnapshot.viewState, restoredSnapshot.viewState)
    }

    private fun sessionSnapshot(
        session: Session = session(),
        viewState: SessionViewState = sessionViewState(),
    ) = SessionSnapshot(
        session = session,
        viewState = viewState,
    )

    private fun session(
        status: SessionStatus = SessionStatus.COLLECTING,
        items: List<Item> = listOf(item()),
        decisions: List<Decision> = listOf(decision()),
    ) = Session(
        id = sessionId,
        name = "Session 1",
        status = status,
        createdAt = Instant.ofEpochMilli(1_000L),
        updatedAt = Instant.ofEpochMilli(2_000L),
        closedAt = null,
        abandonedAt = null,
        items = items,
        decisions = decisions,
    )

    private fun sessionViewState() = SessionViewState(
        sessionId = sessionId,
        lastWebUrl = "https://example.com/session",
        bottomSheetState = BottomSheetState.EXPANDED,
        lastOpenedAt = Instant.ofEpochMilli(4_000L),
    )

    private fun item(
        id: UUID = itemId1,
    ) = Item(
        id = id,
        sessionId = sessionId,
        sourceUrl = "https://example.com/items/$id",
        productName = "Product $id",
        priceSnapshot = MoneySnapshot(
            amount = 10_000L,
            currency = "KRW",
            rawText = "10,000원",
        ),
        thumbnailImage = ImageRef(
            remoteUrl = "https://example.com/items/$id.jpg",
            localPath = "clipy/images/$id.jpg",
        ),
        note = "note",
        intentState = ItemIntentState.INTERESTED,
        captures = listOf(
            capture(
                id = if (id == itemId1) captureId1 else captureId2,
                itemId = id,
            ),
        ),
        createdAt = Instant.ofEpochMilli(1_000L),
        updatedAt = Instant.ofEpochMilli(2_000L),
    )

    private fun capture(
        id: UUID = captureId1,
        itemId: UUID = itemId1,
    ) = Capture(
        id = id,
        itemId = itemId,
        imageRef = ImageRef(
            remoteUrl = "https://example.com/captures/$itemId.jpg",
            localPath = "clipy/images/captures/$itemId.jpg",
        ),
        capturedAt = Instant.ofEpochMilli(1_500L),
        memo = "memo",
    )

    private fun decision(
        itemId: UUID = itemId1,
    ) = Decision(
        id = decisionId1,
        sessionId = sessionId,
        itemId = itemId,
        decidedAt = Instant.ofEpochMilli(3_000L),
    )

    private companion object {
        val sessionId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val itemId1: UUID = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val itemId2: UUID = UUID.fromString("00000000-0000-0000-0000-000000000012")
        val captureId1: UUID = UUID.fromString("00000000-0000-0000-0000-000000000021")
        val captureId2: UUID = UUID.fromString("00000000-0000-0000-0000-000000000022")
        val decisionId1: UUID = UUID.fromString("00000000-0000-0000-0000-000000000031")
    }
}
