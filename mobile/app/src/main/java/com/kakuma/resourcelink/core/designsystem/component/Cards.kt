package com.kakuma.resourcelink.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kakuma.resourcelink.core.designsystem.KrColors
import com.kakuma.resourcelink.core.designsystem.Spacing

/** Rounded surface card with hairline border, the base of every list item. */
@Composable
fun KrCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) { Column(Modifier.padding(contentPadding), content = content) }
}

@Composable
fun CategoryPill(text: String, modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
    )
}

enum class BadgeTone { Neutral, Warning, Success, Error, Info }

@Composable
fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        BadgeTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        BadgeTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        BadgeTone.Success -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        BadgeTone.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f) to MaterialTheme.colorScheme.error
        BadgeTone.Info -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f) to MaterialTheme.colorScheme.secondary
    }
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
    )
}

/** Horizontal chip row ("All · Education · Jobs …"). Selected chip uses the primary fill from the design. */
@Composable
fun <T> KrFilterChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.gutter),
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding,
    ) {
        items(options, key = { it.first.toString() }) { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        action?.invoke()
    }
}

/**
 * Hero banner from the Home design ("Build a better future together"). With a [photo] the text sits on a
 * left-side scrim over the picture; without one it falls back to the brand gradient and optional [icon].
 */
@Composable
fun HeroBanner(
    eyebrow: String,
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    photo: BrandPhoto? = null,
) {
    val showIcon = icon != null && photo == null
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .then(
                if (photo == null) Modifier.background(Brush.linearGradient(listOf(KrColors.Primary, KrColors.Accent)))
                else Modifier.heightIn(min = 184.dp),
            ),
    ) {
        if (photo != null) {
            PhotoBackground(
                photo,
                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.1f))),
            )
            PhotoCredit(photo.credit, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        Column(Modifier.padding(Spacing.lg).fillMaxWidth(if (showIcon) 0.72f else if (photo != null) 0.78f else 1f)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = KrColors.Gold)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(Spacing.sm))
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(50),
                color = Color.White,
                modifier = Modifier.height(40.dp),
            ) {
                Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = KrColors.Primary)
                }
            }
        }
        if (showIcon) {
            Icon(
                icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = Spacing.lg).size(72.dp),
            )
        }
    }
}

@Composable
fun QuickAccessTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = KrColors.Primary) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tint) }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Avatar image, or the person's initial in a tinted circle (design: "D", "G", "P"). */
@Composable
fun InitialAvatar(name: String, imageUrl: String?, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Cover image with a branded gradient placeholder when there is no image (or while offline). */
@Composable
fun CoverImage(url: String?, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Box(
        modifier.background(Brush.linearGradient(listOf(KrColors.Primary.copy(alpha = 0.85f), KrColors.Accent))),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint) }
}

@Composable
fun HorizontalSpace(width: Dp) = Spacer(Modifier.width(width))
