package com.kakuma.resourcelink.feature.stories.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.BadgeTone
import com.kakuma.resourcelink.core.designsystem.component.CategoryPill
import com.kakuma.resourcelink.core.designsystem.component.CoverImage
import com.kakuma.resourcelink.core.designsystem.component.FullCoverImage
import com.kakuma.resourcelink.core.designsystem.component.InitialAvatar
import com.kakuma.resourcelink.core.designsystem.component.KrCard
import com.kakuma.resourcelink.core.designsystem.component.StatusBadge
import com.kakuma.resourcelink.feature.stories.domain.Story
import com.kakuma.resourcelink.feature.stories.domain.StoryCategory
import com.kakuma.resourcelink.feature.stories.domain.StoryStatus

@Composable
fun StoryCategory.label(): String = stringResource(
    when (this) {
        StoryCategory.EDUCATION -> R.string.story_cat_education
        StoryCategory.CAREER -> R.string.story_cat_career
        StoryCategory.RESETTLEMENT -> R.string.story_cat_resettlement
        StoryCategory.ENTREPRENEURSHIP -> R.string.story_cat_entrepreneurship
        StoryCategory.INSPIRATION -> R.string.story_cat_inspiration
        StoryCategory.ADVICE -> R.string.story_cat_advice
        StoryCategory.COMMUNITY -> R.string.story_cat_community
    },
)

@Composable
fun StoryStatusBadge(status: StoryStatus) {
    val (label, tone) = when (status) {
        StoryStatus.DRAFT -> R.string.story_status_draft to BadgeTone.Neutral
        StoryStatus.PENDING_REVIEW -> R.string.story_status_pending to BadgeTone.Warning
        StoryStatus.PUBLISHED -> R.string.story_status_published to BadgeTone.Success
        StoryStatus.REJECTED -> R.string.story_status_rejected to BadgeTone.Error
        StoryStatus.ARCHIVED -> R.string.story_status_archived to BadgeTone.Neutral
    }
    StatusBadge(stringResource(label), tone)
}

@Composable
fun StoryCard(
    story: Story,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    showStatus: Boolean = false,
) {
    KrCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        // The whole photo as uploaded (no cropping); stories without a photo keep the short banner.
        val cover = story.coverImage
        if (!cover.isNullOrBlank()) FullCoverImage(cover, maxHeight = 360.dp)
        else CoverImage(null, Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.large), icon = Icons.Outlined.AutoStories)
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryPill(story.category.label())
                Spacer(Modifier.width(8.dp))
                Text(
                    pluralStringResource(R.plurals.reading_minutes, story.readingTime, story.readingTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (showStatus) StoryStatusBadge(story.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (story.excerpt.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    story.excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialAvatar(story.author.name, story.author.avatarUrl, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(story.author.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    story.author.country?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (story.status == StoryStatus.PUBLISHED) {
                    StoryActions(story, onToggleLike, onToggleBookmark)
                }
            }
        }
    }
}

@Composable
fun StoryActions(story: Story, onToggleLike: () -> Unit, onToggleBookmark: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleLike) {
            Icon(
                if (story.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(if (story.isLiked) R.string.cd_unlike else R.string.cd_like),
                tint = if (story.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(story.likesCount.toString(), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(
            story.commentsCount.toString(),
            style = MaterialTheme.typography.labelMedium,
        )
        IconButton(onClick = onToggleBookmark) {
            Icon(
                if (story.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(if (story.isBookmarked) R.string.cd_remove_bookmark else R.string.cd_bookmark),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
