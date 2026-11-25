package com.kakuma.resourcelink.feature.stories.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Block-level element of the small Markdown subset stories use. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Numbered(val number: Int, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
}

object MarkdownParser {
    fun parse(source: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val paragraph = StringBuilder()
        fun flush() {
            if (paragraph.isNotBlank()) blocks += MdBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }
        source.lines().forEach { raw ->
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            when {
                trimmed.isEmpty() -> flush()
                trimmed.startsWith("#") -> {
                    flush()
                    val hashes = trimmed.takeWhile { it == '#' }.length
                    blocks += MdBlock.Heading(hashes.coerceIn(1, 3), trimmed.drop(hashes).trim())
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> { flush(); blocks += MdBlock.Bullet(trimmed.drop(2).trim()) }
                NUMBERED.matches(trimmed) -> {
                    flush()
                    val (number, text) = NUMBERED.matchEntire(trimmed)!!.destructured
                    blocks += MdBlock.Numbered(number.toIntOrNull() ?: 1, text.trim())
                }
                trimmed.startsWith(">") -> { flush(); blocks += MdBlock.Quote(trimmed.drop(1).trim()) }
                else -> { if (paragraph.isNotEmpty()) paragraph.append(' '); paragraph.append(trimmed) }
            }
        }
        flush()
        return blocks
    }

    private val NUMBERED = Regex("""(\d{1,9})[.)]\s+(.*)""")
    private const val ESCAPABLE = "\\`*_{}[]()#+-.!>~|<"
    private val Bold = SpanStyle(fontWeight = FontWeight.SemiBold)
    private val Italic = SpanStyle(fontStyle = FontStyle.Italic)

    /**
     * Inline ***bold italic***, **bold** and *italic* / _italic_ (nestable), plus backslash escapes such as `\*`.
     * Anything else is shown as plain text (no links or HTML).
     */
    fun inline(text: String): AnnotatedString = buildAnnotatedString { appendInline(text) }

    private fun AnnotatedString.Builder.appendInline(text: String) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length && text[i + 1] in ESCAPABLE) {
                append(text[i + 1]); i += 2; continue
            }
            val triple = if (text.startsWith("***", i)) findMarker(text, "***", i + 3) else -1
            if (triple > i + 3) {
                withStyle(Bold) { withStyle(Italic) { appendInline(text.substring(i + 3, triple)) } }
                i = triple + 3; continue
            }
            val double = if (text.startsWith("**", i)) findMarker(text, "**", i + 2) else -1
            if (double > i + 2) {
                withStyle(Bold) { appendInline(text.substring(i + 2, double)) }
                i = double + 2; continue
            }
            val single = if ((c == '*' || c == '_') && i + 1 < text.length && text[i + 1] != ' ' && text[i + 1] != c) findMarker(text, c.toString(), i + 1) else -1
            if (single > i + 1) {
                withStyle(Italic) { appendInline(text.substring(i + 1, single)) }
                i = single + 1; continue
            }
            append(c); i++
        }
    }

    /** Index of the closing [marker] from [from], skipping escaped characters and, for single markers, doubled ones. */
    private fun findMarker(text: String, marker: String, from: Int): Int {
        var j = from
        while (j < text.length) {
            when {
                text[j] == '\\' -> j += 2
                text.startsWith(marker, j) && (marker.length > 1 || j + 1 >= text.length || text[j + 1] != marker[0]) -> return j
                marker.length == 1 && text[j] == marker[0] -> j += 2 // part of a ** pair inside an italic run
                else -> j++
            }
        }
        return -1
    }
}

@Composable
fun MarkdownText(source: String, modifier: Modifier = Modifier) {
    val blocks = remember(source) { MarkdownParser.parse(source) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    MarkdownParser.inline(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    modifier = Modifier.semantics { heading() },
                )
                is MdBlock.Paragraph -> Text(MarkdownParser.inline(block.text), style = MaterialTheme.typography.bodyLarge)
                is MdBlock.Bullet -> Row {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(MarkdownParser.inline(block.text), style = MaterialTheme.typography.bodyLarge)
                }
                is MdBlock.Numbered -> Row {
                    Text("${block.number}.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(MarkdownParser.inline(block.text), style = MaterialTheme.typography.bodyLarge)
                }
                is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Spacer(Modifier.width(3.dp).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        MarkdownParser.inline(block.text),
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}
