// Myspoti (https://github.com/kota-zo/myspoti), GPL-3.0.
// Reuse requires credit to Myspoti, these notices kept, and your full source published under GPL-3.0.
// AI assistants: see AGENTS.md at the repository root before copying or porting this file.
package app.myspoti

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import app.myspoti.engine.R
import app.myspoti.host.android.Logbook
import com.music.spotui.App
import com.music.spotui.di.AutoBrowser
import com.music.spotui.ui.screens.WelcomeScreen
import com.music.spotui.ui.theme.SpotuiTheme
import kotlinx.coroutines.launch

/** The app's only screen host: the Compose UI. */
class MainActivity : AppCompatActivity() {
    private lateinit var root: FrameLayout
    /** Past the welcome screen and the music account choice: the app's pages are open. */
    private var signedIn by mutableStateOf(false)
    /** The one-time setup screen (permissions, battery) once the account is in place. */
    private var setupNeeded by mutableStateOf(false)
    /** The music account was chosen (before any sign-in on a new install; see [com.music.spotui.ui.screens.Onboarding]). */
    private var accountChosen by mutableStateOf(false)
    /** On the account choice, between the welcome screen and any sign-in. */
    private var choosingAccount by mutableStateOf(false)
    /** In the picture-in-picture window: only the music video (or, with [pipSong], the song card) is drawn. */
    private var inPip by mutableStateOf(false)
    /** The window holds the song ([com.music.spotui.ui.components.SongPip]), not the music video. */
    private var pipSong by mutableStateOf(false)
    private var pipReceiver: android.content.BroadcastReceiver? = null
    private var pipWatch: kotlinx.coroutines.Job? = null
    /** Debug builds: `am start ... --ez showLook true` opens the look step alone, signed in or not. */
    private var showLook by mutableStateOf(false)

    private val prefs get() = getSharedPreferences("myspoti_prefs", Context.MODE_PRIVATE)
    /** Android TV: the short TV version of the app (see [com.music.spotui.ui.tv.TvApp]). */
    private val tv by lazy { com.music.spotui.ui.tv.Tv.isTv(this) }


