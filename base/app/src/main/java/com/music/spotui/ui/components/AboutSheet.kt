package com.music.spotui.ui.components

import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.AppMuted

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.BuildConfig
import app.myspoti.engine.R
import com.music.spotui.ui.theme.AppAccent

/** The two links this build's About offers: the builder's support page and the owner's source code. */
const val SUPPORT_URL = "https://github.com/prakha194/"
const val SOURCE_URL = "https://github.com/kota-zo/Orpheon"

/** Opens [url] outside the app; a browser missing is not worth a crash. */
fun openExternal(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * Who made Spoon, and what this build is: the credit the GPL asks for, and the honest note that
 * this APK is a modified build and not the project's official release.
 */
@Composable
fun AboutCredit(modifier: Modifier = Modifier, muted: Color = AppMuted) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Spoon is made by Prashant.", color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Free and open source, under the GNU GPL v3. Based on Orpheon by kota-zo.",
            color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Text("This APK is a modified build, not an official release.",
            color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The About buttons: support (this build's builder) and the owner's source code. */
@Composable
fun AboutButtons(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth().height(48.dp)
                .clip(RoundedCornerShape(50)).background(AppAccent).pressable(0.96f) { openExternal(context, SUPPORT_URL) },
        ) { Text("Support", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp).fillMaxWidth().height(48.dp)
                .clip(RoundedCornerShape(50)).border(1.dp, AppText.copy(alpha = 0.6f), RoundedCornerShape(50))
                .pressable(0.96f) { openExternal(context, SOURCE_URL) },
        ) { Text("Source code", color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

/** About: the version, who made the app, and two buttons (support, source code). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    com.music.spotui.extensions.ProtectedSurfaceEffect(app.myspoti.extensions.ProtectedSurface.ABOUT)
    // Opened in full: half open, it hid the buttons at the bottom.
    LookSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = tone(0xFF282828),
    ) {
        Column(Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
                Image(
                    painterResource(R.drawable.ic_launcher_foreground), contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(tone(0xFF121212)),
                )
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Spoon", color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${stringResource(R.string.lbl_version)} ${BuildConfig.VERSION_NAME}",
                        color = AppMuted, fontSize = 13.sp)
                }
            }
            AboutCredit(Modifier.padding(top = 12.dp))
            AboutButtons(Modifier.padding(top = 4.dp))
        }
    }
}

/** GitHub Sponsors, under a filled button: outlined, so it stays the second one. */
@Composable
fun GitHubSponsorButton(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50))
            .border(1.dp, AppText.copy(alpha = 0.6f), RoundedCornerShape(50))
            .pressable(0.96f) { onClick?.invoke() ?: com.music.spotui.data.api.SupportReminder.openSponsors(context) },
    ) { Text(stringResource(R.string.support_github_action), color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

