package com.laxotters.clipy.data.session.repository

import androidx.room.withTransaction
import com.laxotters.clipy.data.session.mapper.toDomain
import com.laxotters.clipy.data.session.mapper.toEntity
import com.laxotters.clipy.data.storage.room.ClipyDatabase
import com.laxotters.clipy.domain.model.SessionSnapshot
import com.laxotters.clipy.domain.repository.SessionRepository
import java.util.UUID
import javax.inject.Inject

class DefaultSessionRepository @Inject constructor(
    private val database: ClipyDatabase,
) : SessionRepository {
    private val sessionDao = database.sessionDao()
    private val sessionViewStateDao = database.sessionViewStateDao()

    override suspend fun saveSessionSnapshot(sessionSnapshot: SessionSnapshot) {
        database.withTransaction {
            val session = sessionSnapshot.session
            val items = session.items.map { it.toEntity() }
            val captures = session.items.flatMap { item -> item.captures.map { it.toEntity() } }
            val decisions = session.decisions.map { it.toEntity() }

            sessionDao.insertSession(session.toEntity())
            if (items.isNotEmpty()) sessionDao.insertItems(items)
            if (captures.isNotEmpty()) sessionDao.insertCaptures(captures)
            if (decisions.isNotEmpty()) sessionDao.insertDecisions(decisions)
            sessionViewStateDao.saveSessionViewState(sessionSnapshot.viewState.toEntity())
        }
    }

    override suspend fun getSessionSnapshot(sessionId: UUID): SessionSnapshot? =
        database.withTransaction {
            val session = sessionDao.getSessionWithDetails(sessionId.toString())?.toDomain()
            val viewState = sessionViewStateDao.getSessionViewState(sessionId.toString())?.toDomain()

            if (session == null || viewState == null) {
                null
            } else {
                SessionSnapshot(
                    session = session,
                    viewState = viewState,
                )
            }
        }

    override suspend fun deleteSession(sessionId: UUID) {
        sessionDao.deleteSession(sessionId.toString())
    }

    override suspend fun deleteItem(itemId: UUID) {
        sessionDao.deleteItem(itemId.toString())
    }
}
