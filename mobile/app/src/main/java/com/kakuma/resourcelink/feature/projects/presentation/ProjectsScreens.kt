package com.kakuma.resourcelink.feature.projects.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.UiText
import com.kakuma.resourcelink.core.common.toUiText
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.CategoryPill
import com.kakuma.resourcelink.core.designsystem.component.CoverImage
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.FullScreenLoading
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrFilterChips
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTextField
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.ui.UiEventChannel
import com.kakuma.resourcelink.core.ui.UiEventEffect
import com.kakuma.resourcelink.feature.auth.domain.AuthRepository
import com.kakuma.resourcelink.feature.opportunities.presentation.toPagingMessage
import com.kakuma.resourcelink.feature.projects.domain.CommunityProject
import com.kakuma.resourcelink.feature.projects.domain.Contribution
import com.kakuma.resourcelink.feature.projects.domain.ContributionType
import com.kakuma.resourcelink.feature.projects.domain.PledgeRules
import com.kakuma.resourcelink.feature.projects.domain.PledgeValidation
import com.kakuma.resourcelink.feature.projects.domain.ProjectCategory
import com.kakuma.resourcelink.feature.projects.domain.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject

@Composable
fun ProjectCategory.label(): String = stringResource(
    when (this) {
        ProjectCategory.EDUCATION -> R.string.project_cat_education
        ProjectCategory.HEALTH -> R.string.project_cat_health
        ProjectCategory.LIVELIHOOD -> R.string.project_cat_livelihood
        ProjectCategory.TECHNOLOGY -> R.string.project_cat_technology
        ProjectCategory.ENVIRONMENT -> R.string.project_cat_environment
        ProjectCategory.YOUTH -> R.string.project_cat_youth
        ProjectCategory.OTHER -> R.string.project_cat_other
    },
)

@Composable
fun ContributionType.label(): String = stringResource(
    when (this) {
        ContributionType.MENTORING -> R.string.contribution_mentoring
        ContributionType.VOLUNTEERING -> R.string.contribution_volunteering
        ContributionType.EQUIPMENT -> R.string.contribution_equipment
        ContributionType.SKILLS -> R.string.contribution_skills
        ContributionType.FINANCIAL_PLEDGE -> R.string.contribution_financial
    },
)

fun formatMoney(amount: BigDecimal, currency: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { this.currency = Currency.getInstance(currency); maximumFractionDigits = 0 }.format(amount)
}.getOrElse { "$currency ${amount.toPlainString()}" }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectsViewModel @Inject constructor(repository: ProjectRepository, private val savedState: SavedStateHandle) : ViewModel() {
    val category = savedState.getStateFlow<String?>("cat", null)
    val projects = category.flatMapLatest { repository.projects(it?.let(ProjectCategory::from)) }.cachedIn(viewModelScope)
    fun select(c: ProjectCategory?) { savedState["cat"] = c?.apiValue }
}

