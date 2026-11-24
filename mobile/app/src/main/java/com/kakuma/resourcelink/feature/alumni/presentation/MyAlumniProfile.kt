package com.kakuma.resourcelink.feature.alumni.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.ConfirmDialog
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.FullScreenLoading
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.alumni.domain.AlumniRepository
import com.kakuma.resourcelink.feature.alumni.domain.MyAlumniProfile
import com.kakuma.resourcelink.feature.alumni.domain.ProfileVisibility
import com.kakuma.resourcelink.feature.alumni.domain.parseTags
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyAlumniUiState(
    val profile: MyAlumniProfile = MyAlumniProfile(),
    /** Tag fields are edited as comma-separated text. */
    val skillsText: String = "",
    val areasText: String = "",
    val interestsText: String = "",
    val exists: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val isSaving: Boolean = false,
    val errors: Map<String, UiText> = emptyMap(),
)

@HiltViewModel
class MyAlumniProfileViewModel @Inject constructor(private val repository: AlumniRepository) : ViewModel() {
    private val _state = MutableStateFlow(MyAlumniUiState())
    val state = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            when (val r = repository.myProfile()) {
                is AppResult.Success -> {
                    val p = r.data ?: MyAlumniProfile()
                    _state.value = MyAlumniUiState(
                        p, p.skills.joinToString(", "), p.mentorshipAreas.joinToString(", "), p.contributionInterests.joinToString(", "),
                        exists = r.data != null, isLoading = false,
                    )
                }
                is AppResult.Failure -> _state.update { it.copy(isLoading = false, loadError = r.error.toUiText()) }
            }
        }
    }

    fun update(transform: (MyAlumniProfile) -> MyAlumniProfile) = _state.update { it.copy(profile = transform(it.profile), errors = emptyMap()) }
    fun onSkills(v: String) = _state.update { it.copy(skillsText = v) }
    fun onAreas(v: String) = _state.update { it.copy(areasText = v) }
    fun onInterests(v: String) = _state.update { it.copy(interestsText = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val errors = buildMap<String, UiText> {
            if (s.profile.country.isBlank()) put("country", UiText.Res(R.string.validation_required))
            if (!s.exists && !s.profile.consent) put("consent", UiText.Res(R.string.validation_alumni_consent))
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(errors = errors) }; return }
        val profile = s.profile.copy(
            skills = parseTags(s.skillsText),
            mentorshipAreas = if (s.profile.isMentor) parseTags(s.areasText) else emptyList(),
            contributionInterests = if (s.profile.willingToContribute) parseTags(s.interestsText) else emptyList(),
        )
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val r = repository.saveMyProfile(profile, isNew = !s.exists)) {
                is AppResult.Success -> {
                    uiEvents.toast(UiText.Res(R.string.toast_profile_updated))
                    _state.update { it.copy(isSaving = false, exists = true, profile = r.data) }
                    onDone()
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSaving = false) }
                    val e = r.error
                    if (e is com.kakuma.resourcelink.core.common.AppError.Validation && e.fieldErrors.isNotEmpty()) {
                        _state.update { it.copy(errors = e.fieldErrors.mapValues { f -> UiText.Raw(f.value) }) }
                    } else uiEvents.snackbar(e.toUiText())
                }
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            when (val r = repository.deleteMyProfile()) {
                is AppResult.Success -> { uiEvents.toast(UiText.Res(R.string.toast_alumni_removed)); onDone() }
                is AppResult.Failure -> uiEvents.snackbar(r.error.toUiText())
            }
        }
    }
}

@Composable
fun MyAlumniProfileRoute(onBack: () -> Unit, viewModel: MyAlumniProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    UiEventEffect(viewModel.events)
    Scaffold(topBar = {
        KrTopBar(stringResource(R.string.alumni_my_profile), onBack = onBack, actions = {
            if (state.exists) IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_remove_alumni))
            }
        })
    }) { padding ->
        when {
            state.isLoading -> FullScreenLoading(Modifier.padding(padding))
            state.loadError != null -> ErrorState(state.loadError!!.asString(), viewModel::load, Modifier.padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                val p = state.profile
                Text(stringResource(R.string.alumni_editor_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                KrTextField(p.country, { v -> viewModel.update { it.copy(country = v) } }, stringResource(R.string.field_country), error = state.errors["country"]?.asString())
                KrTextField(p.city, { v -> viewModel.update { it.copy(city = v) } }, stringResource(R.string.field_city))
                KrTextField(p.profession, { v -> viewModel.update { it.copy(profession = v) } }, stringResource(R.string.field_profession))
                KrTextField(p.organization, { v -> viewModel.update { it.copy(organization = v) } }, stringResource(R.string.field_organization))
                KrTextField(state.skillsText, viewModel::onSkills, stringResource(R.string.field_skills), placeholder = stringResource(R.string.placeholder_tags))
                KrTextField(p.bio, { v -> viewModel.update { it.copy(bio = v) } }, stringResource(R.string.field_bio), singleLine = false, minLines = 3, maxLength = 1000)
                SwitchRow(stringResource(R.string.alumni_is_mentor), p.isMentor) { v -> viewModel.update { it.copy(isMentor = v) } }
                if (p.isMentor) KrTextField(state.areasText, viewModel::onAreas, stringResource(R.string.field_mentorship_areas), placeholder = stringResource(R.string.placeholder_tags))
                SwitchRow(stringResource(R.string.alumni_willing_contribute), p.willingToContribute) { v -> viewModel.update { it.copy(willingToContribute = v) } }
                if (p.willingToContribute) KrTextField(state.interestsText, viewModel::onInterests, stringResource(R.string.field_contribution_interests), placeholder = stringResource(R.string.placeholder_tags))

                Text(stringResource(R.string.alumni_privacy), style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    ProfileVisibility.entries.forEach { v ->
                        Row(
                            Modifier.fillMaxWidth().selectable(p.visibility == v, role = Role.RadioButton) { viewModel.update { it.copy(visibility = v) } }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = p.visibility == v, onClick = null)
                            Text(
                                stringResource(
                                    when (v) {
                                        ProfileVisibility.PUBLIC -> R.string.visibility_public
                                        ProfileVisibility.MEMBERS -> R.string.visibility_members
                                        ProfileVisibility.PRIVATE -> R.string.visibility_private
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                SwitchRow(stringResource(R.string.alumni_show_city), p.showCity) { v -> viewModel.update { it.copy(showCity = v) } }
                SwitchRow(stringResource(R.string.alumni_show_email), p.showContactEmail) { v -> viewModel.update { it.copy(showContactEmail = v) } }
                if (!state.exists) {
                    Row(Modifier.toggleable(p.consent, role = Role.Checkbox) { v -> viewModel.update { it.copy(consent = v) } }, verticalAlignment = Alignment.Top) {
                        Checkbox(p.consent, onCheckedChange = null)
                        Text(stringResource(R.string.alumni_consent), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                    }
                    state.errors["consent"]?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
                KrPrimaryButton(
                    stringResource(if (state.exists) R.string.action_save_changes else R.string.action_create_alumni),
                    onClick = { viewModel.save(onBack) }, loading = state.isSaving, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
    if (confirmDelete) ConfirmDialog(
        stringResource(R.string.dialog_remove_alumni_title), stringResource(R.string.dialog_remove_alumni_body),
        stringResource(R.string.action_remove_alumni),
        onConfirm = { confirmDelete = false; viewModel.delete(onBack) }, onDismiss = { confirmDelete = false },
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
