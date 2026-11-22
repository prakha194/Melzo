package com.music.spotui.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

/**
 * Descriptions (show, episode, audiobook, artist bio, playlist) with their links made tappable.
 * Same text, same layout: the [AnnotatedString] goes to the page's own `Text`, so lines, "more"
 * toggles and truncation stay as they were. A link opens in the browser.
 */
private val LINK = Regex("""(?:https?://|www\.)[^\s<>"']+""", RegexOption.IGNORE_CASE)

/** [text] with every link in it tappable, in [linkColor] and underlined. */
@Composable
fun rememberLinkedText(text: String, linkColor: Color): AnnotatedString {
    val context = LocalContext.current
    return remember(text, linkColor) {
        if (!LINK.containsMatchIn(text)) return@remember AnnotatedString(text)
        val styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
        buildAnnotatedString {
            var last = 0
            for (hit in LINK.findAll(text)) {
                // Sentence punctuation after a link is not part of it.
                val link = hit.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
                append(text.substring(last, hit.range.first))
                withLink(LinkAnnotation.Clickable("link", styles) { openDescriptionLink(context, link) }) { append(link) }
                last = hit.range.first + link.length
            }
            append(text.substring(last))
        }
    }
}

/** A tapped link: in the browser. */
fun openDescriptionLink(context: android.content.Context, link: String) {
    val web = if (link.startsWith("www.", ignoreCase = true)) "https://$link" else link
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
