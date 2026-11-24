package com.kakuma.resourcelink.feature.opportunities.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.database.entity.PendingOperationType
import com.kakuma.resourcelink.feature.opportunities.domain.SyncOutcome
import com.kakuma.resourcelink.sync.SyncScheduler
import com.kakuma.resourcelink.testutil.TestData
import com.squareup.moshi.Moshi
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Real Room + Retrofit against a local MockWebServer: verifies optimistic bookmarks and offline queueing. */
@RunWith(AndroidJUnit4::class)
class OpportunityRepositoryImplTest {
    private val server = MockWebServer()
    private lateinit var db: AppDatabase
    private val scheduler = mockk<SyncScheduler>(relaxed = true)
    private lateinit var repository: OpportunityRepositoryImpl

    @Before fun setUp() {
        server.start()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val moshi = Moshi.Builder().build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .client(OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpportunityApi::class.java)
        repository = OpportunityRepositoryImpl(api, db, moshi, scheduler)
    }

    @After fun tearDown() {
        db.close()
        server.shutdown()
    }

    private suspend fun cachedBookmark(id: String) = db.opportunityDao().observe(id).first()!!.isBookmarked

    @Test fun `successful bookmark is kept and posted to the server`() = runTest {
        db.opportunityDao().upsertAll(listOf(TestData.opportunityEntity("o1")))
        server.enqueue(MockResponse().setResponseCode(201))

        val outcome = repository.setBookmarked("o1", true)

        assertEquals(SyncOutcome.Synced, outcome)
        assertTrue(cachedBookmark("o1"))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/opportunities/o1/bookmark/", request.path)
        assertTrue(db.pendingOperationDao().all().isEmpty())
    }

    @Test fun `offline bookmark stays applied and is queued for sync`() = runTest {
        db.opportunityDao().upsertAll(listOf(TestData.opportunityEntity("o1")))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val outcome = repository.setBookmarked("o1", true)

        assertEquals(SyncOutcome.QueuedOffline, outcome)
        assertTrue(cachedBookmark("o1"))
        assertEquals(PendingOperationType.BOOKMARK_OPPORTUNITY, db.pendingOperationDao().all().single().type)
        verify { scheduler.schedulePendingOperations() }
    }

    @Test fun `rejected unbookmark is reverted and not queued`() = runTest {
        db.opportunityDao().upsertAll(listOf(TestData.opportunityEntity("o1", bookmarked = true)))
        server.enqueue(MockResponse().setResponseCode(404))

        val outcome = repository.setBookmarked("o1", false)

        assertEquals(SyncOutcome.Failed(AppError.NotFound), outcome)
        assertTrue(cachedBookmark("o1"))
        assertEquals("DELETE", server.takeRequest().method)
        assertTrue(db.pendingOperationDao().all().isEmpty())
        verify(exactly = 0) { scheduler.schedulePendingOperations() }
    }

    @Test fun `toggling twice while offline leaves only the latest intent`() = runTest {
        db.opportunityDao().upsertAll(listOf(TestData.opportunityEntity("o1")))
        repeat(2) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }

        repository.setBookmarked("o1", true)
        repository.setBookmarked("o1", false)

        assertFalse(cachedBookmark("o1"))
        assertEquals(listOf(PendingOperationType.UNBOOKMARK_OPPORTUNITY), db.pendingOperationDao().all().map { it.type })
    }
}
