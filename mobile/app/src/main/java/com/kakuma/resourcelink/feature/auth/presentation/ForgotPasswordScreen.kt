package com.kakuma.resourcelink.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.ui.UiEventEffect

@Composable
fun ForgotPasswordRoute(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UiEventEffect(viewModel.events)
    Scaffold(topBar = { KrTopBar(stringResource(R.string.forgot_title), onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (state.sent) {
                EmptyState(
                    icon = Icons.Outlined.MarkEmailRead,
                    title = stringResource(R.string.forgot_sent_title),
                    body = stringResource(R.string.forgot_sent_body),
                    actionLabel = stringResource(R.string.action_back_to_sign_in),
                    onAction = onBack,
                )
            } else {
                Text(
                    stringResource(R.string.forgot_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                KrTextField(
                    state.email, viewModel::onEmailChange, stringResource(R.string.field_email),
                    placeholder = stringResource(R.string.placeholder_email), leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email, error = state.error?.asString(),
                )
                KrPrimaryButton(
                    stringResource(R.string.action_send_reset_link), viewModel::submit,
                    loading = state.isLoading, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
