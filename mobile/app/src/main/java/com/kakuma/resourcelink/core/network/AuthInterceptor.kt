package com.kakuma.resourcelink.core.network

import com.kakuma.resourcelink.core.security.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Attaches the access token. Requests marked with [NO_AUTH_HEADER] (login/register) go out anonymously. */
class AuthInterceptor @Inject constructor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(NO_AUTH_HEADER) != null) {
            return chain.proceed(original.newBuilder().removeHeader(NO_AUTH_HEADER).build())
        }
        val token = tokenStore.tokens()?.access ?: return chain.proceed(original)
        return chain.proceed(original.newBuilder().header("Authorization", "Bearer $token").build())
    }

    companion object {
        const val NO_AUTH_HEADER = "X-No-Auth"
        /** Use as `@Headers(AuthInterceptor.NO_AUTH)` on public auth endpoints. */
        const val NO_AUTH = "$NO_AUTH_HEADER: true"
    }
}
