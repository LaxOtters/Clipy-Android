package com.laxotters.clipy.data.storage.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
    ],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sourceUrl: String,
    val productName: String?,
    val priceAmount: Long?,
    val priceCurrency: String?,
    val priceRawText: String?,
    val thumbnailRemoteUrl: String?,
    val thumbnailLocalPath: String?,
    val note: String?,
    val intentState: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
