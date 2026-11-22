package app.myspoti.host.android

import android.content.Context

/** Told when the phone starts or stops ringing or being in a call (Android 12+), on a thread of its own. */
object PhoneCall {
    @Volatile var listener: ((busy: Boolean) -> Unit)? = null
}

/**
 * Calls in the timeline: the phone's audio mode (ringing, in a call, back to normal), the moments
 * a report of "the music did not come back after a call" turns on; [PhoneCall] is told. Android 12+;
 * before it, the telephony call state ([installCallState]).
 */
internal object AudioModeWatch {
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        if (android.os.Build.VERSION.SDK_INT < 31) { installCallState(context); return }
        runCatching {
            val audio = context.getSystemService(android.media.AudioManager::class.java)
            audio.addOnModeChangedListener(java.util.concurrent.Executors.newSingleThreadExecutor()) { mode ->
                val name = when (mode) {
                    android.media.AudioManager.MODE_NORMAL -> "normal"
                    android.media.AudioManager.MODE_RINGTONE -> "ringing"
                    android.media.AudioManager.MODE_IN_CALL -> "in a call"
                    android.media.AudioManager.MODE_IN_COMMUNICATION -> "in a voice/video call (app)"
                    android.media.AudioManager.MODE_CALL_SCREENING -> "call screening"
                    else -> "mode $mode"
                }
                Logbook.log("audio mode: $name")
                PhoneCall.listener?.invoke(mode != android.media.AudioManager.MODE_NORMAL)
            }
        }
    }

    /**
     * Android 10 and 11 have no audio mode listener: the call state instead (no permission asked for
     * it there). Reported: the music went on through a call; nothing paused the players that hold no
     * audio focus (the account's page). Calls of
     * other apps (voice over the internet) are not seen there.
     */
    @Suppress("DEPRECATION")
    private fun installCallState(context: Context) { android.os.Handler(android.os.Looper.getMainLooper()).post {
        runCatching {
            val phone = context.getSystemService(android.telephony.TelephonyManager::class.java) ?: return@runCatching
            phone.listen(object : android.telephony.PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    val name = when (state) {
                        android.telephony.TelephonyManager.CALL_STATE_RINGING -> "ringing"
                        android.telephony.TelephonyManager.CALL_STATE_OFFHOOK -> "in a call"
                        else -> "normal"
                    }
                    Logbook.log("call state: $name")
                    PhoneCall.listener?.invoke(state != android.telephony.TelephonyManager.CALL_STATE_IDLE)
                }
            }, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        }.onFailure { Logbook.log("call state not watched: ${it.javaClass.simpleName}") }
    } }
}
