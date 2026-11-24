package com.kakuma.resourcelink.feature.assistant.data

import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.network.PageDto
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.assistant.domain.AssistantRepository
import com.kakuma.resourcelink.feature.assistant.domain.AssistantTask
import com.kakuma.resourcelink.feature.assistant.domain.ChatMessage
import com.kakuma.resourcelink.feature.assistant.domain.ChatReply
import com.kakuma.resourcelink.feature.assistant.domain.Conversation
import com.kakuma.resourcelink.feature.assistant.domain.Source
import com.kakuma.resourcelink.feature.assistant.domain.SourceType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class SourceDto(val type: String, val id: String, val title: String)

@JsonClass(generateAdapter = true)
data class AiMessageDto(val id: String, val role: String, val content: String, val sources: List<SourceDto>? = null)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val message: String,
    @Json(name = "conversation_id") val conversationId: String? = null,
    val language: String? = null,
    val task: String = "chat",
    @Json(name = "target_id") val targetId: String? = null,
    @Json(name = "target_language") val targetLanguage: String? = null,
)

@JsonClass(generateAdapter = true)
data class ChatResponseDto(
    @Json(name = "conversation_id") val conversationId: String,
    val message: AiMessageDto,
    val sources: List<SourceDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ConversationDto(
    val id: String,
    val title: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    val messages: List<AiMessageDto>? = null,
)

interface AssistantApi {
    @POST("ai/chat/")
    suspend fun chat(@Body body: ChatRequest): ChatResponseDto

    @GET("ai/conversations/")
    suspend fun conversations(): PageDto<ConversationDto>

    @GET("ai/conversations/{id}/")
    suspend fun conversation(@Path("id") id: String): ConversationDto

    @DELETE("ai/conversations/{id}/")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}

fun SourceDto.toDomain() = Source(
    when (type) {
        "opportunity" -> SourceType.OPPORTUNITY
        "story" -> SourceType.STORY
        "service" -> SourceType.SERVICE
        "project" -> SourceType.PROJECT
        else -> SourceType.OTHER
    },
    id, title,
)

fun AiMessageDto.toDomain(fallbackSources: List<SourceDto> = emptyList()) =
    ChatMessage(id, role == "user", content, (sources ?: fallbackSources).map { it.toDomain() })

@Singleton
class AssistantRepositoryImpl @Inject constructor(private val api: AssistantApi, private val moshi: Moshi) : AssistantRepository {
    override suspend fun send(
        message: String, conversationId: String?, language: String, task: AssistantTask, targetId: String?, targetLanguage: String?,
    ): AppResult<ChatReply> = safeApiCall(moshi) {
        api.chat(ChatRequest(message.trim(), conversationId, language, task.apiValue, targetId, targetLanguage))
    }.map { ChatReply(it.conversationId, it.message.toDomain(it.sources)) }

    override suspend fun conversations() =
        safeApiCall(moshi) { api.conversations() }.map { page -> page.results.map { Conversation(it.id, it.title, it.updatedAt) } }

    override suspend fun conversation(id: String) =
        safeApiCall(moshi) { api.conversation(id) }.map { c -> c.messages.orEmpty().map { it.toDomain() } }

    override suspend fun delete(id: String): AppResult<Unit> = safeApiCall(moshi) {
        val r = api.delete(id)
        if (!r.isSuccessful) throw HttpException(r)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {
    @Binds @Singleton abstract fun repository(impl: AssistantRepositoryImpl): AssistantRepository

    companion object {
        @Provides @Singleton fun api(retrofit: Retrofit): AssistantApi = retrofit.create(AssistantApi::class.java)
    }
}
