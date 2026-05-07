package com.laxotters.clipy.data.storage.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.laxotters.clipy.data.storage.room.entity.CaptureEntity
import com.laxotters.clipy.data.storage.room.entity.DecisionEntity
import com.laxotters.clipy.data.storage.room.entity.ItemEntity
import com.laxotters.clipy.data.storage.room.entity.SessionEntity
import com.laxotters.clipy.data.storage.room.relation.SessionWithItemsAndDecisions
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Insert
    suspend fun insertItems(items: List<ItemEntity>)

    @Insert
    suspend fun insertCaptures(captures: List<CaptureEntity>)

    @Insert
    suspend fun insertDecisions(decisions: List<DecisionEntity>)

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionWithDetails(sessionId: String): SessionWithItemsAndDecisions?

    @Query("SELECT * FROM sessions ORDER BY updatedAtMillis DESC")
    fun getSessions(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)
}
