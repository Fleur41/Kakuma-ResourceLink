package com.kakuma.resourcelink.feature.stories.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.kakuma.resourcelink.R
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

private val BoldStyle = SpanStyle(fontWeight = FontWeight.Bold)
private val ItalicStyle = SpanStyle(fontStyle = FontStyle.Italic)
// Text sizes the library gives heading paragraphs (HeadingStyle.H1..H3 visuals).
private val HeadingSizes = listOf(2.em, 1.5.em, 1.17.em)

/**
 * Like a word processor, the line after a heading is normal text. The library carries the heading's
 * size and bold into the new paragraph, so once text is typed into a non-heading paragraph that still
 * looks like a heading, strip those styles from that paragraph.
 */
private fun keepNormalParagraphsNormal(state: RichTextState) {
    val selection = state.selection
    if (!selection.collapsed) return
    // The displayed text joins paragraphs with spaces, but each paragraph keeps its own paragraph-style range.
    val cursor = selection.min
    val ranges = state.annotatedString.paragraphStyles
    val paragraph = ranges.firstOrNull { cursor >= it.start && cursor < it.end } ?: ranges.lastOrNull { it.end == cursor } ?: return
    // Only once something is typed in this paragraph: style lookups read the character before the cursor.
    if (cursor <= paragraph.start || state.currentHeadingStyle != HeadingStyle.Normal) return
    val size = state.currentSpanStyle.fontSize
    if (size !in HeadingSizes) return
    state.removeSpanStyle(SpanStyle(fontSize = size, fontWeight = FontWeight.Bold), TextRange(paragraph.start, paragraph.end))
}

/**
 * Word-style editor for a story body. The story is still stored as Markdown ([markdown] in,
 * [onMarkdownChange] out), and the toolbar matches the web editor: text style, bold, italic and lists.
 */
@Composable
fun StoryBodyEditor(
    markdown: String,
    onMarkdownChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val state = rememberRichTextState()
    val emit by rememberUpdatedState(onMarkdownChange)
    // Markdown we last loaded or reported, so our own edits are not loaded back into the editor.
    var lastMarkdown by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(markdown) {
        if (markdown != lastMarkdown) {
            state.setMarkdown(markdown)
            state.history.clear()
            lastMarkdown = state.toMarkdown()
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.annotatedString }.collect {
            keepNormalParagraphsNormal(state)
            val md = if (state.annotatedString.text.isBlank()) "" else state.toMarkdown()
            if (lastMarkdown != null && md != lastMarkdown) {
                lastMarkdown = md
                emit(md)
            }
        }
    }

    val clipShape = MaterialTheme.shapes.medium
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
        Column(
            Modifier.fillMaxWidth().padding(top = 6.dp)
                .clip(clipShape)
                .border(1.dp, if (error != null) colors.error else colors.outline, MaterialTheme.shapes.medium)
                .background(colors.surface, MaterialTheme.shapes.medium),
        ) {
            FormatToolbar(state)
            HorizontalDivider(color = colors.outlineVariant)
            BasicRichTextEditor(
                state = state,
                modifier = Modifier.fillMaxWidth().padding(16.dp).semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                minLines = 8,
                decorationBox = { inner ->
                    Box {
                        if (state.annotatedString.text.isEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                        }
                        inner()
                    }
                },
            )
        }
        error?.let {
            Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}

@Composable
private fun FormatToolbar(state: RichTextState) {
    val span = state.currentSpanStyle
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val scrollState = rememberScrollState()
    Row(
        Modifier.fillMaxWidth()
            .background(backgroundColor)
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.format_undo), enabled = state.history.canUndo) { state.history.undo() }
        ToolButton(Icons.AutoMirrored.Outlined.Redo, stringResource(R.string.format_redo), enabled = state.history.canRedo) { state.history.redo() }
        ToolbarDivider()
        TextStyleMenu(state)
        ToolbarDivider()
        ToggleButton(Icons.Outlined.FormatBold, stringResource(R.string.format_bold), span.fontWeight == FontWeight.Bold) { state.toggleSpanStyle(BoldStyle) }
        ToggleButton(Icons.Outlined.FormatItalic, stringResource(R.string.format_italic), span.fontStyle == FontStyle.Italic) { state.toggleSpanStyle(ItalicStyle) }
        ToolbarDivider()
        ToggleButton(Icons.Outlined.FormatListBulleted, stringResource(R.string.format_bullet_list), state.isUnorderedList) { state.toggleUnorderedList() }
        ToggleButton(Icons.Outlined.FormatListNumbered, stringResource(R.string.format_numbered_list), state.isOrderedList) { state.toggleOrderedList() }
        ToolbarDivider()
        ToolButton(Icons.Outlined.FormatClear, stringResource(R.string.format_clear)) {
            state.history.group {
                state.clearSpanStyles()
                state.setHeadingStyle(HeadingStyle.Normal)
                if (state.isUnorderedList) state.removeUnorderedList()
                if (state.isOrderedList) state.removeOrderedList()
            }
        }
    }
}

@Composable
private fun TextStyleMenu(state: RichTextState) {
    var open by remember { mutableStateOf(false) }
    val styles = listOf(
        HeadingStyle.Normal to stringResource(R.string.format_normal),
        HeadingStyle.H1 to stringResource(R.string.format_heading_1),
        HeadingStyle.H2 to stringResource(R.string.format_heading_2),
        HeadingStyle.H3 to stringResource(R.string.format_heading_3),
    )
    val current = styles.firstOrNull { it.first == state.currentHeadingStyle } ?: styles.first()
    val menuLabel = stringResource(R.string.format_text_style)
    Box {
        TextButton(
            onClick = { open = true },
            modifier = Modifier.semantics { contentDescription = "$menuLabel: ${current.second}" },
        ) {
            Text(current.second, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            styles.forEach { (style, name) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            name,
                            style = when (style) {
                                HeadingStyle.H1 -> MaterialTheme.typography.titleLarge
                                HeadingStyle.H2 -> MaterialTheme.typography.titleMedium
                                HeadingStyle.H3 -> MaterialTheme.typography.titleSmall
                                else -> MaterialTheme.typography.bodyLarge
                            },
                        )
                    },
                    onClick = { state.setHeadingStyle(style); open = false },
                )
            }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, contentDescription = label) }
}

@Composable
private fun ToggleButton(icon: ImageVector, label: String, checked: Boolean, onToggle: () -> Unit) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = { onToggle() },
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) { Icon(icon, contentDescription = label) }
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp).width(1.dp))
}
