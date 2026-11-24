package com.kakuma.resourcelink.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.KrPasswordField
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.locale.AppLanguage
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.auth.domain.UserRole

@Composable
fun roleLabel(role: UserRole): String = stringResource(
    when (role) {
        UserRole.MEMBER -> R.string.role_member
        UserRole.ALUMNI -> R.string.role_alumni
        UserRole.MENTOR -> R.string.role_mentor
        UserRole.MODERATOR -> R.string.role_moderator
        UserRole.ADMIN -> R.string.role_admin
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterRoute(onBack: () -> Unit, onSignIn: () -> Unit, onRegistered: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) { if (state.completed) onRegistered() }
    UiEventEffect(viewModel.events)
    Scaffold(topBar = { KrTopBar("", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column {
                Text(stringResource(R.string.register_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            KrTextField(
                state.fullName, viewModel::onNameChange, stringResource(R.string.field_full_name),
                placeholder = stringResource(R.string.placeholder_full_name), leadingIcon = Icons.Outlined.Person,
                error = state.errors["full_name"]?.asString(),
            )
            KrTextField(
                state.email, viewModel::onEmailChange, stringResource(R.string.field_email),
                placeholder = stringResource(R.string.placeholder_email), leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email, error = state.errors["email"]?.asString(),
            )
            KrPasswordField(
                state.password, viewModel::onPasswordChange, stringResource(R.string.field_password),
                placeholder = stringResource(R.string.placeholder_password_rules), error = state.errors["password"]?.asString(),
            )
            Column {
                Text(stringResource(R.string.register_joining_as), style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    UserRole.selfAssignable.forEachIndexed { index, role ->
                        SegmentedButton(
                            selected = state.role == role,
                            onClick = { viewModel.onRoleChange(role) },
                            shape = SegmentedButtonDefaults.itemShape(index, UserRole.selfAssignable.size),
                        ) { Text(roleLabel(role), style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
            LanguageDropdown(state.language, viewModel::onLanguageChange)
            KrPrimaryButton(
                stringResource(R.string.action_create_account),
                onClick = viewModel::submit,
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.register_have_account), style = MaterialTheme.typography.bodyMedium)
                KrTextButton(stringResource(R.string.action_sign_in), onSignIn)
            }
            Spacer(Modifier.height(Spacing.md))
        }
    }
}

@Composable
fun LanguageDropdown(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(stringResource(R.string.field_preferred_language), style = MaterialTheme.typography.labelLarge)
        Box(Modifier.padding(top = 6.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text("${selected.flag}  ${selected.nativeName}", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text("${language.flag}  ${language.nativeName}") },
                        onClick = { onSelect(language); expanded = false },
                    )
                }
            }
        }
    }
}