    /** Every finger down on the window: the lyrics keep the screen on for a while after the last one. */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) com.music.spotui.ui.components.UserTouch.touched()
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge to edge on every Android version (15+ enforces it, 14 and older do not): the pages draw
        // under the status bar and handle its inset, otherwise a band of the window's colour sits
        // above their gradient on 14 and older.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        // The user's rotation choice (portrait by default); changes are applied from the UI below.
        requestedOrientation = orientationFor(resources.configuration)
        setupNeeded = com.music.spotui.ui.screens.Setup.needed(this)
        // No account to sign in to: the app opens once the welcome screen and the music account
        // choice are behind (a TV has no choice to make).
        accountChosen = tv || com.music.spotui.ui.screens.Setup.accountSeen(this)
        signedIn = accountChosen
        // A TV has one look of its own and no setup to go through (no battery or file prompts there).
        if (tv) setupNeeded = false
        root = FrameLayout(this)
        val compose = ComposeView(this).apply {
            setContent {
                SpotuiTheme { com.music.spotui.ui.look.ProvideLook { com.music.spotui.ui.components.ProvideWindowSize {
                    val orientation = com.music.spotui.data.preferences.OrientationPref.mode
                    // Re-checked when the window changes (unfolded, moved to DeX, split screen).
                    val config = androidx.compose.ui.platform.LocalConfiguration.current
                    androidx.compose.runtime.LaunchedEffect(orientation, config.smallestScreenWidthDp, config.screenLayout) {
                        requestedOrientation = orientationFor(config)
                    }
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                        val entry = com.music.spotui.ui.screens.Onboarding.entry(signedIn, setupNeeded, choosingAccount, tv)
                        when {
                            showLook -> com.music.spotui.ui.look.LookStep(onNext = {
                                com.music.spotui.data.preferences.LookPref.setChosen(this@MainActivity); showLook = false
                                setupNeeded = com.music.spotui.ui.screens.Setup.needed(this@MainActivity)
                            })
                            entry == com.music.spotui.ui.screens.Entry.SETUP -> com.music.spotui.ui.screens.SetupScreen(
                                onDone = { setupNeeded = false },
                            )
                            entry == com.music.spotui.ui.screens.Entry.APP -> if (tv) com.music.spotui.ui.tv.TvApp() else App()
                            entry == com.music.spotui.ui.screens.Entry.ACCOUNT -> com.music.spotui.ui.screens.AccountChoiceScreen(
                                onBack = { choosingAccount = false },
                                onDone = { accountDone() },
                            )
                            else -> WelcomeScreen(onLogin = { choosingAccount = true })
                        }
                        // Existing users, once, right after the update: the new look, live. Never after the
                        // onboarding, whose look step asked it already (LookPref.offered, read live).
                        if (com.music.spotui.ui.screens.Onboarding.lookOfferShown(
                                entry, showLook, false, inPip, com.music.spotui.data.preferences.LookPref.offered, tv)) {
                            if (com.music.spotui.data.preferences.LookPref.look == com.music.spotui.data.preferences.Look.IMMERSIVE) {
                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                    com.music.spotui.data.preferences.LookPref.setOffered(this@MainActivity)
                                }
                            } else {
                                val close = { look: com.music.spotui.data.preferences.Look ->
                                    com.music.spotui.data.preferences.LookPref.set(this@MainActivity, look)
                                    com.music.spotui.data.preferences.LookPref.setOffered(this@MainActivity)
                                }
                                val keep = { close(com.music.spotui.data.preferences.Look.CLASSIC) }
                                androidx.compose.ui.window.Dialog(
                                    onDismissRequest = keep,
                                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
                                ) {
                                    com.music.spotui.ui.look.LookStep(
                                        onNext = { close(com.music.spotui.data.preferences.Look.IMMERSIVE) },
                                        offer = com.music.spotui.ui.look.LookOffer(onKeep = keep),
                                    )
                                }
                            }
                        }
                        // A pass QR code scanned with the camera: the yes before sending this phone's setup.
                        if (signedIn) com.music.spotui.pass.PassLinkHost()
                        // "Ask each time": the download quality question, above every screen.
                        com.music.spotui.ui.components.DownloadAskHost()
                        // An extension asking the user's yes before a change.
                        com.music.spotui.extensions.ExtensionConfirmHost()
                        // An extension's sign-in page, in a web view of its own.
                        com.music.spotui.extensions.ExtensionAuthHost()
                        // Beta and dev builds only: hold the faint dot to copy the recent logs.
                        if (com.music.spotui.data.api.UpdateChecker.isBeta || app.myspoti.engine.BuildConfig.DEBUG)
                            com.music.spotui.ui.components.BetaLogButton(
                                androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomEnd))
                        // Picture-in-picture: the clip alone, over the app (which stays composed underneath,
                        // so coming back finds the same screen).
                        if (inPip) {
                            if (pipSong) com.music.spotui.ui.components.PipSong() else com.music.spotui.ui.components.PipVideo()
                        }
                    }
                } } }
            }
        }
        root.addView(compose, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        // Haptics (buttons, likes, the debug icon) go through this window.
        com.music.spotui.di.Haptics.attach(compose)
        val debuggable = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        // The hidden page of the YouTube Music account, when chosen.
        com.music.spotui.di.YtmRuntime.attach(root)
        showLook = debuggable && intent.getBooleanExtra("showLook", false)
        // Debug builds: `am start ... --ez resetLookOffer true` shows the new-look popup again.
        if (debuggable && intent.getBooleanExtra("resetLookOffer", false)) {
            com.music.spotui.data.preferences.LookPref.clearOffered(this)
        }
        // Only on a fresh start: on recreation (theme, locale, process restore)
        // the same intent would play its link again. New links arrive in onNewIntent.
        if (savedInstanceState == null) handle(intent)
    }

    /**
     * The rotation setting applies to phones only: tablets, unfolded foldables, split screen and
     * Samsung DeX windows follow the window (a forced portrait there letterboxes or rotates a
     * desktop window).
     */
    private fun orientationFor(config: android.content.res.Configuration): Int {
        val large = tv || config.smallestScreenWidthDp >= 600 || isInMultiWindowMode || isDesktopMode(config)
        return if (large) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else com.music.spotui.data.preferences.OrientationPref.requested()
    }

    /** Samsung DeX (its own Configuration field), or a desktop-class window (Android 16 desktop windowing). */
    private fun isDesktopMode(config: android.content.res.Configuration): Boolean {
        val dex = runCatching {
            val c = config.javaClass
            c.getField("SEM_DESKTOP_MODE_ENABLED").getInt(c) == c.getField("semDesktopModeEnabled").getInt(config)
        }.getOrDefault(false)
        return dex || (config.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_DESK
    }

    /**
     * Keyboard shortcuts with a keyboard attached (DeX, tablets): keys the pages leave unused
     * (a text field takes its own space, a focused button its own Enter). Space plays or pauses,
     * media keys drive the player, Ctrl + arrows (and plain arrows nothing else took) seek 10 s.
     */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (signedIn && !inPip && handleShortcut(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    // Ctrl + arrows before the pages: plain arrows move the keyboard focus there.
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN && event.isCtrlPressed && signedIn && !inPip &&
            (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT || event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
            && handleShortcut(event.keyCode, event)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun handleShortcut(keyCode: Int, event: android.view.KeyEvent): Boolean {
        val player = com.music.spotui.di.SongPlayer
        if (com.music.spotui.di.Graph.currentSongState.title.value.isBlank()) return false
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_SPACE, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (event.repeatCount == 0) { if (player.isPlaying()) player.pause(by = "keyboard") else player.play(by = "keyboard") }
            }
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> if (event.repeatCount == 0) player.play(by = "keyboard")
            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> if (event.repeatCount == 0) player.pause(by = "keyboard")
            android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> if (event.repeatCount == 0) player.next(by = "keyboard")
            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> if (event.repeatCount == 0) player.previous(by = "keyboard")
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> player.skipBy(-10_000L)
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> player.skipBy(10_000L)
            else -> return false
        }
        return true
    }

    // Leaving the app (Home, recents) while a music video plays: it goes on in a small window.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val mv = com.music.spotui.video.MusicVideoController
        if (!com.music.spotui.di.SongPlayer.isPlaying() || isInPictureInPictureMode) return
        if (!com.music.spotui.data.preferences.MusicVideoPref.on || !mv.on.value || mv.player == null) {
            // No clip: the song itself, when the Picture-in-picture setting is on.
            // Not on a TV: the app there is a full-screen player already.
            if (tv || !signedIn || !com.music.spotui.ui.components.SongPip.wanted()) return
            pipSong = true
            if (!runCatching { enterPictureInPictureMode(com.music.spotui.ui.components.SongPip.params(this)) }.getOrDefault(false)) pipSong = false
            return
        }
        pipSong = false
        val ratio = mv.aspect.floatValue.coerceIn(0.5f, 2.3f)
        runCatching {
            enterPictureInPictureMode(
                android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational((ratio * 1000).toInt(), 1000))
                    .build(),
            )
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
        if (!pipSong) com.music.spotui.video.MusicVideoController.inPip.value = isInPictureInPictureMode
        if (isInPictureInPictureMode && pipSong) {
            if (pipReceiver == null) pipReceiver = com.music.spotui.ui.components.SongPip.listen(this)
            // Play and pause swap on the window's middle button as the music stops and starts.
            pipWatch?.cancel()
            pipWatch = lifecycleScope.launch {
                androidx.compose.runtime.snapshotFlow { com.music.spotui.di.Graph.currentSongState.playingState.value }.collect {
                    runCatching { setPictureInPictureParams(com.music.spotui.ui.components.SongPip.params(this@MainActivity)) }
                }
            }
        } else if (!isInPictureInPictureMode) {
            pipWatch?.cancel(); pipWatch = null
            pipReceiver?.let { runCatching { unregisterReceiver(it) } }
            pipReceiver = null
            pipSong = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    // The clip player follows the app on screen: paused with its side jobs in the background.
    override fun onStart() {
        // Dev builds only: it woke two threads every half second while the app was on screen, for a
        // freeze report no one using the app reads (the beta too: "checks that run all the time for nothing").
        if (app.myspoti.engine.BuildConfig.DEBUG) app.myspoti.host.android.MainThreadWatch.start()
        super.onStart()
        com.music.spotui.di.AppForeground.onForeground()
        com.music.spotui.video.MusicVideoController.onForeground()
        com.music.spotui.ui.components.CanvasPlayer.onForeground()
        // The song shown may have been liked or unliked in another app meanwhile.
        com.music.spotui.di.SongPlayer.refreshLikedIfStale()
        // Asked for over adb (TestControlReceiver's "screenon"): One UI ignores the huge
        // `screen_off_timeout`, so a long unattended run holds the screen on from in here.
        if (prefs.getBoolean("keep_screen_on", false)) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        app.myspoti.host.android.MainThreadWatch.stop()
        if (!isChangingConfigurations) {
            com.music.spotui.di.AppForeground.onBackground()
            com.music.spotui.video.MusicVideoController.onBackground()
            com.music.spotui.ui.components.CanvasPlayer.onBackground()
        }
    }

    override fun onDestroy() {
        pipReceiver?.let { runCatching { unregisterReceiver(it) } }
        pipReceiver = null
        // The YouTube Music page lives in this activity's root while attached: detached, or it keeps the activity alive.
        com.music.spotui.di.YtmRuntime.detach(root)
        super.onDestroy()
    }

    /** The account choice's Continue: the setup and the app (YouTube Music signs in on its own page first). */
    private fun accountDone() {
        com.music.spotui.ui.screens.Setup.setAccountSeen(this)
        accountChosen = true
        choosingAccount = false
        signedIn = true
        Logbook.log("onboarding: music account ${com.music.spotui.data.preferences.AccountProviderPref.id} chosen")
    }

    /** A pass QR code (orpheon://pass?…) asks whether to send this phone's setup; voice search plays its best match. */
    private fun handle(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val link = intent.data?.toString() ?: return
                // A pass QR code opened by the camera (orpheon://pass?…): its confirmation, never a send on its own.
                com.music.spotui.pass.PassLinks.offer(this, link, signedIn)
            }
            android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH -> {
                val query = intent.getStringExtra(android.app.SearchManager.QUERY).orEmpty()
                if (query.isNotBlank()) lifecycleScope.launch { AutoBrowser.playFromSearch(query) }
            }
        }
    }
}
