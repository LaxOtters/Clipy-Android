package com.laxotters.clipy.feature.home

import com.laxotters.clipy.domain.model.SessionSnapshot
import com.laxotters.clipy.domain.repository.SessionRepository
import com.laxotters.clipy.domain.usecase.CreateSessionUseCase
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startNewSessionClicked_createSucceeds_navigatesWithCreatedSessionId() = runTest {
        val sessionRepository = FakeSessionRepository()
        val viewModel = HomeViewModel(CreateSessionUseCase(sessionRepository))
        val effect = async { viewModel.effect.first() }

        viewModel.dispatch(HomeUiEvent.StartNewSessionClicked)
        advanceUntilIdle()

        val expectedSessionId = requireNotNull(sessionRepository.savedSnapshot).session.id.toString()
        assertEquals(
            HomeUiSideEffect.NavigateToSession(expectedSessionId),
            effect.await(),
        )
        assertFalse(viewModel.state.value.isStartingSession)
        assertEquals(1, sessionRepository.saveCallCount)
    }

    @Test
    fun startNewSessionClicked_creationInProgress_ignoresRepeatedInput() = runTest {
        val sessionRepository = BlockingSessionRepository()
        val viewModel = HomeViewModel(CreateSessionUseCase(sessionRepository))

        viewModel.dispatch(HomeUiEvent.StartNewSessionClicked)
        viewModel.dispatch(HomeUiEvent.StartNewSessionClicked)

        assertTrue(viewModel.state.value.isStartingSession)
        assertEquals(1, sessionRepository.saveCallCount)

        sessionRepository.completeSave()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isStartingSession)
        assertEquals(1, sessionRepository.saveCallCount)
    }

    @Test
    fun startNewSessionClicked_creationFails_restoresInputAndRetryCanSucceed() = runTest {
        val sessionRepository = FakeSessionRepository(
            saveFailure = IllegalStateException("Session save failed"),
        )
        val viewModel = HomeViewModel(CreateSessionUseCase(sessionRepository))

        viewModel.dispatch(HomeUiEvent.StartNewSessionClicked)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isStartingSession)
        assertEquals(1, sessionRepository.saveCallCount)

        sessionRepository.saveFailure = null
        val effect = async { viewModel.effect.first() }

        viewModel.dispatch(HomeUiEvent.StartNewSessionClicked)
        advanceUntilIdle()

        val expectedSessionId = requireNotNull(sessionRepository.savedSnapshot).session.id.toString()
        assertEquals(
            HomeUiSideEffect.NavigateToSession(expectedSessionId),
            effect.await(),
        )
        assertFalse(viewModel.state.value.isStartingSession)
        assertEquals(2, sessionRepository.saveCallCount)
    }

    private class FakeSessionRepository(
        var saveFailure: Throwable? = null,
    ) : SessionRepository {
        var saveCallCount: Int = 0
            private set

        var savedSnapshot: SessionSnapshot? = null
            private set

        override suspend fun saveSessionSnapshot(sessionSnapshot: SessionSnapshot) {
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

    private class BlockingSessionRepository : SessionRepository {
        private val saveCompletion = CompletableDeferred<Unit>()

        var saveCallCount: Int = 0
            private set

        override suspend fun saveSessionSnapshot(sessionSnapshot: SessionSnapshot) {
            saveCallCount += 1
            saveCompletion.await()
        }

        fun completeSave() {
            saveCompletion.complete(Unit)
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
