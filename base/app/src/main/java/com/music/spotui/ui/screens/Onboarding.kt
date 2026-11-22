/* SPDX-License-Identifier: GPL-3.0-only */
package com.music.spotui.ui.screens

import app.myspoti.api.account.AccountProviders

/** What the main activity puts on screen. */
enum class Entry {
    /** The one-time steps once the music account is chosen ([SetupScreen]). */
    SETUP,
    /** The app's pages. */
    APP,
    /** The music account choice ([AccountChoiceScreen]). */
    ACCOUNT,
    /** The first screen of a new install. */
    WELCOME,
}

/** What follows the music account chosen. */
enum class AfterChoice {
    /** YouTube Music's own sign-in page, then the app. */
    YTM_SIGN_IN,
    /** The app, no sign-in asked. */
    APP,
}

/**
 * The first launch's order: the welcome screen, the music account (YouTube Music or none), the
 * setup, the app. There is no account of the app's own to sign in to. A TV opens straight on its app.
 */
object Onboarding {
    /** [open]: past the welcome screen and the account choice. */
    fun entry(open: Boolean, setupNeeded: Boolean, choosingAccount: Boolean, tv: Boolean): Entry = when {
        open && setupNeeded -> Entry.SETUP
        open || tv -> Entry.APP
        choosingAccount -> Entry.ACCOUNT
        else -> Entry.WELCOME
    }

    /** The welcome screen's button leads to the account choice. */
    fun welcomeChoosesAccount(tv: Boolean): Boolean = !tv

    /** Continue on the account choice: YouTube Music's own sign-in while signed out, else the app. */
    fun afterChoice(provider: String, ytmSignedIn: Boolean): AfterChoice = when (provider) {
        AccountProviders.YTMUSIC -> if (ytmSignedIn) AfterChoice.APP else AfterChoice.YTM_SIGN_IN
        else -> AfterChoice.APP
    }

    /**
     * The "new look" popup, for an existing user who never saw it: never over the onboarding, nor
     * after it (its look step covers it, [com.music.spotui.data.preferences.LookPref.setChosen]).
     */
    fun lookOfferShown(entry: Entry, showLook: Boolean, showingLogin: Boolean, inPip: Boolean, offered: Boolean, tv: Boolean): Boolean =
        entry == Entry.APP && !showLook && !showingLogin && !inPip && !offered && !tv
}
