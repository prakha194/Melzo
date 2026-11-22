package app.myspoti.host.android

import android.content.Context
import app.myspoti.api.MyspotiApi
import app.myspoti.api.NeutralApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The app's music platform for the process. There is no music service behind it: [NeutralApi]
 * is the base, and the app layers over it the metadata providers ([useApi]), the phone's files,
 * Live Radio and the extensions. Nothing here talks to a streaming service.
 */
object MyspotiRuntime {
    private val neutral = NeutralApi()

    /** What the app reads its music from: the app's wrapper ([useApi]), else the bare [NeutralApi]. */
    val api: MyspotiApi get() = wrapped ?: neutral
    @Volatile private var wrapped: MyspotiApi? = null

    /** The app's wrapper over the neutral base (the metadata catalog, for searches and the items they lead to). */
    fun useApi(api: MyspotiApi) { wrapped = api }

    /** The neutral base itself, for the app's wrapper to build on. */
    val base: MyspotiApi get() = neutral

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile var isStarted: Boolean = false
        private set

    /** One client for the app's own reads (catalog, lyrics…): bounded timeouts, a ping on idle HTTP/2 connections. */
    val apiHttp: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .pingInterval(20, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** The phone moved to another network: the idle connections belong to the old one. Not on the main thread. */
    fun resetApiConnections() { apiHttp.connectionPool.evictAll() }

    fun start(context: Context) {
        if (isStarted) return
        isStarted = true
        AudioModeWatch.install(context.applicationContext)
    }
}
