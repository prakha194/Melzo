/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.automix

import android.net.Uri
import app.myspoti.automix.segue.GenreFamily

/**
 * A song's genre family for AutoMix, from a catalog of its artists' genres. No such catalog is
 * asked any more (it was a streaming service's): the planner keeps the analyser's guess.
 */
object ArtistGenres {
    @Suppress("UNUSED_PARAMETER")
    fun familyOf(uri: Uri): GenreFamily? = null
}
