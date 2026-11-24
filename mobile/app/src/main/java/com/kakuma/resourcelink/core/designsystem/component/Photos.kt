package com.kakuma.resourcelink.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** A background photo bundled with the app, with the photographer to credit. */
data class BrandPhoto(@param:DrawableRes val res: Int, val credit: String, val alignment: Alignment = Alignment.Center)

/**
 * Fills its Box with a cropped photo and a scrim that keeps white text readable whatever the picture.
 * Decorative, so screen readers skip it. Call it first inside the Box so the content draws on top.
 */
@Composable
fun BoxScope.PhotoBackground(photo: BrandPhoto, scrim: Brush) {
    Image(
        painterResource(photo.res),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = photo.alignment,
        modifier = Modifier.matchParentSize(),
    )
    androidx.compose.foundation.layout.Box(Modifier.matchParentSize().background(scrim))
}

/** Small photographer credit shown on top of a photo. */
@Composable
fun PhotoCredit(credit: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
        Text(credit, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}

/** The photos shipped with the app. Replace the drawables (same names) to swap them. */
object BrandPhotos {
    val CommunityMeeting = BrandPhoto(
        com.kakuma.resourcelink.R.drawable.photo_community_meeting,
        credit = "Random Institute / Unsplash",
        alignment = androidx.compose.ui.BiasAlignment(0f, -0.3f),
    )
    val Graduates = BrandPhoto(
        com.kakuma.resourcelink.R.drawable.photo_graduates,
        credit = "RUT MIIT / Unsplash",
        alignment = androidx.compose.ui.BiasAlignment(0.3f, -0.1f),
    )
}
