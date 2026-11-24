package com.kakuma.resourcelink.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.kakuma.resourcelink.core.database.entity.PendingOperationType.BOOKMARK_OPPORTUNITY
import com.kakuma.resourcelink.core.database.entity.PendingOperationType.LIKE_STORY
import com.kakuma.resourcelink.core.database.entity.PendingOperationType.UNBOOKMARK_OPPORTUNITY
import com.kakuma.resourcelink.testutil.TestData
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun tearDown() = db.close()

    @Test fun `cached list is ordered by position and list-specific`() = runTest {
        val dao = db.opportunityDao()
        dao.upsertAll(
            listOf(
                TestData.opportunityEntity("b", position = 1),
                TestData.opportunityEntity("a", position = 0),
                TestData.opportunityEntity("c", listKey = "scholarship", position = 0),
            ),
        )

        dao.observeList("all", limit = 10).test {
            assertEquals(listOf("a", "b"), awaitItem().map { it.id })
        }
        assertEquals(1, dao.maxPosition("all"))
        assertEquals(-1, dao.maxPosition("empty"))
    }

    @Test fun `type converter round-trips requirement lists`() = runTest {
        db.opportunityDao().upsertAll(listOf(TestData.opportunityEntity("a")))

        db.opportunityDao().observe("a").test {
            assertEquals(listOf("One", "Two"), awaitItem()!!.requirements)
        }
    }

    @Test fun `bookmark update reaches every cached copy and is observed as a flow`() = runTest {
        val dao = db.opportunityDao()
        dao.upsertAll(listOf(TestData.opportunityEntity("a", "all"), TestData.opportunityEntity("a", "featured")))

        dao.observeList("featured", 5).test {
            assertEquals(false, awaitItem().single().isBookmarked)
            dao.setBookmarked("a", true)
            assertEquals(true, awaitItem().single().isBookmarked)
        }
        dao.observeList("all", 5).test { assertTrue(awaitItem().single().isBookmarked) }
    }

    @Test fun `clearing one list keeps the others`() = runTest {
        val dao = db.opportunityDao()
        dao.upsertAll(listOf(TestData.opportunityEntity("a", "all"), TestData.opportunityEntity("a", "featured")))

        dao.clearList("all")

        dao.observeList("all", 5).test { assertTrue(awaitItem().isEmpty()) }
        dao.observeList("featured", 5).test { assertEquals(1, awaitItem().size) }
    }

    @Test fun `queued offline action cancels its opposite`() = runTest {
        val dao = db.pendingOperationDao()

        dao.enqueue(BOOKMARK_OPPORTUNITY, UNBOOKMARK_OPPORTUNITY, "o1", now = 1)
        dao.enqueue(UNBOOKMARK_OPPORTUNITY, BOOKMARK_OPPORTUNITY, "o1", now = 2)

        val ops = dao.all()
        assertEquals(listOf(UNBOOKMARK_OPPORTUNITY), ops.map { it.type })
    }

    @Test fun `repeating the same action keeps a single row and count is observable`() = runTest {
        val dao = db.pendingOperationDao()

        dao.observeCount().test {
            assertEquals(0, awaitItem())
            dao.enqueue(LIKE_STORY, LIKE_STORY, "s1", now = 1)
            assertEquals(1, awaitItem())
            dao.enqueue(BOOKMARK_OPPORTUNITY, UNBOOKMARK_OPPORTUNITY, "o1", now = 2)
            assertEquals(2, awaitItem())
            dao.enqueue(BOOKMARK_OPPORTUNITY, UNBOOKMARK_OPPORTUNITY, "o1", now = 3) // replaces, no new row
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("s1", "o1"), dao.all().map { it.targetId })
    }

    @Test fun `attempts are counted and rows can be removed by id`() = runTest {
        val dao = db.pendingOperationDao()
        dao.enqueue(BOOKMARK_OPPORTUNITY, UNBOOKMARK_OPPORTUNITY, "o1", now = 1)
        val id = dao.all().single().id

        dao.incrementAttempts(id)
        assertEquals(1, dao.all().single().attempts)

        dao.deleteById(id)
        assertTrue(dao.all().isEmpty())
    }
}
