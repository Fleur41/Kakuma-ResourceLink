package com.kakuma.resourcelink.feature.opportunities.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.KrColors
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.CategoryPill
import com.kakuma.resourcelink.core.designsystem.component.IconTile
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.feature.opportunities.domain.Opportunity
import com.kakuma.resourcelink.feature.opportunities.domain.OpportunityCategory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun OpportunityCategory.icon(): ImageVector = when (this) {
    OpportunityCategory.SCHOLARSHIP -> Icons.Outlined.EmojiEvents
    OpportunityCategory.EDUCATION -> Icons.Outlined.School
    OpportunityCategory.VOCATIONAL -> Icons.Outlined.Construction
    OpportunityCategory.JOB -> Icons.Outlined.Work
    OpportunityCategory.INTERNSHIP -> Icons.Outlined.BusinessCenter
    OpportunityCategory.ENTREPRENEURSHIP -> Icons.Outlined.Lightbulb
}

@Composable
fun OpportunityCategory.label(): String = stringResource(
    when (this) {
        OpportunityCategory.SCHOLARSHIP -> R.string.opp_cat_scholarship
        OpportunityCategory.EDUCATION -> R.string.opp_cat_education
        OpportunityCategory.VOCATIONAL -> R.string.opp_cat_vocational
        OpportunityCategory.JOB -> R.string.opp_cat_job
        OpportunityCategory.INTERNSHIP -> R.string.opp_cat_internship
        OpportunityCategory.ENTREPRENEURSHIP -> R.string.opp_cat_entrepreneurship
    },
)

fun LocalDate.formatted(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@Composable
fun OpportunityCard(
    opportunity: Opportunity,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KrCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(opportunity.category.icon(), tint = if (opportunity.category == OpportunityCategory.SCHOLARSHIP) KrColors.Gold else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(opportunity.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    opportunity.organization,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                opportunity.deadline?.let {
                    Text(
                        stringResource(R.string.opp_deadline, it.formatted()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                CategoryPill(opportunity.category.label())
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    if (opportunity.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(if (opportunity.isBookmarked) R.string.cd_remove_bookmark else R.string.cd_bookmark),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun BookmarkHint(modifier: Modifier = Modifier) {
    Text(stringResource(R.string.opp_bookmark_hint), style = MaterialTheme.typography.bodySmall, modifier = modifier.padding(4.dp))
}
