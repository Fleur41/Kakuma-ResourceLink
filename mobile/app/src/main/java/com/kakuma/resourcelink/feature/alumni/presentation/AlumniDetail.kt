package com.kakuma.resourcelink.feature.alumni.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.CategoryPill
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.FullScreenLoading
import com.kakuma.resourcelink.core.designsystem.component.InitialAvatar
import com.kakuma.resourcelink.core.designsystem.component.KrOutlinedButton
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.feature.alumni.domain.AlumniProfile
import com.kakuma.resourcelink.feature.alumni.domain.AlumniRepository
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlumniDetailUiState(
    val profile: AlumniProfile? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val isSelf: Boolean = false,
)

@HiltViewModel
class AlumniDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: AlumniRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedState["id"])
    private val data = MutableStateFlow(AlumniDetailUiState())
    val state = combine(data, authRepository.currentUser) { s, user -> s.copy(isSelf = user != null && s.profile?.userId == user.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlumniDetailUiState())

    init { load() }

    fun load() {
        viewModelScope.launch {
            data.value = data.value.copy(isLoading = true, error = null)
            data.value = when (val r = repository.get(id)) {
                is AppResult.Success -> AlumniDetailUiState(profile = r.data, isLoading = false)
                is AppResult.Failure -> AlumniDetailUiState(isLoading = false, error = r.error.toUiText())
            }
        }
    }
}

@Composable
fun AlumniDetailRoute(
    onBack: () -> Unit,
    onRequestMentorship: (profileId: String, name: String) -> Unit,
    viewModel: AlumniDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(topBar = { KrTopBar(stringResource(R.string.alumni_profile_title), onBack = onBack) }) { padding ->
        val p = state.profile
        when {
            p == null && state.isLoading -> FullScreenLoading(Modifier.padding(padding))
            p == null -> ErrorState(state.error?.asString().orEmpty(), viewModel::load, Modifier.padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InitialAvatar(p.fullName, p.avatarUrl, size = 96.dp)
                Text(p.fullName, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    listOf(p.profession, p.organization).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                )
                Text(
                    listOfNotNull(p.city?.ifBlank { null }, p.country.ifBlank { null }).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (p.bio.isNotBlank()) {
                        Section(stringResource(R.string.alumni_about)) { Text(p.bio, style = MaterialTheme.typography.bodyMedium) }
                    }
                    if (p.skills.isNotEmpty()) Section(stringResource(R.string.alumni_skills)) { Tags(p.skills) }
                    if (p.isMentor && p.mentorshipAreas.isNotEmpty()) Section(stringResource(R.string.alumni_mentors_in)) { Tags(p.mentorshipAreas) }
                    if (p.willingToContribute && p.contributionInterests.isNotEmpty()) Section(stringResource(R.string.alumni_contributes)) { Tags(p.contributionInterests) }
                    if (p.isMentor && !state.isSelf) {
                        KrPrimaryButton(
                            stringResource(R.string.action_request_mentorship), onClick = { onRequestMentorship(p.id, p.fullName) },
                            icon = Icons.Outlined.Handshake, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    p.contactEmail?.takeIf { it.isNotBlank() }?.let { email ->
                        KrOutlinedButton(
                            stringResource(R.string.action_email), onClick = {
                                try { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))) } catch (_: ActivityNotFoundException) {}
                            }, icon = Icons.Outlined.Email, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun Tags(tags: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { tags.forEach { CategoryPill(it) } }
}
