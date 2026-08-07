package com.laxotters.clipy.data.session.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
import org.junit.Assert.assertThrows
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
    fun viewStateSaveFails_saveSessionSnapshot_rollsBackSessionInsert() {
        val sessionSnapshot = sessionSnapshot(
            session = session(
                status = SessionStatus.DRAFT,
                items = emptyList(),
                decisions = emptyList(),
            ),
            viewState = sessionViewState(viewStateSessionId = differentSessionId),
        )

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { sessionRepository.saveSessionSnapshot(sessionSnapshot) }
        }

        val savedSession = runBlocking {
            database.sessionDao().getSessionWithDetails(sessionSnapshot.session.id.toString())
        }
        assertNull(savedSession)
    }

    @Test
    fun duplicateSessionId_saveSessionSnapshot_keepsOriginalSnapshot() {
        val originalSnapshot = sessionSnapshot()
        val duplicateSnapshot = sessionSnapshot(
            session = session(status = SessionStatus.ABANDONED),
        )
        runBlocking { sessionRepository.saveSessionSnapshot(originalSnapshot) }

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { sessionRepository.saveSessionSnapshot(duplicateSnapshot) }
        }

        val savedSnapshot = runBlocking {
            sessionRepository.getSessionSnapshot(originalSnapshot.session.id)
        }
        assertEquals(originalSnapshot, savedSnapshot)
    }

    @Test
    fun unknownSessionId_getSessionSnapshot_returnsNull() = runBlocking {
        val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000099")

        val sessionSnapshot = sessionRepository.getSessionSnapshot(sessionId)

        assertNull(sessionSnapshot)
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

    private fun sessionViewState(
        viewStateSessionId: UUID = sessionId,
    ) = SessionViewState(
        sessionId = viewStateSessionId,
        lastWebUrl = "https://example.com/session",
        bottomSheetState = BottomSheetState.EXPANDED,
        lastOpenedAt = Instant.ofEpochMilli(4_000L),
    )

    private fun item() = Item(
        id = itemId1,
        sessionId = sessionId,
        sourceUrl = "https://example.com/items/$itemId1",
        productName = "Product $itemId1",
        priceSnapshot = MoneySnapshot(
            amount = 10_000L,
            currency = "KRW",
            rawText = "10,000원",
        ),
        thumbnailImage = ImageRef(
            remoteUrl = "https://example.com/items/$itemId1.jpg",
            localPath = "clipy/images/$itemId1.jpg",
        ),
        note = "note",
        intentState = ItemIntentState.INTERESTED,
        captures = listOf(capture()),
        createdAt = Instant.ofEpochMilli(1_000L),
        updatedAt = Instant.ofEpochMilli(2_000L),
    )

    private fun capture() = Capture(
        id = captureId1,
        itemId = itemId1,
        imageRef = ImageRef(
            remoteUrl = "https://example.com/captures/$itemId1.jpg",
            localPath = "clipy/images/captures/$itemId1.jpg",
        ),
        capturedAt = Instant.ofEpochMilli(1_500L),
        memo = "memo",
    )

    private fun decision() = Decision(
        id = decisionId1,
        sessionId = sessionId,
        itemId = itemId1,
        decidedAt = Instant.ofEpochMilli(3_000L),
    )

    private companion object {
        val sessionId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val differentSessionId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val itemId1: UUID = UUID.fromString("00000000-0000-0000-0000-000000000011")
        val captureId1: UUID = UUID.fromString("00000000-0000-0000-0000-000000000021")
        val decisionId1: UUID = UUID.fromString("00000000-0000-0000-0000-000000000031")
    }
}
