package com.laxotters.clipy.data.storage.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val closedAtMillis: Long?,
    val abandonedAtMillis: Long?,
)
