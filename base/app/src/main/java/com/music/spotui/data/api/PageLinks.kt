/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.data.api

import kotlinx.coroutines.flow.MutableStateFlow

/** A page to open over whatever is on screen (an item's uri: a list, an artist, a show), from a link or an extension. */
object PageLinks {
    val page = MutableStateFlow<String?>(null)
}
