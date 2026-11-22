/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.data.preferences

import android.content.SharedPreferences

/**
 * Settings kept per account. The app has no accounts of its own any more: every setting is the
 * phone's, under its plain key (the first account's, which earlier versions kept the same way).
 */
object ProfilePrefs {
    fun key(base: String): String = base

    fun getString(p: SharedPreferences, base: String, default: String?): String? = p.getString(base, default)

    fun getBoolean(p: SharedPreferences, base: String, default: Boolean): Boolean = p.getBoolean(base, default)
}
