package com.laxotters.clipy.domain.repository

import com.laxotters.clipy.domain.model.SessionSnapshot
import java.util.UUID

interface SessionRepository {
    suspend fun saveSessionSnapshot(sessionSnapshot: SessionSnapshot)

    suspend fun getSessionSnapshot(sessionId: UUID): SessionSnapshot?

    suspend fun deleteSession(sessionId: UUID)

    suspend fun deleteItem(itemId: UUID)
}
