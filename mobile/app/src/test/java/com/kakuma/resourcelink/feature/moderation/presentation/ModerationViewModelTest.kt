package com.kakuma.resourcelink.feature.moderation.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.ui.UiEvent
import com.kakuma.resourcelink.feature.moderation.domain.ModerationRepository
import com.kakuma.resourcelink.feature.moderation.domain.ReviewQueue
import com.kakuma.resourcelink.feature.moderation.domain.ReviewStory
import com.kakuma.resourcelink.feature.stories.domain.Author
import com.kakuma.resourcelink.feature.stories.domain.Story
import com.kakuma.resourcelink.feature.stories.domain.StoryCategory
import com.kakuma.resourcelink.feature.stories.domain.StoryDetail
import com.kakuma.resourcelink.feature.stories.domain.StoryStatus
import com.kakuma.resourcelink.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ModerationViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<ModerationRepository>()

    private fun pending(id: String) = ReviewStory(
        StoryDetail(
            Story(
                id = id, title = "Story $id", excerpt = "", coverImage = null, category = StoryCategory.EDUCATION,
                status = StoryStatus.PENDING_REVIEW, author = Author("a1", "Amina Hassan", null, null), readingTime = 1,
                likesCount = 0, commentsCount = 0, isLiked = false, isBookmarked = false, publishedAt = null,
            ),
            body = "Body", rejectionReason = "",
        ),
        submittedAt = "2026-11-25T10:00:00Z",
    )

    private fun viewModel(vararg ids: String): ModerationViewModel {
        coEvery { repository.pendingStories() } returns AppResult.Success(ids.map(::pending))
        return ModerationViewModel(repository, SavedStateHandle())
    }

    @Test fun `loads the story queue on start and the project queue only when its tab opens`() {
        coEvery { repository.pendingProjects() } returns AppResult.Success(emptyList())
        val vm = viewModel("s1", "s2")

        assertEquals(listOf("s1", "s2"), vm.state.value.stories.items.map { it.detail.story.id })
        coVerify(exactly = 0) { repository.pendingProjects() }

        vm.selectQueue(ReviewQueue.PROJECTS)
        vm.selectQueue(ReviewQueue.STORIES)
        vm.selectQueue(ReviewQueue.PROJECTS)

        assertTrue(vm.state.value.projects.loaded)
        coVerify(exactly = 1) { repository.pendingProjects() }
    }

    @Test fun `rejecting a story without a reason shows an error and makes no call`() {
        val vm = viewModel("s1")
        vm.openDecision(ReviewQueue.STORIES, "s1", "Story s1", approve = false)
        vm.onNote("  ")

        vm.confirmDecision()

        assertEquals(UiText.Res(R.string.moderation_reason_required), vm.state.value.noteError)
        coVerify(exactly = 0) { repository.rejectStory(any(), any()) }
    }

    @Test fun `approving removes the story from the queue and confirms with a toast`() = runTest {
        coEvery { repository.approveStory("s1") } returns AppResult.Success(Unit)
        val vm = viewModel("s1", "s2")

        vm.events.test {
            vm.openDecision(ReviewQueue.STORIES, "s1", "Story s1", approve = true)
            vm.confirmDecision()
            assertEquals(UiEvent.Toast(UiText.Res(R.string.toast_approved)), awaitItem())
        }
        val state = vm.state.value
        assertEquals(listOf("s2"), state.stories.items.map { it.detail.story.id })
        assertNull(state.decision)
        assertFalse(state.isDeciding)
    }

    @Test fun `a story someone else already reviewed is dropped and the queue reloads`() = runTest {
        coEvery { repository.rejectStory("s1", any()) } returns AppResult.Failure(AppError.NotFound)
        val vm = viewModel("s1", "s2")
        coEvery { repository.pendingStories() } returns AppResult.Success(listOf(pending("s2")))

        vm.events.test {
            vm.openDecision(ReviewQueue.STORIES, "s1", "Story s1", approve = false)
            vm.onNote("Please add more detail")
            vm.confirmDecision()
            assertEquals(UiEvent.Snackbar(UiText.Res(R.string.moderation_already_reviewed)), awaitItem())
        }
        assertEquals(listOf("s2"), vm.state.value.stories.items.map { it.detail.story.id })
        assertNull(vm.state.value.decision)
        coVerify(exactly = 2) { repository.pendingStories() }
    }

    @Test fun `a failed decision keeps the dialog open so the moderator can retry`() = runTest {
        coEvery { repository.decideProject("p1", false, "") } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()

        vm.events.test {
            vm.openDecision(ReviewQueue.PROJECTS, "p1", "Project", approve = false)
            vm.confirmDecision()
            assertEquals(UiEvent.Snackbar(UiText.Res(R.string.error_network)), awaitItem())
        }
        assertEquals("p1", vm.state.value.decision?.id)
        assertFalse(vm.state.value.isDeciding)
    }
}
