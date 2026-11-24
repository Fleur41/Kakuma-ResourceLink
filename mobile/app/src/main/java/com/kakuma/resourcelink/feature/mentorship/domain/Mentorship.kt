package com.kakuma.resourcelink.feature.mentorship.domain

import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.feature.stories.domain.Author
import kotlinx.coroutines.flow.Flow

enum class HelpArea(val apiValue: String) {
    CAREER("career"), TECHNICAL("technical"), SCHOLARSHIPS("scholarships"), ENTREPRENEURSHIP("entrepreneurship"), OTHER("other");
    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } ?: OTHER }
}

enum class MentorshipStatus(val apiValue: String) {
    PENDING("pending"), ACCEPTED("accepted"), DECLINED("declined"), CANCELLED("cancelled"), COMPLETED("completed");
    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } ?: PENDING }
}

enum class MentorshipBox(val apiValue: String) { SENT("sent"), RECEIVED("received") }

data class MentorshipRequest(
    val id: String,
    val mentee: Author,
    val mentor: Author,
    val goal: String,
    val helpAreas: List<HelpArea>,
    val message: String,
    val status: MentorshipStatus,
    val responseMessage: String,
    val createdAt: String,
)

/** Mirrors the backend state machine so only valid buttons are shown. */
object MentorshipRules {
    fun actionsFor(box: MentorshipBox, status: MentorshipStatus): List<MentorshipStatus> = when (box) {
        MentorshipBox.RECEIVED -> when (status) {
            MentorshipStatus.PENDING -> listOf(MentorshipStatus.ACCEPTED, MentorshipStatus.DECLINED)
            MentorshipStatus.ACCEPTED -> listOf(MentorshipStatus.COMPLETED)
            else -> emptyList()
        }
        MentorshipBox.SENT -> when (status) {
            MentorshipStatus.PENDING -> listOf(MentorshipStatus.CANCELLED)
            MentorshipStatus.ACCEPTED -> listOf(MentorshipStatus.COMPLETED)
            else -> emptyList()
        }
    }

    const val MIN_GOAL = 3
    const val MIN_MESSAGE = 10
}

interface MentorshipRepository {
    fun requests(box: MentorshipBox): Flow<PagingData<MentorshipRequest>>
    suspend fun send(mentorProfileId: String, goal: String, helpAreas: Set<HelpArea>, message: String): AppResult<MentorshipRequest>
    suspend fun updateStatus(id: String, status: MentorshipStatus, response: String?): AppResult<MentorshipRequest>
}
