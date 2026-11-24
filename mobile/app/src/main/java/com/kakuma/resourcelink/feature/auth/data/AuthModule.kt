package com.kakuma.resourcelink.feature.auth.data

import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds @Singleton
    abstract fun repository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides @Singleton
        fun api(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    }
}
