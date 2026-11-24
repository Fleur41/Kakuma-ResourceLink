package com.kakuma.resourcelink.navigation

import android.net.Uri

/** String routes; arguments are URL-encoded so titles/names with spaces are safe. */
object Routes {
    const val WELCOME = "welcome"
    const val LANGUAGE = "language?onboarding={onboarding}"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"

    const val HOME = "home"
    const val EXPLORE = "explore"
    const val STORIES = "stories"
    const val COMMUNITY = "community"
    const val PROFILE = "profile"

    const val OPPORTUNITY = "opportunity/{id}"
    const val SAVED_OPPORTUNITIES = "saved_opportunities?bookmarked={bookmarked}"
    const val SERVICES = "services"
    const val STORY = "story/{id}"
    const val STORY_EDITOR = "story_editor?draftId={draftId}"
    const val ALUMNI = "alumni?mentorsOnly={mentorsOnly}"
    const val ALUMNI_DETAIL = "alumni/{id}"
    const val MY_ALUMNI = "my_alumni"
    const val MENTORSHIP = "mentorship?initialBox={initialBox}"
    const val REQUEST_MENTORSHIP = "request_mentorship/{mentorId}?name={name}"
    const val PROJECTS = "projects"
    const val PROJECT = "project/{id}"
    const val ASSISTANT = "assistant?task={task}&targetId={targetId}"
    const val ASSISTANT_HISTORY = "assistant_history"
    const val NOTIFICATIONS = "notifications"
    const val EDIT_PROFILE = "edit_profile"
    const val OFFLINE_LIBRARY = "offline_library"

    val topLevel = setOf(HOME, EXPLORE, STORIES, COMMUNITY, PROFILE)

    fun language(onboarding: Boolean) = "language?onboarding=$onboarding"
    fun opportunity(id: String) = "opportunity/${Uri.encode(id)}"
    fun savedOpportunities() = "saved_opportunities?bookmarked=true"
    fun story(id: String) = "story/${Uri.encode(id)}"
    fun storyEditor(draftId: String?) = if (draftId == null) "story_editor" else "story_editor?draftId=${Uri.encode(draftId)}"
    fun alumni(mentorsOnly: Boolean = false) = "alumni?mentorsOnly=$mentorsOnly"
    fun alumniDetail(id: String) = "alumni/${Uri.encode(id)}"
    fun mentorship(received: Boolean = false) = "mentorship?initialBox=${if (received) "received" else "sent"}"
    fun requestMentorship(mentorId: String, name: String) = "request_mentorship/${Uri.encode(mentorId)}?name=${Uri.encode(name)}"
    fun project(id: String) = "project/${Uri.encode(id)}"
    fun assistant(task: String? = null, targetId: String? = null) =
        if (task == null) "assistant" else "assistant?task=$task&targetId=${Uri.encode(targetId.orEmpty())}"

    /** Result key used by the history screen to open a past conversation. */
    const val RESULT_CONVERSATION = "open_conversation"
}
