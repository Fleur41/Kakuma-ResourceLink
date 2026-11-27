package com.kakuma.resourcelink.feature.auth.presentation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import com.kakuma.resourcelink.core.ui.UiEventEffect

/** Forgot password: email → 6-digit code from the email → new password, then back to sign in. */
@Composable
fun ForgotPasswordRoute(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UiEventEffect(viewModel.events)
    val context = LocalContext.current
    // Shown here rather than as a UiEvent, because this screen closes straight after.
    val finish = {
        Toast.makeText(context, context.getString(R.string.reset_done), Toast.LENGTH_SHORT).show()
        onBack()
    }
    // Back on the code step returns to the email step instead of leaving.
    BackHandler(enabled = state.step == ResetStep.CODE) { viewModel.changeEmail() }

    val title = when (state.step) {
        ResetStep.EMAIL -> R.string.forgot_title
        ResetStep.CODE -> R.string.code_title
        ResetStep.PASSWORD -> R.string.reset_title
    }
    Scaffold(topBar = { KrTopBar(stringResource(title), onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (state.step) {
                ResetStep.EMAIL -> EmailStep(state, viewModel)
                ResetStep.CODE -> CodeStep(state, viewModel)
                ResetStep.PASSWORD -> PasswordStep(state, viewModel, finish)
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EmailStep(state: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Hint(stringResource(R.string.forgot_body))
    KrTextField(
        state.email, viewModel::onEmailChange, stringResource(R.string.field_email),
        placeholder = stringResource(R.string.placeholder_email), leadingIcon = Icons.Outlined.Email,
        keyboardType = KeyboardType.Email, imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { viewModel.sendCode() }),
        error = state.emailError?.asString(),
    )
    KrPrimaryButton(stringResource(R.string.action_send_code), viewModel::sendCode, loading = state.isLoading, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CodeStep(state: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Hint(stringResource(R.string.code_body, state.email.trim()))
    KrTextField(
        state.code, viewModel::onCodeChange, stringResource(R.string.field_code),
        placeholder = "000000", leadingIcon = Icons.Outlined.Pin,
        keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { viewModel.verifyCode() }),
        maxLength = ForgotPasswordViewModel.CODE_LENGTH,
        error = state.codeError?.asString(),
    )
    Text(stringResource(R.string.code_check_spam), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    KrPrimaryButton(stringResource(R.string.action_continue), viewModel::verifyCode, loading = state.isLoading, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        KrTextButton(stringResource(R.string.code_change_email), viewModel::changeEmail)
        if (state.resendIn > 0) {
            Text(
                stringResource(R.string.code_resend_in, state.resendIn),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 12.dp),
            )
        } else {
            KrTextButton(stringResource(R.string.code_resend), viewModel::resendCode)
        }
    }
}

@Composable
private fun PasswordStep(state: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel, onDone: () -> Unit) {
    Hint(stringResource(R.string.reset_body))
    KrPasswordField(
        state.password, viewModel::onPasswordChange, stringResource(R.string.field_new_password),
        error = state.passwordError?.asString(), imeAction = ImeAction.Next,
    )
    KrPasswordField(
        state.confirm, viewModel::onConfirmChange, stringResource(R.string.field_confirm_new_password),
        error = state.confirmError?.asString(),
        keyboardActions = KeyboardActions(onDone = { viewModel.savePassword(onDone) }),
    )
    KrPrimaryButton(stringResource(R.string.reset_save), { viewModel.savePassword(onDone) }, loading = state.isLoading, modifier = Modifier.fillMaxWidth())
}
