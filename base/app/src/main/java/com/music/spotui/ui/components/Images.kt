package com.music.spotui.ui.components

/** An image address as the UI loads it: null when there is none. */
fun imageUrlOf(url: String?): String? = url?.takeIf { it.isNotBlank() && !it.startsWith("spotify:") }

/** The cover of a playlist that has none of its own: its first song's cover, "" when no song has one. */
fun mosaicCoverFor(songCovers: List<String>): String = songCovers.firstOrNull { it.isNotBlank() && !it.startsWith("spotify:") }.orEmpty()
