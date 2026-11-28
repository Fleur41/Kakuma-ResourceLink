package com.kakuma.resourcelink.feature.services.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.BadgeTone
import com.kakuma.resourcelink.core.designsystem.component.EmptyState
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.IconTile
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.KrFilterChips
import com.kakuma.resourcelink.core.designsystem.component.KrSearchField
import com.kakuma.resourcelink.core.designsystem.component.OfflineBanner
import com.kakuma.resourcelink.core.designsystem.component.SkeletonCardList
import com.kakuma.resourcelink.core.designsystem.component.StatusBadge
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.feature.services.domain.EssentialService
import com.kakuma.resourcelink.feature.services.domain.ServiceCategory
import androidx.core.net.toUri

fun ServiceCategory.icon(): ImageVector = when (this) {
    ServiceCategory.HEALTHCARE -> Icons.Outlined.LocalHospital
    ServiceCategory.HUMANITARIAN -> Icons.Outlined.VolunteerActivism
    ServiceCategory.EDUCATION -> Icons.Outlined.School
    ServiceCategory.LEGAL -> Icons.Outlined.Gavel
    ServiceCategory.PROTECTION -> Icons.Outlined.Shield
    ServiceCategory.LIVELIHOOD -> Icons.Outlined.Storefront
    ServiceCategory.COMMUNITY -> Icons.Outlined.People
}

@Composable
fun ServiceCategory.label(): String = stringResource(
    when (this) {
        ServiceCategory.HEALTHCARE -> R.string.service_cat_healthcare
        ServiceCategory.HUMANITARIAN -> R.string.service_cat_humanitarian
        ServiceCategory.EDUCATION -> R.string.service_cat_education
        ServiceCategory.LEGAL -> R.string.service_cat_legal
        ServiceCategory.PROTECTION -> R.string.service_cat_protection
        ServiceCategory.LIVELIHOOD -> R.string.service_cat_livelihood
        ServiceCategory.COMMUNITY -> R.string.service_cat_community
    },
)

private fun Context.open(intent: Intent) = try { startActivity(intent) } catch (_: ActivityNotFoundException) { }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesRoute(onBack: (() -> Unit)?, viewModel: ServicesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        KrTopBar(stringResource(R.string.services_title), onBack = onBack)
        if (!state.isOnline) OfflineBanner()
        KrSearchField(state.query, viewModel::onQueryChange, stringResource(R.string.services_search_hint), Modifier.padding(horizontal = Spacing.gutter))
        val options = listOf<Pair<ServiceCategory?, String>>(null to stringResource(R.string.filter_all)) + ServiceCategory.entries.map { it to it.label() }
        KrFilterChips(options, state.category, viewModel::onCategoryChange, Modifier.padding(vertical = Spacing.xs))
        PullToRefreshBox(isRefreshing = state.isRefreshing && state.hasCache, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when {
                !state.hasCache && state.isRefreshing -> SkeletonCardList()
                !state.hasCache && state.error != null -> ErrorState(state.error!!.asString(), viewModel::refresh)
                state.services.isEmpty() -> EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.services_empty_title), stringResource(R.string.services_empty_body))
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item {
                        Text(stringResource(R.string.services_emergency_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(state.services, key = { it.id }) { ServiceCard(it) }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(service: EssentialService) {
    val context = LocalContext.current
    KrCard {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(service.category.icon())
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(service.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                if (service.organization.isNotBlank()) {
                    Text(service.organization, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (service.isVerified) StatusBadge(stringResource(R.string.badge_verified), BadgeTone.Success)
        }
        if (service.description.isNotBlank()) {
            Text(service.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.xs))
        }
        if (service.openingHours.isNotBlank()) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, Modifier.padding(end = 6.dp), tint = MaterialTheme.colorScheme.primary)
                Text(service.openingHours, style = MaterialTheme.typography.bodySmall)
            }
        }
        FlowRow(Modifier.padding(top = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (service.phone.isNotBlank()) AssistChip(
                onClick = { context.open(Intent(Intent.ACTION_DIAL, "tel:${service.phone}".toUri())) },
                label = { Text(stringResource(R.string.action_call)) },
                leadingIcon = { Icon(Icons.Outlined.Call, null) },
            )
            if (service.email.isNotBlank()) AssistChip(
                onClick = { context.open(Intent(Intent.ACTION_SENDTO,
                    "mailto:${service.email}".toUri())) },
                label = { Text(stringResource(R.string.action_email)) },
                leadingIcon = { Icon(Icons.Outlined.Email, null) },
            )
            if (service.website.isNotBlank()) AssistChip(
                onClick = { context.open(Intent(Intent.ACTION_VIEW, service.website.toUri())) },
                label = { Text(stringResource(R.string.action_website)) },
                leadingIcon = { Icon(Icons.Outlined.Language, null) },
            )
            if (service.address.isNotBlank()) AssistChip(
                onClick = { context.open(Intent(Intent.ACTION_VIEW,
                    ("geo:0,0?q=" + Uri.encode(service.address)).toUri())) },
                label = { Text(stringResource(R.string.action_map)) },
                leadingIcon = { Icon(Icons.Outlined.Map, null) },
            )
        }
    }
}

