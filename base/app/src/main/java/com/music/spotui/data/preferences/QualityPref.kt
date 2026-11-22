package com.music.spotui.data.preferences

/*
 * Sound quality: how downloads are saved.
 */

import android.content.Context
import kotlinx.coroutines.launch

private const val PREF = "settings_prefs"
private const val KEY_CHOSEN = "quality_chosen"
private const val KEY_CANVAS_WIFI = "canvas_wifi_only"

/**
 * How a downloaded song is saved: not at all, small (AAC re-encoded from the checked FLAC),
 * FLAC 16-bit (CD), or FLAC 24-bit when it exists.
 */
enum class DownloadQuality {
    OFF, SMALL, CD, HI_RES,
    /** Asked when a download starts (once for a whole playlist or album). */
    ASK;

    companion object {
        /** A quality a file can be saved in ([ASK] and [OFF] are not); null when unknown. */
        fun saved(name: String?): DownloadQuality? = entries.firstOrNull { it.name == name }?.takeIf { it != ASK && it != OFF }
    }
}

/** Canvas (the looping videos) only on Wi-Fi: they cost data on every song. */
fun isCanvasWifiOnly(c: Context): Boolean = prefs(c).getBoolean(KEY_CANVAS_WIFI, false)
fun setCanvasWifiOnly(c: Context, on: Boolean) = prefs(c).edit().putBoolean(KEY_CANVAS_WIFI, on).apply()

fun getDownloadQuality(c: Context): DownloadQuality = when {
    !isDownloadsEnabled(c) -> DownloadQuality.OFF
    isAskDownloadQuality(c) -> DownloadQuality.ASK
    isSmallDownloads(c) -> DownloadQuality.SMALL
    isHiRes(c) -> DownloadQuality.HI_RES
    else -> DownloadQuality.CD
}

fun setDownloadQuality(c: Context, q: DownloadQuality) {
    setDownloadsEnabled(c, q != DownloadQuality.OFF)
    setHiRes(c, q == DownloadQuality.HI_RES)
    setSmallDownloads(c, q == DownloadQuality.SMALL)
    setAskDownloadQuality(c, q == DownloadQuality.ASK)
}

/** The quality step of the onboarding was seen (new installs and updates alike). */
fun isQualityChosen(c: Context): Boolean = prefs(c).getBoolean(KEY_CHOSEN, false)
fun setQualityChosen(c: Context) = prefs(c).edit().putBoolean(KEY_CHOSEN, true).apply()

private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
