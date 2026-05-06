package com.laxotters.clipy.data.storage.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "captures",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["itemId"]),
    ],
)
data class CaptureEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val imageRemoteUrl: String,
    val imageLocalPath: String?,
    val capturedAtMillis: Long,
    val memo: String?,
)
