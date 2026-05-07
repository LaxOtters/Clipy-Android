package com.laxotters.clipy.data.storage.room.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.laxotters.clipy.data.storage.room.entity.DecisionEntity
import com.laxotters.clipy.data.storage.room.entity.ItemEntity
import com.laxotters.clipy.data.storage.room.entity.SessionEntity

data class SessionWithItemsAndDecisions(
    @Embedded val session: SessionEntity,
    @Relation(
        entity = ItemEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val items: List<ItemWithCaptures>,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val decisions: List<DecisionEntity>,
)
