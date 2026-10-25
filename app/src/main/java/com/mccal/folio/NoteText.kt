package com.mccal.folio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * The little of Markdown a changelog line uses: bold, italic and https links, and nothing else. Anything else is left
 * exactly as it was typed, which is the safe way round: a stray asterisk shows up as an asterisk rather than
 * swallowing the rest of a sentence.
 */
internal object NoteText {

    /**
     * Bold, italic and links inside one paragraph.
     *
     * `**bold**`, `*italic*` (and `_italic_`), `[words](https://…)`. A marker with nothing to close it, or a link to
     * anything but https, is left as the author typed it rather than guessed at.
     */
    fun inline(source: String, link: Color = FolioColors.BlueOnDark): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < source.length) {
            val rest = source.substring(i)
            val emphasis = MARKERS.firstOrNull { rest.startsWith(it.mark) }
            val closed = emphasis?.let { rest.indexOf(it.mark, it.mark.length).takeIf { end -> end > it.mark.length } }
            val linked = LINK.matchAt(rest, 0)?.takeIf { it.groupValues[2].startsWith("https://") }
            when {
                emphasis != null && closed != null -> {
                    withStyle(emphasis.style) { append(inline(rest.substring(emphasis.mark.length, closed), link)) }
                    i += closed + emphasis.mark.length
                }
                linked != null -> {
                    val url = linked.groupValues[2]
                    withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = link)))) {
                        append(inline(linked.groupValues[1], link))
                    }
                    i += linked.value.length
                }
                else -> {
                    append(source[i])
                    i++
                }
            }
        }
    }

    private data class Marker(val mark: String, val style: SpanStyle)

    /** Longest first, so `**bold**` isn't read as an empty italic. */
    private val MARKERS = listOf(
        Marker("**", SpanStyle(fontWeight = FontWeight.SemiBold)),
        Marker("__", SpanStyle(fontWeight = FontWeight.SemiBold)),
        Marker("*", SpanStyle(fontStyle = FontStyle.Italic)),
        Marker("_", SpanStyle(fontStyle = FontStyle.Italic)),
    )

    private val LINK = Regex("""\[([^\[\]]{1,200})]\(([^()\s]{1,500})\)""")
}
