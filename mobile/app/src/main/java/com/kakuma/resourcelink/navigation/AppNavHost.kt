package com.kakuma.resourcelink.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kakuma.resourcelink.feature.alumni.presentation.AlumniDetailRoute
import com.kakuma.resourcelink.feature.alumni.presentation.AlumniDirectoryRoute
import com.kakuma.resourcelink.feature.alumni.presentation.MyAlumniProfileRoute
import com.kakuma.resourcelink.feature.assistant.domain.SourceType
import com.kakuma.resourcelink.feature.assistant.presentation.AssistantHistoryRoute
import com.kakuma.resourcelink.feature.assistant.presentation.AssistantRoute
import com.kakuma.resourcelink.feature.auth.presentation.ForgotPasswordRoute
import com.kakuma.resourcelink.feature.auth.presentation.LanguageScreen
import com.kakuma.resourcelink.feature.auth.presentation.LoginRoute
import com.kakuma.resourcelink.feature.auth.presentation.RegisterRoute
import com.kakuma.resourcelink.feature.auth.presentation.WelcomeScreen
import com.kakuma.resourcelink.feature.home.CommunityRoute
import com.kakuma.resourcelink.feature.home.HomeActions
import com.kakuma.resourcelink.feature.home.HomeRoute
import com.kakuma.resourcelink.feature.mentorship.presentation.MentorshipRequestsRoute
import com.kakuma.resourcelink.feature.moderation.presentation.ModerationRoute
import com.kakuma.resourcelink.feature.mentorship.presentation.RequestMentorshipRoute
import com.kakuma.resourcelink.feature.notifications.domain.NotificationTarget
import com.kakuma.resourcelink.feature.notifications.presentation.NotificationsRoute
import com.kakuma.resourcelink.feature.opportunities.presentation.OpportunitiesRoute
import com.kakuma.resourcelink.feature.opportunities.presentation.OpportunityDetailRoute
import com.kakuma.resourcelink.feature.profile.EditProfileRoute
import com.kakuma.resourcelink.feature.profile.OfflineLibraryRoute
import com.kakuma.resourcelink.feature.profile.ProfileActions
import com.kakuma.resourcelink.feature.profile.ProfileRoute
import com.kakuma.resourcelink.feature.projects.presentation.ProjectDetailRoute
import com.kakuma.resourcelink.feature.projects.presentation.ProjectsRoute
import com.kakuma.resourcelink.feature.services.presentation.ServicesRoute
import com.kakuma.resourcelink.feature.stories.domain.StoryFeed
import com.kakuma.resourcelink.feature.stories.presentation.StoriesRoute
import com.kakuma.resourcelink.feature.stories.presentation.StoryDetailRoute
import com.kakuma.resourcelink.feature.stories.presentation.StoryEditorRoute

