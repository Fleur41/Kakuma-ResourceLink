package com.kakuma.resourcelink.feature.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kakuma.resourcelink.R
import com.kakuma.resourcelink.core.designsystem.KrColors
import com.kakuma.resourcelink.core.designsystem.Spacing
import com.kakuma.resourcelink.core.designsystem.component.BrandPhotos
import com.kakuma.resourcelink.core.designsystem.component.KrOutlinedButton
import com.kakuma.resourcelink.core.designsystem.component.PhotoBackground
import com.kakuma.resourcelink.core.designsystem.component.PhotoCredit
import com.kakuma.resourcelink.core.designsystem.component.KrPrimaryButton

private data class Slide(val icon: ImageVector, val title: Int, val body: Int)

private val slides = listOf(
    Slide(Icons.Outlined.School, R.string.onboarding_title_1, R.string.onboarding_body_1),
    Slide(Icons.AutoMirrored.Outlined.MenuBook, R.string.onboarding_title_2, R.string.onboarding_body_2),
    Slide(Icons.Outlined.Groups, R.string.onboarding_title_3, R.string.onboarding_body_3),
)

@Composable
fun BrandLockup(modifier: Modifier = Modifier, light: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.ic_logo_mark), contentDescription = null, modifier = Modifier.size(64.dp))
        Text("Kakuma", style = MaterialTheme.typography.headlineMedium, color = if (light) Color.White else KrColors.Primary)
        Text("ResourceLink", style = MaterialTheme.typography.headlineSmall, color = KrColors.Accent)
    }
}

/** White status-bar icons while this screen shows, since it starts with a dark photo; restored on leave. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onSignIn: () -> Unit, onBrowseAsGuest: () -> Unit) {
    val pager = rememberPagerState { slides.size }
    LightStatusBarIcons()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Photo header runs edge to edge under the status bar, with the logo on a bottom scrim. It scales with
        // the screen so the buttons below always fit on small phones.
        val headerHeight = (LocalConfiguration.current.screenHeightDp * 0.34f).dp.coerceAtMost(300.dp)
        Box(Modifier.fillMaxWidth().height(headerHeight)) {
            PhotoBackground(
                BrandPhotos.CommunityMeeting,
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.45f),
                    0.45f to Color.Black.copy(alpha = 0.3f),
                    1f to Color.Black.copy(alpha = 0.85f),
                ),
            )
            PhotoCredit(BrandPhotos.CommunityMeeting.credit, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp))
            BrandLockup(Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.md), light = true)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val slide = slides[page]
            Column(
                Modifier.fillMaxSize().padding(vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(KrColors.Primary, KrColors.Accent))),
                    contentAlignment = Alignment.Center,
                ) { Icon(slide.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp)) }
                Spacer(Modifier.height(Spacing.md))
                Text(stringResource(slide.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    stringResource(slide.body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val pageLabel = stringResource(R.string.cd_page_indicator, pager.currentPage + 1, slides.size)
        Row(Modifier.semantics { contentDescription = pageLabel }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(slides.size) { i ->
                Box(
                    Modifier.size(if (i == pager.currentPage) 10.dp else 8.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) KrColors.Primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        KrPrimaryButton(stringResource(R.string.action_get_started), onGetStarted, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.xs))
        KrOutlinedButton(stringResource(R.string.action_have_account), onSignIn, Modifier.fillMaxWidth())
        com.kakuma.resourcelink.core.designsystem.component.KrTextButton(stringResource(R.string.action_browse_guest), onBrowseAsGuest)
        Text(
            "English · Kiswahili · Español · Italiano · Deutsch",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        }
    }
}
