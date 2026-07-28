package com.laxotters.clipy.domain.usecase

import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.domain.model.SessionSnapshot
import com.laxotters.clipy.domain.model.SessionStatus
import com.laxotters.clipy.domain.repository.SessionRepository
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateSessionUseCaseTest {
    @Test
    fun newSession_invoke_savesInitialSnapshotAndReturnsSessionId() = runBlocking {
        val sessionRepository = FakeSessionRepository()
        val createSession = CreateSessionUseCase(sessionRepository)

        val sessionId = createSession()

        val savedSnapshot = requireNotNull(sessionRepository.savedSnapshot)
        val session = savedSnapshot.session
        val viewState = savedSnapshot.viewState

        assertEquals(1, sessionRepository.saveCallCount)
        assertEquals(sessionId, session.id)
        assertEquals(sessionId, viewState.sessionId)
        assertNull(session.name)
        assertEquals(SessionStatus.DRAFT, session.status)
        assertEquals(session.createdAt, session.updatedAt)
        assertNull(session.closedAt)
        assertNull(session.abandonedAt)
        assertTrue(session.items.isEmpty())
        assertTrue(session.decisions.isEmpty())
        assertNull(viewState.lastWebUrl)
        assertEquals(BottomSheetState.PEEK, viewState.bottomSheetState)
        assertEquals(session.createdAt, viewState.lastOpenedAt)
    }

    @Test
    fun snapshotSaveFails_invoke_propagatesFailure() {
        val saveFailure = IllegalStateException("Session save failed")
        val sessionRepository = FakeSessionRepository(saveFailure = saveFailure)
        val createSession = CreateSessionUseCase(sessionRepository)

        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking { createSession() }
        }

        assertSame(saveFailure, thrown)
        assertEquals(1, sessionRepository.saveCallCount)
        assertNull(sessionRepository.savedSnapshot)
    }

    private class FakeSessionRepository(
        private val saveFailure: Throwable? = null,
    ) : SessionRepository {
        var saveCallCount: Int = 0
            private set

        var savedSnapshot: SessionSnapshot? = null
            private set

        override suspend fun saveSessionSnapshot(
            sessionSnapshot: SessionSnapshot,
        ) {
            saveCallCount += 1
            saveFailure?.let { throw it }
            savedSnapshot = sessionSnapshot
        }

        override suspend fun getSessionSnapshot(sessionId: UUID): SessionSnapshot? =
            error("Not used in this test")

        override suspend fun deleteSession(sessionId: UUID) {
            error("Not used in this test")
        }

        override suspend fun deleteItem(itemId: UUID) {
            error("Not used in this test")
        }
    }
}