@Composable
fun ProjectsRoute(onBack: (() -> Unit)?, onOpen: (String) -> Unit, viewModel: ProjectsViewModel = hiltViewModel()) {
    val category by viewModel.category.collectAsStateWithLifecycle()
    val items = viewModel.projects.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        KrTopBar(stringResource(R.string.projects_title), onBack = onBack)
        Text(
            stringResource(R.string.projects_subtitle), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.gutter),
        )
        val options = listOf<Pair<ProjectCategory?, String>>(null to stringResource(R.string.filter_all)) + ProjectCategory.entries.map { it to it.label() }
        KrFilterChips(options, category?.let(ProjectCategory::from), viewModel::select, Modifier.padding(vertical = Spacing.xs))
        val refresh = items.loadState.refresh
        when {
            refresh is LoadState.Loading && items.itemCount == 0 -> SkeletonCardList()
            refresh is LoadState.Error && items.itemCount == 0 -> ErrorState(refresh.error.toPagingMessage().asString(), { items.retry() })
            refresh is LoadState.NotLoading && items.itemCount == 0 ->
                EmptyState(Icons.Outlined.Groups, stringResource(R.string.projects_empty_title), stringResource(R.string.projects_empty_body))
            else -> LazyColumn(contentPadding = PaddingValues(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(items.itemCount, key = items.itemKey { it.id }) { i -> items[i]?.let { ProjectCard(it) { onOpen(it.id) } } }
                if (items.loadState.append is LoadState.Loading) item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: CommunityProject, onClick: () -> Unit) {
    KrCard(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        CoverImage(project.coverImage, Modifier.fillMaxWidth().height(120.dp).clip(MaterialTheme.shapes.large), Icons.Outlined.Groups)
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CategoryPill(project.category.label())
            Text(project.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(project.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ProgressBlock(project)
        }
    }
}

@Composable
private fun ProgressBlock(project: CommunityProject) {
    project.pledgeProgress?.let { progress ->
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small), color = MaterialTheme.colorScheme.secondary)
        Text(
            stringResource(
                R.string.project_pledged_of_goal,
                formatMoney(project.summaryStats.pledgedAmount, project.currency),
                formatMoney(project.fundingGoal ?: BigDecimal.ZERO, project.currency),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Text(
        androidx.compose.ui.res.pluralStringResource(R.plurals.project_supporters, project.summaryStats.totalContributions, project.summaryStats.totalContributions),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

data class ProjectDetailUiState(
    val project: CommunityProject? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val mine: List<Contribution> = emptyList(),
    val isLoggedIn: Boolean = false,
    val showPledge: Boolean = false,
    val pledgeType: ContributionType? = null,
    val pledgeDescription: String = "",
    val pledgeAmount: String = "",
    val pledgeErrors: Map<String, UiText> = emptyMap(),
    val isPledging: Boolean = false,
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: ProjectRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedState["id"])
    private val _state = MutableStateFlow(ProjectDetailUiState(isLoggedIn = authRepository.isLoggedIn.value))
    val state = _state.asStateFlow()
    private val uiEvents = UiEventChannel()
    val events = uiEvents.events

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = repository.get(id)) {
                is AppResult.Success -> _state.update { it.copy(project = r.data, isLoading = false) }
                is AppResult.Failure -> _state.update { it.copy(isLoading = false, error = r.error.toUiText()) }
            }
            if (authRepository.isLoggedIn.value) {
                (repository.myContributions(id) as? AppResult.Success)?.let { r -> _state.update { it.copy(mine = r.data) } }
            }
        }
    }

    fun openPledge() = _state.update { s -> s.copy(showPledge = true, pledgeType = s.pledgeType ?: s.project?.acceptedTypes?.firstOrNull()) }
    fun closePledge() = _state.update { it.copy(showPledge = false, pledgeErrors = emptyMap()) }
    fun onType(t: ContributionType) = _state.update { it.copy(pledgeType = t, pledgeErrors = emptyMap()) }
    fun onDescription(v: String) = _state.update { it.copy(pledgeDescription = v.take(1000), pledgeErrors = it.pledgeErrors - "description") }
    fun onAmount(v: String) = _state.update { it.copy(pledgeAmount = v.filter { c -> c.isDigit() || c == '.' }.take(12), pledgeErrors = it.pledgeErrors - "amount") }

    fun submitPledge() {
        val s = _state.value
        val type = s.pledgeType ?: return
        when (PledgeRules.validate(type, s.pledgeDescription, s.pledgeAmount)) {
            PledgeValidation.DescriptionTooShort -> { _state.update { it.copy(pledgeErrors = mapOf("description" to UiText.Res(R.string.validation_required))) }; return }
            PledgeValidation.AmountRequired -> { _state.update { it.copy(pledgeErrors = mapOf("amount" to UiText.Res(R.string.validation_amount))) }; return }
            PledgeValidation.Ok -> Unit
        }
        _state.update { it.copy(isPledging = true) }
        viewModelScope.launch {
            val amount = if (type == ContributionType.FINANCIAL_PLEDGE) s.pledgeAmount.toBigDecimalOrNull() else null
            when (val r = repository.pledge(id, type, s.pledgeDescription, amount)) {
                is AppResult.Success -> {
                    uiEvents.toast(UiText.Res(R.string.toast_pledge_recorded))
                    _state.update { it.copy(isPledging = false, showPledge = false, pledgeDescription = "", pledgeAmount = "", mine = it.mine + r.data) }
                    load()
                }
                is AppResult.Failure -> {
                    _state.update { it.copy(isPledging = false) }
                    val e = r.error
                    if (e is AppError.Validation && e.fieldErrors.isNotEmpty()) _state.update { it.copy(pledgeErrors = e.fieldErrors.mapValues { f -> UiText.Raw(f.value) }) }
                    else uiEvents.snackbar(e.toUiText())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailRoute(onBack: () -> Unit, onRequireLogin: () -> Unit, viewModel: ProjectDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UiEventEffect(viewModel.events)
    Scaffold(topBar = { KrTopBar(stringResource(R.string.project_detail_title), onBack = onBack) }) { padding ->
        val p = state.project
        when {
            p == null && state.isLoading -> FullScreenLoading(Modifier.padding(padding))
            p == null -> ErrorState(state.error?.asString().orEmpty(), viewModel::load, Modifier.padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CoverImage(p.coverImage, Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.large), Icons.Outlined.Groups)
                CategoryPill(p.category.label())
                Text(p.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                if (p.location.isNotBlank()) Text(p.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.project_by, p.owner.name), style = MaterialTheme.typography.bodySmall)
                ProgressBlock(p)
                Text(p.summary, style = MaterialTheme.typography.titleSmall)
                Text(p.description, style = MaterialTheme.typography.bodyMedium)
                if (p.acceptedTypes.isNotEmpty()) {
                    Text(stringResource(R.string.project_ways_to_help), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        p.acceptedTypes.forEach { CategoryPill(it.label()) }
                    }
                }
                if (state.mine.isNotEmpty()) {
                    Text(stringResource(R.string.project_your_pledges), style = MaterialTheme.typography.titleMedium)
                    state.mine.forEach { c ->
                        Text(
                            "• ${c.type.label()}: ${c.description}" + (c.amount?.let { " (${formatMoney(it, c.currency.ifBlank { p.currency })})" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                // Pledges are commitments; no money moves through the app.
                Text(p.summaryStats.note.ifBlank { stringResource(R.string.project_pledge_note) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (p.status == "active" && p.acceptedTypes.isNotEmpty()) {
                    KrPrimaryButton(
                        stringResource(R.string.action_pledge_support),
                        onClick = { if (state.isLoggedIn) viewModel.openPledge() else onRequireLogin() },
                        icon = Icons.Outlined.VolunteerActivism, modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
    if (state.showPledge && state.project != null) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = viewModel::closePledge, sheetState = sheet) {
            Column(
                Modifier.padding(horizontal = Spacing.gutter).imePadding().navigationBarsPadding().padding(bottom = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(stringResource(R.string.pledge_title), style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.project!!.acceptedTypes.forEach { t ->
                        FilterChip(selected = state.pledgeType == t, onClick = { viewModel.onType(t) }, label = { Text(t.label()) })
                    }
                }
                KrTextField(
                    state.pledgeDescription, viewModel::onDescription, stringResource(R.string.field_pledge_description),
                    placeholder = stringResource(R.string.placeholder_pledge), error = state.pledgeErrors["description"]?.asString(),
                    singleLine = false, minLines = 3,
                )
                if (state.pledgeType == ContributionType.FINANCIAL_PLEDGE) {
                    KrTextField(
                        state.pledgeAmount, viewModel::onAmount, stringResource(R.string.field_amount, state.project!!.currency),
                        keyboardType = KeyboardType.Decimal, error = state.pledgeErrors["amount"]?.asString(),
                    )
                }
                Text(stringResource(R.string.project_pledge_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                KrPrimaryButton(stringResource(R.string.action_confirm_pledge), viewModel::submitPledge, Modifier.fillMaxWidth(), loading = state.isPledging)
                Row { Spacer(Modifier.height(4.dp)) }
            }
        }
    }
}
