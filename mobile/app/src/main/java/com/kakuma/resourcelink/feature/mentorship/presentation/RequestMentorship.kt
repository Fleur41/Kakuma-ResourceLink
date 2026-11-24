package com.kakuma.resourcelink.feature.mentorship.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.mentorship.domain.HelpArea
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipRepository
import com.kakuma.resourcelink.feature.mentorship.domain.MentorshipRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun HelpArea.label(): String = stringResource(
    when (this) {
        HelpArea.CAREER -> R.string.help_career
        HelpArea.TECHNICAL -> R.string.help_technical
        HelpArea.SCHOLARSHIPS -> R.string.help_scholarships
        HelpArea.ENTREPRENEURSHIP -> R.string.help_entrepreneurship
        HelpArea.OTHER -> R.string.help_other
    },
)

data class RequestMentorshipUiState(
    val mentorName: String = "",
    val goal: String = "",
    val areas: Set<HelpArea> = emptySet(),
    val message: String = "",
    val errors: Map<String, UiText> = emptyMap(),
    val isSending: Boolean = false,
)

@HiltViewModel
class RequestMentorshipViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val repository: MentorshipRepository,
) : ViewModel() {
    private val mentorId: String = checkNotNull(savedState["mentorId"])
    private val _state = MutableStateFlow(
        RequestMentorshipUiState(
            mentorName = savedState["name"] ?: "",
            goal = savedState["goal"] ?: "",
            message = savedState["message"] ?: "",
            areas = savedState.get<ArrayList<String>>("areas")?.map(HelpArea::from)?.toSet() ?: emptySet(),
        ),
    )
    val state = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    fun onGoal(v: String) { savedState["goal"] = v; _state.update { it.copy(goal = v.take(200), errors = it.errors - "goal") } }
    fun onMessage(v: String) { savedState["message"] = v; _state.update { it.copy(message = v.take(2000), errors = it.errors - "message") } }
    fun toggleArea(a: HelpArea) = _state.update {
        val next = if (a in it.areas) it.areas - a else it.areas + a
        savedState["areas"] = ArrayList(next.map { x -> x.apiValue })
        it.copy(areas = next, errors = it.errors - "help_areas")
    }

    fun send(onDone: () -> Unit) {
        val s = _state.value
        if (s.isSending) return
        val errors = buildMap<String, UiText> {
            if (s.goal.trim().length < MentorshipRules.MIN_GOAL) put("goal", UiText.Res(R.string.validation_required))
            if (s.areas.isEmpty()) put("help_areas", UiText.Res(R.string.validation_help_area))
            if (s.message.trim().length < MentorshipRules.MIN_MESSAGE) put("message", UiText.Res(R.string.validation_message_length, listOf(MentorshipRules.MIN_MESSAGE)))
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(errors = errors) }; return }
        _state.update { it.copy(isSending = true) }
        viewModelScope.launch {
            when (val r = repository.send(mentorId, s.goal, s.areas, s.message)) {
                is AppResult.Success -> { uiEvents.toast(UiText.Res(R.string.toast_mentorship_sent)); onDone() }
                is AppResult.Failure -> {
                    _state.update { it.copy(isSending = false) }
                    val e = r.error
                    if (e is AppError.Validation && e.fieldErrors.isNotEmpty()) _state.update { it.copy(errors = e.fieldErrors.mapValues { f -> UiText.Raw(f.value) }) }
                    else uiEvents.snackbar(e.toUiText())
                }
            }
        }
    }
}

@Composable
fun RequestMentorshipRoute(onBack: () -> Unit, viewModel: RequestMentorshipViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UiEventEffect(viewModel.events)
    Scaffold(topBar = { KrTopBar(stringResource(R.string.mentorship_request_title), onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.mentorship_request_to, state.mentorName), style = MaterialTheme.typography.titleMedium)
            KrTextField(
                state.goal, viewModel::onGoal, stringResource(R.string.field_goal),
                placeholder = stringResource(R.string.placeholder_goal), error = state.errors["goal"]?.asString(),
            )
            Column {
                Text(stringResource(R.string.field_help_areas), style = MaterialTheme.typography.labelLarge)
                FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HelpArea.entries.forEach { a ->
                        FilterChip(selected = a in state.areas, onClick = { viewModel.toggleArea(a) }, label = { Text(a.label()) })
                    }
                }
                state.errors["help_areas"]?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            KrTextField(
                state.message, viewModel::onMessage, stringResource(R.string.field_message),
                placeholder = stringResource(R.string.placeholder_mentorship_message), error = state.errors["message"]?.asString(),
                singleLine = false, minLines = 5, imeAction = ImeAction.Default,
            )
            Text(stringResource(R.string.mentorship_privacy_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            KrPrimaryButton(stringResource(R.string.action_send_request), { viewModel.send(onBack) }, Modifier.fillMaxWidth(), loading = state.isSending)
        }
    }
}
