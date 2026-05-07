package com.laxotters.clipy.data.storage.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.laxotters.clipy.data.storage.room.dao.SessionDao
import com.laxotters.clipy.data.storage.room.dao.SessionViewStateDao
import com.laxotters.clipy.data.storage.room.entity.CaptureEntity
import com.laxotters.clipy.data.storage.room.entity.DecisionEntity
import com.laxotters.clipy.data.storage.room.entity.ItemEntity
import com.laxotters.clipy.data.storage.room.entity.SessionEntity
import com.laxotters.clipy.data.storage.room.entity.SessionViewStateEntity

@Database(
    entities = [
        SessionEntity::class,
        ItemEntity::class,
        CaptureEntity::class,
        DecisionEntity::class,
        SessionViewStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ClipyDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun sessionViewStateDao(): SessionViewStateDao
}
