package com.kakuma.resourcelink.feature.assistant.presentation

import androidx.lifecycle.SavedStateHandle
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.connectivity.ConnectivityObserver
import com.kakuma.resourcelink.core.locale.AppLanguage
import com.kakuma.resourcelink.core.locale.LanguageManager
import com.kakuma.resourcelink.feature.assistant.domain.AssistantRepository
import com.kakuma.resourcelink.feature.assistant.domain.ChatMessage
import com.kakuma.resourcelink.feature.assistant.domain.ChatReply
import com.kakuma.resourcelink.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AssistantViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<AssistantRepository>()
    private val language = mockk<LanguageManager> { every { current() } returns AppLanguage.ENGLISH }
    private val connectivity = object : ConnectivityObserver { override val isOnline = flowOf(true) }

    private fun reply(conversationId: String, text: String) =
        AppResult.Success(ChatReply(conversationId, ChatMessage("r-$text", fromUser = false, content = text)))

    @Test fun `new chat drops a reply that was still on its way and starts a fresh conversation`() = runTest(mainDispatcher.dispatcher) {
        val slowReply = CompletableDeferred<AppResult<ChatReply>>()
        coEvery { repository.send("old question", null, "en") } coAnswers { slowReply.await() }
        coEvery { repository.send("new question", null, "en") } returns reply("c-new", "new answer")
        val vm = AssistantViewModel(repository, language, connectivity, SavedStateHandle())
        backgroundScope.launch { vm.state.collect {} }

        vm.send("old question")
        assertTrue(vm.state.value.isThinking)

        vm.newConversation()
        slowReply.complete(reply("c-old", "old answer"))
        assertTrue(vm.state.value.messages.isEmpty())
        assertFalse(vm.state.value.isThinking)

        // The next message must start a new conversation (no conversation id), not continue the old one.
        vm.send("new question")
        assertEquals(listOf("new question", "new answer"), vm.state.value.messages.map { it.content })
        coVerify(exactly = 1) { repository.send("new question", null, "en") }
    }
}
