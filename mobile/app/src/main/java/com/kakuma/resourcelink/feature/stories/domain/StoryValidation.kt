package com.kakuma.resourcelink.feature.stories.domain

enum class DraftError { TITLE_TOO_SHORT, BODY_TOO_SHORT, CONSENT_REQUIRED }

object StoryValidation {
    const val MIN_TITLE = 3
    const val MIN_BODY = 50
    const val MAX_TITLE = 200

    /** Rules for sending a story to review. Saving a draft is always allowed. */
    fun forSubmit(title: String, body: String, consent: Boolean): Map<String, DraftError> = buildMap {
        if (title.trim().length < MIN_TITLE) put("title", DraftError.TITLE_TOO_SHORT)
        if (body.trim().length < MIN_BODY) put("body", DraftError.BODY_TOO_SHORT)
        if (!consent) put("consent", DraftError.CONSENT_REQUIRED)
    }

    fun readingMinutes(body: String): Int = maxOf(1, body.split(Regex("\\s+")).count { it.isNotBlank() } / 200)
}
