package com.kakuma.resourcelink.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.KrColors
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.IconTile
import com.kakuma.resourcelink.core.designsystem.component.KrCard

/** "Community" tab: diaspora, mentorship, projects and services in one hub. */
@Composable
fun CommunityRoute(onAlumni: () -> Unit, onMentorship: () -> Unit, onProjects: () -> Unit, onServices: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(stringResource(R.string.community_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.community_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HubCard(Icons.Outlined.Public, R.string.nav_alumni, R.string.community_alumni_body, onAlumni)
        HubCard(Icons.Outlined.Handshake, R.string.nav_mentorship, R.string.community_mentorship_body, onMentorship)
        HubCard(Icons.Outlined.Groups, R.string.nav_projects, R.string.community_projects_body, onProjects, KrColors.Gold)
        HubCard(Icons.Outlined.LocalHospital, R.string.nav_services, R.string.community_services_body, onServices)
    }
}

@Composable
private fun HubCard(icon: ImageVector, title: Int, body: Int, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color = KrColors.Primary) {
    KrCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, tint = tint)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
