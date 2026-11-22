/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.ui.components

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

/** What a share is about: an item of the app (a song, album, playlist, artist…), by its name. */
data class ShareTarget(val uri: String, val title: String, val subtitle: String = "", val coverUri: String = "") {
    /**
     * What is shared: the name, who it is by, and the item's page on the service it comes from —
     * a Spotify link for a Spotify item, a YouTube Music link for a YouTube Music one. An item
     * from nowhere else (a local file) is shared by its name alone.
     */
    val text: String get() = buildString {
        append(if (subtitle.isBlank()) title else "$title — $subtitle")
        webLink()?.let { append("\n").append(it) }
    }

    /** The item's public page, when its uri names one. */
    private fun webLink(): String? {
        val kind = uri.substringBefore(':')
        val rest = uri.substringAfter(':', "").removePrefix(kind).removePrefix(":")
        return when {
            uri.startsWith("spotify:") && rest.isNotBlank() -> "https://open.spotify.com/" + rest.replace(':', '/')
            uri.startsWith("ytmusic:track:") -> "https://music.youtube.com/watch?v=" + rest.substringAfterLast(':')
            uri.startsWith("ytmusic:album:") -> "https://music.youtube.com/browse/" + rest.substringAfterLast(':')
            uri.startsWith("ytmusic:playlist:") -> "https://music.youtube.com/playlist?list=" + rest.substringAfterLast(':')
            else -> null
        }
    }
}

/**
 * Sharing: the item's name, through Android's share sheet. Asked by a menu (which closes right
 * after) and sent by the app's root (App.kt).
 */
object ShareItem {
    val request = MutableStateFlow<ShareTarget?>(null)

    fun show(target: ShareTarget) { request.value = target }

    /** Android's share sheet for [target]. */
    fun send(context: Context, target: ShareTarget) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, target.text)
        runCatching { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
