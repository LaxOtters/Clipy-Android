package com.laxotters.clipy.data.storage.room.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.laxotters.clipy.data.storage.room.entity.CaptureEntity
import com.laxotters.clipy.data.storage.room.entity.ItemEntity

data class ItemWithCaptures(
    @Embedded val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId",
    )
    val captures: List<CaptureEntity>,
)
