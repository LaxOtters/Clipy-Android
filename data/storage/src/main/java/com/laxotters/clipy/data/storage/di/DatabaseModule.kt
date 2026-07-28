package com.laxotters.clipy.data.storage.di

import android.content.Context
import androidx.room.Room
import com.laxotters.clipy.data.storage.room.ClipyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideClipyDatabase(
        @ApplicationContext context: Context,
    ): ClipyDatabase = Room.databaseBuilder(
        context,
        ClipyDatabase::class.java,
        DATABASE_NAME,
    ).build()

    private const val DATABASE_NAME = "clipy.db"
}
