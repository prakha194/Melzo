/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.ui.components

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

/** What a share is about: an item of the app (a song, album, playlist, artist…), by its name. */
data class ShareTarget(val uri: String, val title: String, val subtitle: String = "", val coverUri: String = "") {
    /** What is shared: the name and who it is by. No service link: the app has none of its own. */
    val text: String get() = if (subtitle.isBlank()) title else "$title — $subtitle"
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
