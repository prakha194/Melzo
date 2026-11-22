package com.music.spotui.di

/**
 * "Upgrade source while playing" (Settings > Playback): see [SongPlayer.upgradeSource]. On by default.
 */
object SourceUpgrade {
    private const val PREFS = "source_upgrade"
    private const val KEY = "on"

    @Volatile var on: Boolean = true
        private set

    /** The song has played this long before the better sources are asked again: its start is not slowed. */
    const val START_DELAY_MS = 4_000L
    /** How long one source is given to answer again. */
    const val ASK_MS = 20_000L
    /** A swap needs at least this much of the song left: near the end it is not worth the short break. */
    const val MIN_LEFT_MS = 30_000L
    /** How far a new answer's length may be from the song's before it counts as another recording or a clip. */
    const val LENGTH_SLACK_MS = 3_000L

    fun init(context: android.content.Context) {
        on = context.getSharedPreferences(PREFS, 0).getBoolean(KEY, true)
    }

    fun set(context: android.content.Context, value: Boolean) {
        on = value
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY, value).apply()
    }
}
