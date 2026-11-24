package com.kakuma.resourcelink.core.network

import com.squareup.moshi.JsonClass
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class RefreshRequest(val refresh: String)

@JsonClass(generateAdapter = true)
data class TokenPairDto(val access: String, val refresh: String)

/** Synchronous API used only by [TokenAuthenticator] on OkHttp's thread. */
interface TokenRefreshApi {
    @POST("auth/token/refresh/")
    fun refresh(@Body body: RefreshRequest): Call<TokenPairDto>
}
