/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.data.api

/**
 * Which liked list the Liked Songs page reads, when both accounts are signed in: null follows the
 * account chosen in Settings, true is YouTube Music's list, false is Spotify's. Set by the library
 * entry that was opened.
 */
object LikedSource {
    @Volatile var yt: Boolean? = null
}