/** Switches bottom-bar tabs the standard way: one copy per tab, state saved and restored. */
fun NavHostController.navigateToTab(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Opens the Stories tab on the signed-in user's own stories (drafts, in review and published). */
fun NavHostController.openMyStories() {
    navigateToTab(Routes.STORIES)
    getBackStackEntry(Routes.STORIES).savedStateHandle[KEY_OPEN_FEED] = StoryFeed.MINE.name
}

private const val KEY_OPEN_FEED = "openFeed"

/** Leaves the auth flow after sign-in, so Back never returns to the login screen. */
fun NavHostController.finishAuth() = navigate(Routes.HOME) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

/** After sign-out: back to the welcome screen with nothing signed-in left on the back stack. */
fun NavHostController.toWelcome() = navigate(Routes.WELCOME) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

fun NavHostController.openTarget(target: NotificationTarget) {
    when (target) {
        is NotificationTarget.Story -> navigate(Routes.story(target.id))
        is NotificationTarget.Project -> navigate(Routes.project(target.id))
        is NotificationTarget.Mentorship -> navigate(Routes.mentorship(target.received))
        NotificationTarget.None -> navigate(Routes.NOTIFICATIONS) { launchSingleTop = true }
    }
}

private fun optionalString(name: String) = navArgument(name) { type = NavType.StringType; nullable = true; defaultValue = null }
private fun bool(name: String, default: Boolean) = navArgument(name) { type = NavType.BoolType; defaultValue = default }

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onOnboardingDone: () -> Unit,
    onSignedIn: () -> Unit,
    /** Member-only screens (mentorship, assistant…) send guests to sign in. */
    isLoggedIn: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val requireLogin: () -> Unit = { navController.navigate(Routes.LOGIN) { launchSingleTop = true } }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController, startDestination, modifier) {
        // --- Onboarding & auth ---
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.language(onboarding = true)) },
                onSignIn = { navController.navigate(Routes.LOGIN) },
                onBrowseAsGuest = { onOnboardingDone(); navController.finishAuth() },
            )
        }
        composable(Routes.LANGUAGE, listOf(bool("onboarding", false))) { entry ->
            val onboarding = entry.arguments?.getBoolean("onboarding") == true
            LanguageScreen(
                onBack = back,
                onContinue = { if (onboarding) navController.navigate(Routes.REGISTER) else navController.popBackStack() },
            )
        }
        composable(Routes.LOGIN) {
            LoginRoute(
                onBack = if (navController.previousBackStackEntry != null) back else null,
                onRegister = { navController.navigate(Routes.REGISTER) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onForgotPassword = { navController.navigate(Routes.FORGOT) },
                onSignedIn = { onSignedIn(); navController.finishAuth() },
            )
        }
        composable(Routes.REGISTER) {
            RegisterRoute(
                onBack = back,
                onSignIn = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true } } },
                onRegistered = { onSignedIn(); navController.finishAuth() },
            )
        }
        composable(Routes.FORGOT) { ForgotPasswordRoute(onBack = back) }

        // --- Bottom-bar tabs (no top app bar, so they pad the status bar themselves) ---
        tab(Routes.HOME) {
            HomeRoute(
                HomeActions(
                    openSearch = { navController.navigateToTab(Routes.EXPLORE) },
                    openOpportunities = { navController.navigateToTab(Routes.EXPLORE) },
                    openOpportunity = { navController.navigate(Routes.opportunity(it)) },
                    openServices = { navController.navigate(Routes.SERVICES) },
                    openStories = { navController.navigateToTab(Routes.STORIES) },
                    openStory = { navController.navigate(Routes.story(it)) },
                    openMentorship = { navController.navigate(Routes.alumni(mentorsOnly = true)) },
                    openAlumni = { navController.navigate(Routes.alumni()) },
                    openProjects = { navController.navigate(Routes.PROJECTS) },
                    openAssistant = { navController.navigate(Routes.assistant()) },
                    openNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    requireLogin = requireLogin,
                ),
            )
        }
        tab(Routes.EXPLORE) { OpportunitiesRoute(onOpen = { navController.navigate(Routes.opportunity(it)) }) }
        tab(Routes.STORIES) { entry ->
            val openFeed by entry.savedStateHandle.getStateFlow<String?>(KEY_OPEN_FEED, null).collectAsStateWithLifecycle()
            StoriesRoute(
                onOpenStory = { navController.navigate(Routes.story(it)) },
                onWrite = { navController.navigate(Routes.storyEditor(it)) },
                onRequireLogin = requireLogin,
                openFeed = openFeed?.let(StoryFeed::valueOf),
                onFeedOpened = { entry.savedStateHandle[KEY_OPEN_FEED] = null },
            )
        }
        tab(Routes.COMMUNITY) {
            CommunityRoute(
                onAlumni = { navController.navigate(Routes.alumni()) },
                onMentorship = { if (isLoggedIn()) navController.navigate(Routes.mentorship()) else requireLogin() },
                onProjects = { navController.navigate(Routes.PROJECTS) },
                onServices = { navController.navigate(Routes.SERVICES) },
            )
        }
        tab(Routes.PROFILE) {
            ProfileRoute(
                ProfileActions(
                    signIn = requireLogin,
                    editProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    language = { navController.navigate(Routes.language(onboarding = false)) },
                    savedOpportunities = { navController.navigate(Routes.savedOpportunities()) },
                    offlineLibrary = { navController.navigate(Routes.OFFLINE_LIBRARY) },
                    myStories = { navController.openMyStories() },
                    mentorship = { navController.navigate(Routes.mentorship()) },
                    alumniProfile = { navController.navigate(Routes.MY_ALUMNI) },
                    moderation = { navController.navigate(Routes.MODERATION) },
                    signedOut = { navController.toWelcome() },
                ),
            )
        }

        // --- Detail screens ---
        composable(Routes.OPPORTUNITY) {
            OpportunityDetailRoute(onBack = back, onAskAssistant = { id -> navController.navigate(Routes.assistant("explain_opportunity", id)) })
        }
        padded(Routes.SAVED_OPPORTUNITIES, listOf(bool("bookmarked", true))) {
            OpportunitiesRoute(onOpen = { navController.navigate(Routes.opportunity(it)) }, onBack = back)
        }
        padded(Routes.SERVICES) { ServicesRoute(onBack = back) }
        composable(Routes.STORY) {
            StoryDetailRoute(
                onBack = back,
                onSummarize = { id -> navController.navigate(Routes.assistant("summarize_story", id)) },
                onRequireLogin = requireLogin,
            )
        }
        composable(Routes.STORY_EDITOR, listOf(optionalString("draftId"))) {
            StoryEditorRoute(onClose = back, onSubmitted = { navController.popBackStack(); navController.openMyStories() })
        }
        padded(Routes.ALUMNI, listOf(bool("mentorsOnly", false))) {
            AlumniDirectoryRoute(
                onBack = back,
                onOpen = { navController.navigate(Routes.alumniDetail(it)) },
                onEditMyProfile = { if (isLoggedIn()) navController.navigate(Routes.MY_ALUMNI) else requireLogin() },
            )
        }
        composable(Routes.ALUMNI_DETAIL) {
            AlumniDetailRoute(onBack = back, onRequestMentorship = { id, name ->
                if (isLoggedIn()) navController.navigate(Routes.requestMentorship(id, name)) else requireLogin()
            })
        }
        composable(Routes.MY_ALUMNI) { MyAlumniProfileRoute(onBack = back) }
        padded(Routes.MENTORSHIP, listOf(optionalString("initialBox"))) {
            MentorshipRequestsRoute(onBack = back, onFindMentor = { navController.navigate(Routes.alumni(mentorsOnly = true)) })
        }
        composable(Routes.REQUEST_MENTORSHIP, listOf(optionalString("name"))) { RequestMentorshipRoute(onBack = back) }
        padded(Routes.PROJECTS) { ProjectsRoute(onBack = back, onOpen = { navController.navigate(Routes.project(it)) }) }
        composable(Routes.PROJECT) { ProjectDetailRoute(onBack = back, onRequireLogin = requireLogin) }
        composable(Routes.ASSISTANT, listOf(optionalString("task"), optionalString("targetId"))) { entry ->
            val toOpen by entry.savedStateHandle.getStateFlow<String?>(Routes.RESULT_CONVERSATION, null).collectAsStateWithLifecycle()
            if (!isLoggedIn()) {
                // The assistant is personal (history is saved), so it needs an account.
                LaunchedEffect(Unit) { navController.popBackStack(); requireLogin() }
            } else AssistantRoute(
                onBack = back,
                onOpenSource = { s ->
                    when (s.type) {
                        SourceType.OPPORTUNITY -> navController.navigate(Routes.opportunity(s.id))
                        SourceType.STORY -> navController.navigate(Routes.story(s.id))
                        SourceType.PROJECT -> navController.navigate(Routes.project(s.id))
                        SourceType.SERVICE -> navController.navigate(Routes.SERVICES)
                        SourceType.OTHER -> Unit
                    }
                },
                onHistory = { navController.navigate(Routes.ASSISTANT_HISTORY) },
                conversationToOpen = toOpen,
                onConversationOpened = { entry.savedStateHandle[Routes.RESULT_CONVERSATION] = null },
            )
        }
        composable(Routes.ASSISTANT_HISTORY) {
            AssistantHistoryRoute(onBack = back, onOpen = { id ->
                navController.previousBackStackEntry?.savedStateHandle?.set(Routes.RESULT_CONVERSATION, id)
                navController.popBackStack()
            })
        }
        padded(Routes.NOTIFICATIONS) { NotificationsRoute(onBack = back, onNavigate = navController::openTarget) }
        composable(Routes.EDIT_PROFILE) { EditProfileRoute(onBack = back) }
        padded(Routes.MODERATION) { ModerationRoute(onBack = back) }
        composable(Routes.OFFLINE_LIBRARY) { OfflineLibraryRoute(onBack = back, onOpen = { navController.navigate(Routes.story(it)) }) }
    }
}

private fun NavGraphBuilder.tab(route: String, content: @Composable (NavBackStackEntry) -> Unit) =
    composable(route) { entry -> Box(Modifier.fillMaxSize().statusBarsPadding()) { content(entry) } }

/** Non-tab screens built from a Column (not a Scaffold) keep clear of the gesture/navigation bar. */
private fun NavGraphBuilder.padded(route: String, args: List<androidx.navigation.NamedNavArgument> = emptyList(), content: @Composable () -> Unit) =
    composable(route, args) { Box(Modifier.fillMaxSize().navigationBarsPadding()) { content() } }
