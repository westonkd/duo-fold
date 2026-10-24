package com.mccal.folio

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Markdown subset What's New reads, and the promise that nothing else is touched. */
class NoteTextTest {

    @Test fun `bold and italic are styles, not characters`() {
        val text = NoteText.inline("Warm icons for **2,400** apps, *mostly*.")
        assertEquals("Warm icons for 2,400 apps, mostly.", text.text)
        val bold = text.spanStyles.single { it.item.fontWeight == FontWeight.SemiBold }
        assertEquals("2,400", text.text.substring(bold.start, bold.end))
        val italic = text.spanStyles.single { it.item.fontStyle == FontStyle.Italic }
        assertEquals("mostly", text.text.substring(italic.start, italic.end))
    }

    @Test fun `a marker with nothing to close it stays as it was typed`() {
        assertEquals("2 * 3 is 6", NoteText.inline("2 * 3 is 6").text)
        assertEquals("**almost bold", NoteText.inline("**almost bold").text)
        assertTrue(NoteText.inline("2 * 3 is 6").spanStyles.isEmpty())
    }

    @Test fun `a link becomes a link`() {
        val text = NoteText.inline("Read the [guide](https://example.com/guide) first.")
        assertEquals("Read the guide first.", text.text)
        val link = text.getLinkAnnotations(0, text.length).single()
        assertEquals("https://example.com/guide", (link.item as LinkAnnotation.Url).url)
        assertEquals("guide", text.text.substring(link.start, link.end))
    }

    @Test fun `anything but https is left as text`() {
        for (url in listOf("http://example.com", "javascript:alert(1)", "folio://package/x", "/etc/passwd")) {
            val text = NoteText.inline("Open [this]($url) now.")
            assertEquals("Open [this]($url) now.", text.text)
            assertTrue("$url should not be a link", text.getLinkAnnotations(0, text.length).isEmpty())
        }
    }
}
