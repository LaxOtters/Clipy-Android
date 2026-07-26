package com.laxotters.clipy.domain.usecase

import com.laxotters.clipy.domain.model.BottomSheetState
import com.laxotters.clipy.domain.model.Session
import com.laxotters.clipy.domain.model.SessionSnapshot
import com.laxotters.clipy.domain.model.SessionStatus
import com.laxotters.clipy.domain.model.SessionViewState
import com.laxotters.clipy.domain.repository.SessionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): UUID {
        val sessionId = UUID.randomUUID()
        val createdAt = Instant.now()
        val sessionSnapshot = createInitialSessionSnapshot(
            sessionId = sessionId,
            createdAt = createdAt,
        )

        sessionRepository.saveSessionSnapshot(sessionSnapshot)

        return sessionId
    }

    private fun createInitialSessionSnapshot(
        sessionId: UUID,
        createdAt: Instant,
    ) = SessionSnapshot(
        session = Session(
            id = sessionId,
            name = null,
            status = SessionStatus.DRAFT,
            createdAt = createdAt,
            updatedAt = createdAt,
            closedAt = null,
            abandonedAt = null,
            items = emptyList(),
            decisions = emptyList(),
        ),
        viewState = SessionViewState(
            sessionId = sessionId,
            lastWebUrl = null,
            bottomSheetState = BottomSheetState.PEEK,
            lastOpenedAt = createdAt,
        ),
    )
}
