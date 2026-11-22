package app.myspoti.wear

import android.content.Context
import android.content.Intent

/** Keeps the process and the sound going while the watch plays (a media playback foreground service). */
class WatchPlaybackService : android.app.Service() {
    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PAUSE) { WatchLocal.pause(); return START_NOT_STICKY }
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(android.app.NotificationChannel(CHANNEL, getString(R.string.wear_playing_channel), android.app.NotificationManager.IMPORTANCE_LOW))
        val local = WatchLocal.now.value
        val open = android.app.PendingIntent.getActivity(this, 0, Intent(this, WatchActivity::class.java), android.app.PendingIntent.FLAG_IMMUTABLE)
        val notification = android.app.Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(local?.name ?: getString(R.string.app_name)).setContentText(local?.artists.orEmpty())
            .setContentIntent(open).setOngoing(true).build()
        startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "playback"
        const val ACTION_PAUSE = "app.myspoti.wear.PAUSE"

        /** Foreground while [playing]; gone a moment after it stops. */
        fun follow(context: Context, playing: Boolean) {
            val intent = Intent(context, WatchPlaybackService::class.java)
            val any = playing || WatchLocal.now.value?.playing == true
            if (any) runCatching { context.startForegroundService(intent) } else context.stopService(intent)
        }
    }
}
