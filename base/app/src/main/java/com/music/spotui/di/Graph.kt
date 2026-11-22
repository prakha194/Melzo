package com.music.spotui.di

import android.app.Application
import app.myspoti.host.android.MyspotiRuntime
import com.music.spotui.data.api.Api
import com.music.spotui.ui.repository.AppRepository

/** Manual replacement for Spotui's Hilt graph: app-wide singletons. */
object Graph {
    lateinit var app: Application
        private set

    fun init(application: Application) {
        app = application
        // The data saver first: on a metered network with it on, nothing may go out, not even the page.
        com.music.spotui.data.DataSaver.init(application)
        // The app's music platform: no service behind it (the phone, Live Radio, the extensions).
        MyspotiRuntime.start(application)
        // Metadata (searches, albums, artists) from the providers in the user's order.
        com.music.spotui.data.catalog.CatalogRuntime.install()
        // The YouTube Music account, when chosen: its hidden page, once signed in.
        com.music.spotui.di.YtmRuntime.startIfChosen()
        // The crossfade length (0 = off), in the player from the first song on.
        com.music.spotui.data.preferences.applyCrossfade(com.music.spotui.data.preferences.getCrossfadeSeconds(application))
        com.music.spotui.data.local.ListenHistory.init(application)
        // Downloaded songs (private storage), and the downloads left unfinished last time.
        // Supporter thank-yous (accent colour) before any screen draws.
        com.music.spotui.data.api.SupportReminder.init(application)
        com.music.spotui.data.offline.OfflineStore.init(application)
        com.music.spotui.data.offline.OfflineCollections.init(application)
        SongPlayer.startLibraryOutbox(application)
        com.music.spotui.data.offline.OfflineCollections.refreshAll(application)
        com.music.spotui.data.local.LocalPlaylists.init(application)
        com.music.spotui.data.local.LibraryOrder.init(application)
        runCatching { com.music.spotui.data.DataSaverGlide.install(application) }
        com.music.spotui.data.offline.OfflineDownloader.resumePending()
        // Episodes have their own queue and their own file: resumed the same way.
        com.music.spotui.data.offline.PodcastDownloader.resumePending()
        // Installed extensions: each enabled one loads in its own sandbox, off the main thread.
        com.music.spotui.extensions.Extensions.init(application)
        // Live Radio: its own setting and favorites file (see com.music.spotui.radio).
        com.music.spotui.radio.RadioFeaturePref.init(application)
        com.music.spotui.data.preferences.LocalFilesPref.init(application)
        // A watch linked before reaches this phone over the Wi-Fi too, while the app runs.
        com.music.spotui.wear.WatchLan.startIfLinked(application)
        com.music.spotui.radio.RadioFavoritesStore.init(application)
    }

    val api: Api by lazy { Api(app) }
    val repository: AppRepository by lazy { AppRepository(api) }
    val currentSongState: CurrentSongState by lazy { CurrentSongState() }
}
