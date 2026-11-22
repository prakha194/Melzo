# What this build changes

This is a modified build of [Orpheon](https://github.com/kota-zo/Orpheon) by **kota-zo**, which is
free software under the GNU GPL v3. It is **not** the official release, and its author is not
responsible for it. The GPL asks a modified version to say so, and to keep its source available:
both are done here.

Changes, all in the app module:

1. **No update card.** `UpdatePrompt()` is no longer called from `App.kt`, so no "A new update is
   available" popup is drawn and nothing is downloaded or installed from inside the app.
2. **No donation reminder.** `SupportPrompt()` is no longer called: the card that asked for a
   donation a few seconds after launch is gone.
3. **No support entry.** The "Support" row (and its Ko-fi / GitHub Sponsors sheet) is removed from
   the profile sheet in both looks — `HomeScreen.kt` (Classic) and `ImmersiveHomeProfile.kt`
   (Immersive). `SupportSheet` itself is removed from `AboutSheet.kt`.
4. **About, rebuilt.** `AboutSheet.kt` now shows the app icon, the version, the credit to the owner
   (kota-zo, GNU GPL v3, "this APK is a modified build") and two buttons:
   - **Support** → https://github.com/prakha194/
   - **Source code** → https://github.com/kota-zo/Orpheon
   The same credit and links replace the update/diagnostics rows of Settings → App, in both looks
   (`SettingsScreen.kt`, `ImmersiveSettingsScreen.kt`).
5. **No developer popups.** The "Stats for nerds" sheet is no longer reachable from a song's
   options (`SongOptionsSheet.kt`, `ImmersiveSongOptionsSheet.kt`); the diagnostic-report export is
   gone from Settings; the beta log dot and the one-time "new look" popup are disabled in
   `MainActivity.kt`.
6. **New launcher icon** (`drawable-*dpi/ic_launcher_foreground.png`, `drawable/ic_launcher_background.xml`).

Everything else is upstream Orpheon 1.0.0, unchanged.
