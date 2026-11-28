package com.kakuma.resourcelink.feature.opportunities.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.kakuma.resourcelink.core.designsystem.component.CategoryPill
import com.kakuma.resourcelink.core.designsystem.component.ErrorState
import com.kakuma.resourcelink.core.designsystem.component.FullScreenLoading
import com.kakuma.resourcelink.core.designsystem.component.IconTile
import com.kakuma.resourcelink.core.designsystem.component.KrOutlinedButton
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton
import com.kakuma.resourcelink.core.designsystem.component.KrTopBar
import com.kakuma.resourcelink.core.designsystem.component.StatusBadge
import com.kakuma.resourcelink.core.ui.UiEventEffect
import androidx.core.net.toUri

@Composable
fun OpportunityDetailRoute(
    onBack: () -> Unit,
    onAskAssistant: (String) -> Unit,
    viewModel: OpportunityDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    UiEventEffect(viewModel.events)
    val opp = state.opportunity
    Scaffold(topBar = {
        KrTopBar(stringResource(R.string.opp_detail_title), onBack = onBack, actions = {
            if (opp != null) {
                IconButton(onClick = viewModel::onToggleBookmark) {
                    Icon(
                        if (opp.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(if (opp.isBookmarked) R.string.cd_remove_bookmark else R.string.cd_bookmark),
                    )
                }
            }
        })
    }) { padding ->
        when {
            opp == null && state.isLoading -> FullScreenLoading(Modifier.padding(padding))
            opp == null -> ErrorState(state.error?.asString() ?: "", viewModel::refresh, Modifier.padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState)
                    .padding(horizontal = Spacing.gutter).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(opp.category.icon())
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(opp.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                        Text(opp.organization, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPill(opp.category.label())
                    if (opp.isVerified) StatusBadge(stringResource(R.string.badge_verified), BadgeTone.Success)
                    else StatusBadge(stringResource(R.string.badge_unverified), BadgeTone.Warning)
                }
                if (!opp.isVerified) {
                    Text(stringResource(R.string.opp_unverified_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                opp.deadline?.let { InfoRow(Icons.Outlined.Event, stringResource(R.string.opp_deadline, it.formatted())) }
                val place = listOfNotNull(opp.location.ifBlank { null }, if (opp.isRemote) stringResource(R.string.opp_remote) else null).joinToString(" · ")
                if (place.isNotBlank()) InfoRow(Icons.Outlined.Place, place)
                Text(opp.summary, style = MaterialTheme.typography.titleSmall)
                Text(opp.description, style = MaterialTheme.typography.bodyMedium)
                if (opp.requirements.isNotEmpty()) {
                    Text(stringResource(R.string.opp_requirements), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    opp.requirements.forEach { req ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(req, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                if (opp.applicationUrl.isNotBlank()) {
                    KrPrimaryButton(
                        stringResource(R.string.action_apply_now),
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    opp.applicationUrl.toUri()))
                            } catch (_: ActivityNotFoundException) { /* no browser installed */ }
                        },
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                KrOutlinedButton(
                    stringResource(R.string.action_explain_with_ai),
                    onClick = { onAskAssistant(opp.id) },
                    icon = Icons.Outlined.AutoAwesome,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.ai_requires_internet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
