package com.kakuma.resourcelink.domain

import com.kakuma.resourcelink.feature.alumni.domain.parseTags
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipBox
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipRules
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipStatus
import com.kakuma.resourcelink.feature.projects.domain.ContributionType
import com.kakuma.resourcelink.feature.projects.domain.PledgeRules
import com.kakuma.resourcelink.feature.projects.domain.PledgeValidation
import com.kakuma.resourcelink.feature.services.domain.EssentialService
import com.kakuma.resourcelink.feature.services.domain.ServiceCategory
import com.kakuma.resourcelink.feature.services.domain.filterBy
import com.kakuma.resourcelink.feature.stories.domain.DraftError
import com.kakuma.resourcelink.feature.stories.domain.StoryValidation
import com.kakuma.resourcelink.sync.OperationResult
import com.kakuma.resourcelink.sync.toOperationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainRulesTest {

    // Stories

    @Test fun `story submit requires title, body and consent`() {
        assertEquals(
            mapOf("title" to DraftError.TITLE_TOO_SHORT, "body" to DraftError.BODY_TOO_SHORT, "consent" to DraftError.CONSENT_REQUIRED),
            StoryValidation.forSubmit(" ab ", "short", consent = false),
        )
        assertTrue(StoryValidation.forSubmit("My story", "x".repeat(StoryValidation.MIN_BODY), consent = true).isEmpty())
    }

    @Test fun `reading time is at least one minute and about 200 words per minute`() {
        assertEquals(1, StoryValidation.readingMinutes(""))
        assertEquals(1, StoryValidation.readingMinutes("word ".repeat(150)))
        assertEquals(3, StoryValidation.readingMinutes("word ".repeat(600)))
    }

    // Alumni

    @Test fun `tags are trimmed, de-duplicated case-insensitively and capped`() {
        assertEquals(listOf("Design", "python"), parseTags(" Design, python , ,design,Python"))
        assertEquals(20, parseTags((1..30).joinToString(",") { "tag$it" }).size)
        assertEquals(50, parseTags("x".repeat(80)).single().length)
    }

    // Mentorship

    @Test fun `mentor can accept or decline a pending request`() {
        assertEquals(
            listOf(MentorshipStatus.ACCEPTED, MentorshipStatus.DECLINED),
            MentorshipRules.actionsFor(MentorshipBox.RECEIVED, MentorshipStatus.PENDING),
        )
    }

    @Test fun `mentee can only cancel a pending request`() {
        assertEquals(listOf(MentorshipStatus.CANCELLED), MentorshipRules.actionsFor(MentorshipBox.SENT, MentorshipStatus.PENDING))
    }

    @Test fun `both sides can complete an accepted request and closed requests have no actions`() {
        MentorshipBox.entries.forEach { box ->
            assertEquals(listOf(MentorshipStatus.COMPLETED), MentorshipRules.actionsFor(box, MentorshipStatus.ACCEPTED))
            listOf(MentorshipStatus.DECLINED, MentorshipStatus.CANCELLED, MentorshipStatus.COMPLETED).forEach {
                assertTrue(MentorshipRules.actionsFor(box, it).isEmpty())
            }
        }
    }

    // Projects

    @Test fun `pledge needs a description`() {
        assertEquals(PledgeValidation.DescriptionTooShort, PledgeRules.validate(ContributionType.MENTORING, " a ", ""))
        assertEquals(PledgeValidation.Ok, PledgeRules.validate(ContributionType.MENTORING, "Weekly tutoring", ""))
    }

    @Test fun `financial pledge needs a positive amount, other types ignore it`() {
        listOf("", "0", "-5", "abc").forEach {
            assertEquals(it, PledgeValidation.AmountRequired, PledgeRules.validate(ContributionType.FINANCIAL_PLEDGE, "Support", it))
        }
        assertEquals(PledgeValidation.Ok, PledgeRules.validate(ContributionType.FINANCIAL_PLEDGE, "Support", "12.50"))
        assertEquals(PledgeValidation.Ok, PledgeRules.validate(ContributionType.EQUIPMENT, "Laptops", "abc"))
    }

    // Services

    @Test fun `services filter by category and by name, organisation or description`() {
        val clinic = service("1", "Clinic", ServiceCategory.HEALTHCARE, org = "Health partner")
        val legal = service("2", "Legal desk", ServiceCategory.LEGAL, description = "Help with documents")
        val all = listOf(clinic, legal)

        assertEquals(all, all.filterBy(null, ""))
        assertEquals(listOf(legal), all.filterBy(ServiceCategory.LEGAL, ""))
        assertEquals(listOf(clinic), all.filterBy(null, "  PARTNER "))
        assertEquals(listOf(legal), all.filterBy(null, "documents"))
        assertTrue(all.filterBy(ServiceCategory.HEALTHCARE, "documents").isEmpty())
    }

    // Offline sync

    @Test fun `http status decides whether a queued operation is done, retried or dropped`() {
        assertEquals(OperationResult.DONE, 200.toOperationResult())
        assertEquals(OperationResult.DONE, 204.toOperationResult())
        assertEquals(OperationResult.RETRY, 408.toOperationResult())
        assertEquals(OperationResult.RETRY, 429.toOperationResult())
        assertEquals(OperationResult.DROP, 404.toOperationResult())
        assertEquals(OperationResult.DROP, 403.toOperationResult())
        assertEquals(OperationResult.RETRY, 500.toOperationResult())
        assertEquals(OperationResult.RETRY, 503.toOperationResult())
    }

    private fun service(
        id: String,
        name: String,
        category: ServiceCategory,
        org: String = "Org",
        description: String = "",
    ) = EssentialService(id, name, category, org, description, "", "", "", "", "", false)
}
