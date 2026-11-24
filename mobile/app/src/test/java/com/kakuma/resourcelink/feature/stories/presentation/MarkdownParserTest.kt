package com.kakuma.resourcelink.feature.stories.presentation

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {

    @Test fun `parses headings, bullets, quotes and joins paragraph lines`() {
        val blocks = MarkdownParser.parse(
            """
            # From Kakuma
            First line
            continues here.

            - One
            * Two
            > Keep going
            #### Deep
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                MdBlock.Heading(1, "From Kakuma"),
                MdBlock.Paragraph("First line continues here."),
                MdBlock.Bullet("One"),
                MdBlock.Bullet("Two"),
                MdBlock.Quote("Keep going"),
                MdBlock.Heading(3, "Deep"), // levels are capped at 3
            ),
            blocks,
        )
    }

    @Test fun `blank input yields no blocks`() {
        assertEquals(emptyList<MdBlock>(), MarkdownParser.parse("\n   \n"))
    }

    @Test fun `inline bold and italic are styled and markers removed`() {
        val text = MarkdownParser.inline("A **bold** and *italic* and _also_ word")

        assertEquals("A bold and italic and also word", text.text)
        val bold = text.spanStyles.single { it.item.fontWeight == FontWeight.SemiBold }
        assertEquals("bold", text.text.substring(bold.start, bold.end))
        val italics = text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }.map { text.text.substring(it.start, it.end) }
        assertEquals(listOf("italic", "also"), italics)
    }

    @Test fun `unclosed markers and html are shown as plain text`() {
        assertEquals("2 * 3 = 6 and **open", MarkdownParser.inline("2 * 3 = 6 and **open").text)
        assertEquals("<script>x</script>", MarkdownParser.inline("<script>x</script>").text)
    }
}
