package com.kakuma.resourcelink.core.network

import com.kakuma.resourcelink.core.security.AuthTokens
import com.kakuma.resourcelink.core.security.TokenStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider

/**
 * Refreshes the access token when the API answers 401.
 *
 * Race safety: refreshes are serialised with a lock. A request that waited for the lock first checks
 * whether another thread already refreshed (its token differs from the stored one) and simply retries
 * with the new token — so N parallel 401s trigger exactly one refresh. Refresh tokens rotate on the
 * backend, so a second refresh with the old token would fail and log the user out.
 *
 * Unrecoverable failures (refresh rejected) clear the session and notify [SessionEvents].
 * Network failures during refresh keep the session so the user is not logged out while offline.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val refreshApi: Provider<TokenRefreshApi>,
    private val sessionEvents: SessionEvents,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            ?: return null // anonymous request: nothing to refresh
        if (responseCount(response) >= MAX_ATTEMPTS) return null

        synchronized(lock) {
            val current = tokenStore.tokens() ?: return null
            if (current.access != failedToken) {
                return response.request.withToken(current.access) // someone else refreshed already
            }
            val refreshed = try {
                refreshApi.get().refresh(RefreshRequest(current.refresh)).execute()
            } catch (e: IOException) {
                return null // offline: keep the session, the call fails with a network error
            }
            val body = refreshed.body()
            if (refreshed.isSuccessful && body != null) {
                tokenStore.save(AuthTokens(body.access, body.refresh))
                return response.request.withToken(body.access)
            }
            if (refreshed.code() in 400..499) {
                tokenStore.clear()
                sessionEvents.notifyExpired()
            }
            return null
        }
    }

    private fun Request.withToken(token: String) = newBuilder().header("Authorization", "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }

    private companion object { const val MAX_ATTEMPTS = 2 }
}
