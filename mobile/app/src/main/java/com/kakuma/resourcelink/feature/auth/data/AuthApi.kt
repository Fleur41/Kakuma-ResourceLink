package com.kakuma.resourcelink.feature.auth.data

import com.kakuma.resourcelink.core.network.AuthInterceptor
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthApi {
    @Headers(AuthInterceptor.NO_AUTH)
    @POST("auth/register/")
    suspend fun register(@Body body: RegisterRequest): AuthResponseDto

    @Headers(AuthInterceptor.NO_AUTH)
    @POST("auth/login/")
    suspend fun login(@Body body: LoginRequest): AuthResponseDto

    @POST("auth/logout/")
    suspend fun logout(@Body body: LogoutRequest)

    @GET("auth/me/")
    suspend fun me(): UserDto

    @PATCH("auth/me/")
    suspend fun updateMe(@Body body: UpdateMeRequest): UserDto

    @Multipart
    @POST("auth/me/avatar/")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): UserDto

    @Headers(AuthInterceptor.NO_AUTH)
    @POST("auth/password-reset/")
    suspend fun requestPasswordReset(@Body body: EmailRequest)
}
