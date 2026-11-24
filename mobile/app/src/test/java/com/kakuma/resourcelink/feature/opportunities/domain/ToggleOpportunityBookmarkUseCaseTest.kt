package com.kakuma.resourcelink.feature.opportunities.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ToggleOpportunityBookmarkUseCaseTest {
    private val repository = mockk<OpportunityRepository>()
    private val useCase = ToggleOpportunityBookmarkUseCase(repository)

    private fun opportunity(bookmarked: Boolean) = mockk<Opportunity> {
        every { id } returns "o1"
        every { isBookmarked } returns bookmarked
    }

    @Test fun `bookmarks an unsaved opportunity`() = runTest {
        coEvery { repository.setBookmarked("o1", true) } returns SyncOutcome.Synced

        assertEquals(SyncOutcome.Synced, useCase(opportunity(bookmarked = false)))
        coVerify { repository.setBookmarked("o1", true) }
    }

    @Test fun `removes a saved opportunity and reports offline queueing`() = runTest {
        coEvery { repository.setBookmarked("o1", false) } returns SyncOutcome.QueuedOffline

        assertEquals(SyncOutcome.QueuedOffline, useCase(opportunity(bookmarked = true)))
    }
}
