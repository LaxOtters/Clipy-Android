package com.laxotters.clipy.data.storage.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.laxotters.clipy.data.storage.room.entity.SessionViewStateEntity

@Dao
interface SessionViewStateDao {
    @Upsert
    suspend fun saveSessionViewState(sessionViewState: SessionViewStateEntity)

    @Query("SELECT * FROM session_view_states WHERE sessionId = :sessionId")
    suspend fun getSessionViewState(sessionId: String): SessionViewStateEntity?
}
