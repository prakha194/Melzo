/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.myspoti.api.account.AccountProviders
import app.myspoti.engine.R
import com.music.spotui.data.preferences.Look
import com.music.spotui.data.preferences.LookPref
import com.music.spotui.di.Graph
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The first launch: the music account is chosen (none or YouTube Music), with no sign-in of the
 * app's own; someone already set up is asked nothing again, and the "new look" popup never
 * follows an onboarding whose look step asked it already (it stays for an existing user who never saw it).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val app get() = RuntimeEnvironment.getApplication()
    private fun prefs(name: String) = app.getSharedPreferences(name, 0)

    @Before fun setUp() {
        Graph::class.java.getDeclaredField("app").apply { isAccessible = true }.set(null, app)
        prefs("myspoti_prefs").edit().clear().commit()
        prefs("settings_prefs").edit().clear().commit()
        LookPref.clearOffered(app)
    }

    @After fun tearDown() { LookPref.set(app, Look.CLASSIC) }

    private fun entry(open: Boolean, setup: Boolean = true, choosing: Boolean = false, tv: Boolean = false) =
        Onboarding.entry(open, setup, choosing, tv)

    @Test fun `a new install chooses its music account, then the setup`() {
        // Welcome first; its button leads to the account choice. No sign-in anywhere.
        assertEquals(Entry.WELCOME, entry(open = false))
        assertTrue(Onboarding.welcomeChoosesAccount(tv = false))
        assertEquals(Entry.ACCOUNT, entry(open = false, choosing = true))
        // Chosen: the setup, which no longer asks the account.
        Setup.setAccountSeen(app)
        assertEquals(Entry.SETUP, entry(open = true))
        assertFalse(STEP_ACCOUNT in Setup.steps(app))
    }

    @Test fun `no account or YouTube Music, and YouTube Music signs in on its own page`() {
        assertEquals(AfterChoice.APP, Onboarding.afterChoice(AccountProviders.NONE, ytmSignedIn = false))
        assertEquals(AfterChoice.YTM_SIGN_IN, Onboarding.afterChoice(AccountProviders.YTMUSIC, ytmSignedIn = false))
        assertEquals(AfterChoice.APP, Onboarding.afterChoice(AccountProviders.YTMUSIC, ytmSignedIn = true))
        // A Spotify account chosen by an earlier version: none now.
        assertEquals(AccountProviders.NONE, AccountProviders.normalized("spotify"))
        assertFalse("spotify" in AccountProviders.OFFERED)
    }

    @Test fun `someone already set up is asked nothing again`() {
        prefs("myspoti_prefs").edit().putBoolean("SetupDone", true).putBoolean("SetupExtensionsSeen", true)
            .putBoolean("SetupAccountSeen", true).commit()
        com.music.spotui.data.preferences.setQualityChosen(app)
        LookPref.setChosen(app)
        assertFalse(Setup.needed(app))
        assertEquals(Entry.APP, entry(open = true, setup = false))
    }

    @Test fun `a TV opens straight on its app`() {
        assertFalse(Onboarding.welcomeChoosesAccount(tv = true))
        assertEquals(Entry.APP, entry(open = false, setup = false, tv = true))
    }

    @Test fun `the new look popup never follows the onboarding`() {
        // A new install: not offered yet. Never over the onboarding's steps.
        assertFalse(LookPref.offered)
        assertFalse(Onboarding.lookOfferShown(Entry.SETUP, false, false, false, LookPref.offered, tv = false))
        assertFalse(Onboarding.lookOfferShown(Entry.WELCOME, false, false, false, LookPref.offered, tv = false))
        // The look step's Continue: the popup is spent at once, without the activity starting again.
        LookPref.setChosen(app)
        assertTrue(LookPref.offered)
        assertFalse(Onboarding.lookOfferShown(Entry.APP, false, false, false, LookPref.offered, tv = false))
        assertTrue(LookPref.isOffered(app))
    }

    @Test fun `an existing user who never saw the popup still gets it, once`() {
        LookPref.clearOffered(app)
        assertTrue(Onboarding.lookOfferShown(Entry.APP, false, false, false, LookPref.offered, tv = false))
        // Not over a sign-in page, the picture-in-picture window, or on a TV.
        assertFalse(Onboarding.lookOfferShown(Entry.APP, false, true, false, LookPref.offered, tv = false))
        assertFalse(Onboarding.lookOfferShown(Entry.APP, false, false, true, LookPref.offered, tv = false))
        assertFalse(Onboarding.lookOfferShown(Entry.APP, false, false, false, LookPref.offered, tv = true))
        LookPref.setOffered(app)
        assertFalse(Onboarding.lookOfferShown(Entry.APP, false, false, false, LookPref.offered, tv = false))
    }

    /** The account choice: "none" is offered, Continue hands over. */
    private fun accountChoice(look: Look) {
        LookPref.set(app, look)
        var done = 0
        var back = 0
        rule.setContent {
            com.music.spotui.ui.look.ProvideLook {
                AccountChoiceScreen(onBack = { back++ }, onDone = { done++ })
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText(app.getString(R.string.setup_account_title)).assertExists()
        rule.onNodeWithText(app.getString(R.string.setup_account_none_detail)).assertExists()
        rule.onNodeWithText(app.getString(R.string.setup_continue)).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(1, done)
        assertEquals(0, back)
    }

    @Test fun `the account choice, Classic`() = accountChoice(Look.CLASSIC)
    @Test fun `the account choice, Immersive`() = accountChoice(Look.IMMERSIVE)
}
