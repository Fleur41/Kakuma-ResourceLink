package com.kakuma.resourcelink.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kakuma.resourcelink.R

private val MinTouch = 52.dp

/** Primary filled button. While [loading], the label stays (for accessibility) next to a spinner and taps are ignored. */
@Composable
fun KrPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val loadingLabel = stringResource(R.string.cd_loading)
    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        modifier = modifier
            .heightIn(min = MinTouch)
            .semantics { if (loading) stateDescription = loadingLabel },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(),
    ) { ButtonContent(text, loading, icon, MaterialTheme.colorScheme.onPrimary) }
}

@Composable
fun KrOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        modifier = modifier.heightIn(min = MinTouch),
        shape = MaterialTheme.shapes.medium,
    ) { ButtonContent(text, loading, icon, MaterialTheme.colorScheme.primary) }
}

@Composable
fun KrTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 44.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun KrDestructiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouch),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun ButtonContent(text: String, loading: Boolean, icon: ImageVector?, spinnerColor: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = spinnerColor)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Full-width helper used by forms. */
fun Modifier.fullWidth() = this.fillMaxWidth()
