package com.kakuma.resourcelink.core.network

import app.cash.turbine.test
import com.kakuma.resourcelink.core.security.AuthTokens
import com.kakuma.resourcelink.core.security.TokenStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Token refresh on 401 and session expiry, against a local MockWebServer. */
class TokenAuthenticatorTest {
    private val server = MockWebServer()
    private val store = FakeTokenStore(AuthTokens("old-access", "old-refresh"))
    private val sessionEvents = SessionEvents()
    private lateinit var client: OkHttpClient

    @Before fun setUp() {
        server.start()
        val refreshApi = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(TokenRefreshApi::class.java)
        client = OkHttpClient.Builder().authenticator(TokenAuthenticator(store, { refreshApi }, sessionEvents)).build()
    }

    @After fun tearDown() = server.shutdown()

    private fun call(token: String? = "old-access"): Int {
        val request = Request.Builder().url(server.url("/api/v1/auth/me/"))
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        return client.newCall(request).execute().use { it.code }
    }

    @Test fun `401 refreshes once and retries with the new token`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"access":"new-access","refresh":"new-refresh"}"""))
        server.enqueue(MockResponse().setResponseCode(200))

        assertEquals(200, call())

        server.takeRequest() // original
        val refresh = server.takeRequest()
        assertEquals("/api/v1/auth/token/refresh/", refresh.path)
        assertEquals("""{"refresh":"old-refresh"}""", refresh.body.readUtf8())
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals(AuthTokens("new-access", "new-refresh"), store.tokens())
    }

    @Test fun `rejected refresh clears the session and reports expiry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401)) // refresh token blacklisted / expired

        sessionEvents.sessionExpired.test {
            assertEquals(401, withContext(Dispatchers.IO) { call() })
            awaitItem()
        }
        assertNull(store.tokens())
    }

    @Test fun `token already refreshed by another request is reused without a second refresh`() {
        store.save(AuthTokens("fresh-access", "fresh-refresh"))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        assertEquals(200, call(token = "old-access"))

        server.takeRequest()
        assertEquals("Bearer fresh-access", server.takeRequest().getHeader("Authorization"))
        assertEquals(2, server.requestCount)
    }

    @Test fun `server error during refresh keeps the session`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(401, call())
        assertEquals(AuthTokens("old-access", "old-refresh"), store.tokens())
    }

    @Test fun `anonymous requests are never refreshed`() {
        server.enqueue(MockResponse().setResponseCode(401))

        assertEquals(401, call(token = null))
        assertEquals(1, server.requestCount)
    }

    private class FakeTokenStore(initial: AuthTokens?) : TokenStore {
        private val state = MutableStateFlow(initial)
        private val loggedIn = MutableStateFlow(initial != null)
        override val isLoggedIn: StateFlow<Boolean> = loggedIn
        override fun tokens() = state.value
        override fun save(tokens: AuthTokens) { state.value = tokens; loggedIn.value = true }
        override fun clear() { state.value = null; loggedIn.value = false }
    }
}
