package com.kakuma.resourcelink.feature.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.BuildConfig
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.datastore.ThemeMode
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.ConfirmDialog
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.InitialAvatar
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.auth.presentation.roleLabel

data class ProfileActions(
    val signIn: () -> Unit,
    val editProfile: () -> Unit,
    val language: () -> Unit,
    val savedOpportunities: () -> Unit,
    val offlineLibrary: () -> Unit,
    val myStories: () -> Unit,
    val mentorship: () -> Unit,
    val alumniProfile: () -> Unit,
)

@Composable
fun ProfileRoute(actions: ProfileActions, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var themeDialog by rememberSaveable { mutableStateOf(false) }
    UiEventEffect(viewModel.events)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { viewModel.onAvatarPicked(it) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.sm)) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.sm))
        val user = state.user
        if (user == null || !state.isLoggedIn) {
            EmptyState(
                Icons.Outlined.Public, stringResource(R.string.profile_guest_title), stringResource(R.string.profile_guest_body),
                actionLabel = stringResource(R.string.action_sign_in), onAction = actions.signIn,
            )
        } else {
            KrCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        InitialAvatar(user.fullName, user.avatarUrl, size = 72.dp)
                        SmallFloatingActionButton(
                            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.align(Alignment.BottomEnd).size(30.dp),
                        ) {
                            if (state.isUploadingAvatar) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Outlined.CameraAlt, stringResource(R.string.cd_change_photo), Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(roleLabel(user.role), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (user.bio.isNotBlank()) Text(user.bio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.xs))
            }
            Spacer(Modifier.height(Spacing.sm))
            SettingsGroup(stringResource(R.string.profile_my_activity)) {
                SettingRow(Icons.Outlined.Edit, stringResource(R.string.profile_edit), onClick = actions.editProfile)
                SettingRow(Icons.Outlined.EditNote, stringResource(R.string.profile_my_stories), onClick = actions.myStories)
                SettingRow(Icons.Outlined.Bookmarks, stringResource(R.string.opp_saved_title), onClick = actions.savedOpportunities)
                SettingRow(Icons.Outlined.Handshake, stringResource(R.string.mentorship_title), onClick = actions.mentorship)
                SettingRow(Icons.Outlined.Public, stringResource(R.string.alumni_my_profile), onClick = actions.alumniProfile)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        SettingsGroup(stringResource(R.string.profile_settings)) {
            SettingRow(Icons.Outlined.Language, stringResource(R.string.settings_language), value = state.languageName, onClick = actions.language)
            SettingRow(
                Icons.Outlined.DarkMode, stringResource(R.string.settings_theme),
                value = stringResource(themeLabel(state.themeMode)), onClick = { themeDialog = true },
            )
            SettingRow(Icons.Outlined.DownloadForOffline, stringResource(R.string.settings_offline_library), onClick = actions.offlineLibrary)
            SettingRow(Icons.Outlined.Notifications, stringResource(R.string.settings_notifications), onClick = {
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            })
            SettingRow(Icons.Outlined.Info, stringResource(R.string.settings_about), value = BuildConfig.VERSION_NAME, onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            })
        }
        if (state.isLoggedIn) {
            Spacer(Modifier.height(Spacing.sm))
            KrCard(onClick = { confirmLogout = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isLoggingOut) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.action_sign_out), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
    if (confirmLogout) ConfirmDialog(
        stringResource(R.string.dialog_sign_out_title), stringResource(R.string.dialog_sign_out_body), stringResource(R.string.action_sign_out),
        onConfirm = { confirmLogout = false; viewModel.logout() }, onDismiss = { confirmLogout = false },
    )
    if (themeDialog) AlertDialog(
        onDismissRequest = { themeDialog = false },
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier.fillMaxWidth().selectable(state.themeMode == mode, role = Role.RadioButton) { viewModel.setTheme(mode); themeDialog = false }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(state.themeMode == mode, onClick = null)
                        Text(stringResource(themeLabel(mode)), Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton({ themeDialog = false }) { Text(stringResource(R.string.action_close)) } },
    )
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
    KrCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) { content() }
}

@Composable
private fun SettingRow(icon: ImageVector, label: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = Spacing.md, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        value?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EditProfileRoute(onBack: () -> Unit, viewModel: EditProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UiEventEffect(viewModel.events)
    Scaffold(topBar = { KrTopBar(stringResource(R.string.profile_edit), onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            KrTextField(state.fullName, viewModel::onName, stringResource(R.string.field_full_name), error = state.errors["full_name"]?.asString())
            KrTextField(state.location, viewModel::onLocation, stringResource(R.string.field_location))
            KrTextField(state.bio, viewModel::onBio, stringResource(R.string.field_bio), singleLine = false, minLines = 4)
            KrPrimaryButton(stringResource(R.string.action_save_changes), { viewModel.save(onBack) }, Modifier.fillMaxWidth(), loading = state.isSaving)
        }
    }
}

