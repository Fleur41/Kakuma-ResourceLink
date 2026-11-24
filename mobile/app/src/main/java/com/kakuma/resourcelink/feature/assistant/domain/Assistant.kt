package com.kakuma.resourcelink.feature.assistant.domain

import com.kakuma.resourcelink.core.common.AppResult

enum class AssistantTask(val apiValue: String) {
    CHAT("chat"), EXPLAIN_OPPORTUNITY("explain_opportunity"), SUMMARIZE_STORY("summarize_story"), TRANSLATE("translate");
    companion object { fun from(v: String?) = entries.firstOrNull { it.apiValue == v } ?: CHAT }
}

enum class SourceType { OPPORTUNITY, STORY, SERVICE, PROJECT, OTHER }

data class Source(val type: SourceType, val id: String, val title: String)

data class ChatMessage(val id: String, val fromUser: Boolean, val content: String, val sources: List<Source> = emptyList())

data class ChatReply(val conversationId: String, val message: ChatMessage)

data class Conversation(val id: String, val title: String, val updatedAt: String)

interface AssistantRepository {
    suspend fun send(
        message: String,
        conversationId: String?,
        language: String,
        task: AssistantTask = AssistantTask.CHAT,
        targetId: String? = null,
        targetLanguage: String? = null,
    ): AppResult<ChatReply>
    suspend fun conversations(): AppResult<List<Conversation>>
    suspend fun conversation(id: String): AppResult<List<ChatMessage>>
    suspend fun delete(id: String): AppResult<Unit>
}
