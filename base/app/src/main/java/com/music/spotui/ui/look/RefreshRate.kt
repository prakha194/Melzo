package com.music.spotui.ui.look

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The screen's refresh rate for the app (Settings > Display), after BitChord's DisplayRefreshRates:
 * "Automatic" leaves it to the phone; a number asks for that rate while the app is in front (60 Hz
 * saves battery, and keeps the Immersive look's blurs smooth on phones that struggle at 120 Hz).
 * Only rates the screen offers, at its current resolution, are listed.
 */
object RefreshRate {
    private const val PREFS = "display"
    private const val KEY = "refresh_rate"

    /** 0: automatic; else the rate in Hz. */
    var chosen by mutableIntStateOf(0)
        private set

    private var activity: WeakReference<Activity>? = null

    fun init(context: Context) {
        chosen = context.getSharedPreferences(PREFS, 0).getInt(KEY, 0)
    }

    fun set(context: Context, hz: Int) {
        chosen = hz
        context.getSharedPreferences(PREFS, 0).edit().putInt(KEY, hz).apply()
        activity?.get()?.let { a -> a.runOnUiThread { apply(a) } }
    }

    /** The rates this screen offers at its current resolution, rounded, lowest first. */
    fun supported(activity: Activity): List<Int> {
        val display = display(activity) ?: return emptyList()
        val current = display.mode
        return display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .map { it.refreshRate.roundToInt() }
            .filter { it in 30..240 }
            .distinct().sorted()
    }

    /** Applies the choice to [activity]'s window (called as it comes to the front, and on a change). */
    fun apply(activity: Activity) {
        this.activity = WeakReference(activity)
        val display = display(activity) ?: return
        val current = display.mode
        val modeId = if (chosen == 0) 0 else display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .minByOrNull { abs(it.refreshRate - chosen) }?.modeId ?: 0
        val params = activity.window.attributes
        if (params.preferredDisplayModeId == modeId) return
        params.preferredDisplayModeId = modeId
        activity.window.attributes = params
    }

    @Suppress("DEPRECATION")
    private fun display(activity: Activity): Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display else activity.windowManager.defaultDisplay
}
