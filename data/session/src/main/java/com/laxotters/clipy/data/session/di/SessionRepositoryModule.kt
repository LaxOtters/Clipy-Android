package com.laxotters.clipy.data.session.di

import com.laxotters.clipy.data.session.repository.DefaultSessionRepository
import com.laxotters.clipy.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        defaultSessionRepository: DefaultSessionRepository,
    ): SessionRepository
}
