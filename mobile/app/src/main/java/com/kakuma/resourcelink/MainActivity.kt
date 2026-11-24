package com.kakuma.resourcelink

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kakuma.resourcelink.core.designsystem.KakumaTheme
import com.kakuma.resourcelink.core.ui.LocalSnackbarHostState
import com.kakuma.resourcelink.core.ui.ObserveAsEvents
import com.kakuma.resourcelink.core.ui.UiEvent
import com.kakuma.resourcelink.navigation.AppNavHost
import com.kakuma.resourcelink.navigation.Routes
import com.kakuma.resourcelink.navigation.navigateToTab
import com.kakuma.resourcelink.navigation.openTarget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    /** Latest notification-tap intent (launch or onNewIntent with singleTop). */
    private val pendingIntent = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.startDestination.value == null }
        enableEdgeToEdge()
        // Only handle the launch intent once (not again after rotation/process recreation).
        if (savedInstanceState == null) pendingIntent.value = intent

        setContent {
            val theme by viewModel.themeMode.collectAsStateWithLifecycle()
            KakumaTheme(theme) {
                val start by produceState<String?>(null) { value = viewModel.awaitStart() }
                start?.let { KakumaAppUi(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent.value = intent
    }

    @Composable
    private fun KakumaAppUi(startDestination: String) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val loggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

        // App-wide one-off messages (sign-in welcome, session expired) survive screen changes.
        ObserveAsEvents(viewModel.appMessages.events) { event ->
            if (event is UiEvent.Toast) Toast.makeText(this, event.message.asString(this), Toast.LENGTH_SHORT).show()
        }
        ObserveAsEvents(viewModel.events) { event ->
            when (event) {
                AppNavEvent.SessionExpired -> navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.HOME) { inclusive = false }
                    launchSingleTop = true
                }
                is AppNavEvent.Open -> navController.openTarget(event.target)
            }
        }
        val intentState by pendingIntent.collectAsStateWithLifecycle()
        LaunchedEffect(intentState) {
            intentState?.let { handleNotificationIntent(it); pendingIntent.value = null }
        }
        // Ask for notification permission once, after the person signs in (Android 13+).
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(loggedIn) {
            val prefs = getSharedPreferences("app_flags", MODE_PRIVATE)
            if (loggedIn && Build.VERSION.SDK_INT >= 33 && !prefs.getBoolean("asked_notifications", false)) {
                prefs.edit().putBoolean("asked_notifications", true).apply()
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val backStack by navController.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = { if (route in Routes.topLevel) BottomBar(navController, route) },
                // Each screen handles its own insets (top bars, IME); only the bottom bar is accounted for here.
                contentWindowInsets = WindowInsets(0),
            ) { padding ->
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    onOnboardingDone = viewModel::completeOnboarding,
                    onSignedIn = viewModel::onSignedIn,
                    isLoggedIn = { viewModel.isLoggedIn.value },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent) {
        val type = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: return
        val data = intent.extras?.keySet().orEmpty()
            .filter { it.startsWith("data_") }
            .associate { it.removePrefix("data_") to intent.getStringExtra(it).orEmpty() }
        viewModel.onNotificationTapped(intent.getStringExtra(EXTRA_NOTIFICATION_ID), type, data)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    }
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.EXPLORE, R.string.nav_explore, Icons.Outlined.Search, Icons.Filled.Search),
    Tab(Routes.STORIES, R.string.nav_stories, Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    Tab(Routes.COMMUNITY, R.string.nav_community, Icons.Outlined.Groups, Icons.Filled.Groups),
    Tab(Routes.PROFILE, R.string.nav_profile, Icons.Outlined.Person, Icons.Filled.Person),
)

@Composable
private fun BottomBar(navController: NavHostController, current: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            val selected = current == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) navController.navigateToTab(tab.route) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label), maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

