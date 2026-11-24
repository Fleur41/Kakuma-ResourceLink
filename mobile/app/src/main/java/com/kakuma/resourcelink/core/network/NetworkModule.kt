package com.kakuma.resourcelink.core.network

import com.kakuma.resourcelink.BuildConfig
import com.kakuma.resourcelink.core.di.RefreshClient
import com.kakuma.resourcelink.core.security.KeystoreTokenStore
import com.kakuma.resourcelink.core.security.TokenStore
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun moshi(): Moshi = Moshi.Builder().build()

    private fun baseClient(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS) // AI answers can take a while
        .writeTimeout(60, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.HTTP_LOGGING) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                })
            }
        }

    @Provides @Singleton @RefreshClient
    fun refreshClient(): OkHttpClient = baseClient().build()

    @Provides @Singleton
    fun okHttp(authInterceptor: AuthInterceptor, authenticator: TokenAuthenticator): OkHttpClient =
        baseClient().addInterceptor(authInterceptor).authenticator(authenticator).build()

    @Provides @Singleton
    fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides @Singleton
    fun tokenRefreshApi(@RefreshClient client: OkHttpClient, moshi: Moshi): TokenRefreshApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TokenRefreshApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    @Binds @Singleton
    abstract fun tokenStore(impl: KeystoreTokenStore): TokenStore
}
