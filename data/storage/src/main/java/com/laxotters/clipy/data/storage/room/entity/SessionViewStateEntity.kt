package com.laxotters.clipy.data.storage.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_view_states",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SessionViewStateEntity(
    @PrimaryKey val sessionId: String,
    val lastWebUrl: String?,
    val bottomSheetState: String,
    val lastOpenedAtMillis: Long,
)
