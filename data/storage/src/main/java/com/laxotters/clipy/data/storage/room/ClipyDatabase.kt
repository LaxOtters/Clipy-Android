package com.laxotters.clipy.data.storage.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.laxotters.clipy.data.storage.room.dao.SessionDao
import com.laxotters.clipy.data.storage.room.entity.CaptureEntity
import com.laxotters.clipy.data.storage.room.entity.DecisionEntity
import com.laxotters.clipy.data.storage.room.entity.ItemEntity
import com.laxotters.clipy.data.storage.room.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        ItemEntity::class,
        CaptureEntity::class,
        DecisionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ClipyDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
