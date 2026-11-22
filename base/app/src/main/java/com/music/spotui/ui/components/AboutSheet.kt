package com.music.spotui.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.AppMuted

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.BuildConfig
import app.myspoti.engine.R
import com.music.spotui.data.api.SupportReminder
import com.music.spotui.ui.theme.AppAccent
import kotlinx.coroutines.launch

private val repo get() = BuildConfig.UPDATE_REPO.ifBlank { "Kirisos-Guna/myspoti" }
val REPO_URL get() = "https://github.com/prakha194/Spotify"

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** About: the version, the source code and a star on GitHub. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    com.music.spotui.extensions.ProtectedSurfaceEffect(app.myspoti.extensions.ProtectedSurface.ABOUT)
    // Opened in full: half open, it hid the support buttons at the bottom.
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
                    Text("${stringResource(R.string.settings_item_version_title)} ${BuildConfig.VERSION_NAME}",
                        color = AppMuted, fontSize = 13.sp)
                }
            }
            Text(stringResource(R.string.about_free), color = AppMuted, fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp))
            Text("Spoon is made by Prashant.", color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
            Text("Free and open source, under the GNU GPL v3. Based on Myspoti/Orpheon by kota-zo.",
                color = AppMuted, fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp))
            AboutRow("Source code", "github.com/prakha194/Spotify") { openUrl(context, REPO_URL) }
        }
    }
}

/**
 * "Update channel" (stable or beta, chosen in a dialog) and "Check for updates" (asks GitHub now;
 * a newer release opens the app's update card).
 */
@Composable
private fun UpdateRows() {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val check = com.music.spotui.data.api.rememberUpdateCheckState(context)
    var picking by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val beta = com.music.spotui.data.api.UpdateChecker.isBeta
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { picking = true }.padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.update_channel_title), color = AppText, fontSize = 16.sp)
            Text(stringResource(R.string.update_channel_detail), color = AppMuted, fontSize = 12.sp)
        }
        Text(stringResource(if (beta) R.string.update_channel_beta else R.string.update_channel_stable),
            color = AppText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
    AboutRow(
        stringResource(R.string.settings_update_refresh),
        when {
            check.checking -> "…"
            check.message != null -> check.message!!
            else -> stringResource(R.string.update_check_detail)
        },
    ) { scope.launch { check.run() } }
    if (picking) UpdateChannelDialog(beta, onPick = { com.music.spotui.data.api.UpdateChecker.setBetaChannel(context, it); picking = false }) { picking = false }
}

@Composable
internal fun UpdateChannelDialog(beta: Boolean, onPick: (Boolean) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(tone(0xFF1E1E1E))
                .border(1.dp, AppText.copy(alpha = 0.12f), RoundedCornerShape(28.dp)).padding(24.dp),
        ) {
            Text(stringResource(R.string.update_channel_title), color = AppText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            ChannelOption(stringResource(R.string.update_channel_stable), stringResource(R.string.update_channel_stable_detail), !beta) { onPick(false) }
            ChannelOption(stringResource(R.string.update_channel_beta), stringResource(R.string.update_channel_beta_detail), beta) { onPick(true) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(AppText.copy(alpha = 0.08f)).clickable(onClick = onDismiss),
            ) { Text(stringResource(R.string.update_channel_cancel), color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ChannelOption(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected, onClick = onClick,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = AppText, unselectedColor = AppMuted),
        )
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (detail.isNotEmpty()) Text(detail, color = AppMuted, fontSize = 13.sp)
        }
    }
}

/** "Star on GitHub": opens the repository, where a star helps others find the app. */
@Composable
fun StarOnGitHubButton(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50))
            .background(AppAccent).pressable(0.96f) { onClick?.invoke(); openUrl(context, REPO_URL) },
    ) { Text("★ " + stringResource(R.string.about_star_action), color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

/** "Support Myspoti" from the profile menu: a star on GitHub. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    com.music.spotui.extensions.ProtectedSurfaceEffect(app.myspoti.extensions.ProtectedSurface.SUPPORT)
    LookSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = tone(0xFF282828),
    ) {
        androidx.compose.foundation.layout.Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(stringResource(R.string.support_title), color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.about_star_body), color = AppMuted, fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 18.dp))
            StarOnGitHubButton { onDismiss() }
        }
    }
}

@Composable
private fun AboutRow(title: String, detail: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(title, color = AppText, fontSize = 16.sp)
        Text(detail, color = AppMuted, fontSize = 12.sp)
    }
}

/** The refresh-rate row's value: "Automatic" or "120 Hz". */
@Composable
fun refreshRateLabel(): String = com.music.spotui.ui.look.RefreshRate.chosen.let { hz ->
    if (hz == 0) stringResource(R.string.refresh_rate_auto) else stringResource(R.string.refresh_rate_hz, hz)
}

/** Picks the screen refresh rate (see RefreshRate): automatic, or one the screen offers. */
@Composable
internal fun RefreshRateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
    val rates = androidx.compose.runtime.remember { activity?.let { com.music.spotui.ui.look.RefreshRate.supported(it) }.orEmpty() }
    val chosen = com.music.spotui.ui.look.RefreshRate.chosen
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(tone(0xFF1E1E1E))
                .border(1.dp, AppText.copy(alpha = 0.12f), RoundedCornerShape(28.dp)).padding(24.dp),
        ) {
            Text(stringResource(R.string.refresh_rate_title), color = AppText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.refresh_rate_detail), color = AppMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            ChannelOption(stringResource(R.string.refresh_rate_auto), stringResource(R.string.refresh_rate_auto_detail), chosen == 0) {
                com.music.spotui.ui.look.RefreshRate.set(context, 0); onDismiss()
            }
            rates.forEach { hz ->
                ChannelOption(stringResource(R.string.refresh_rate_hz, hz), "", chosen == hz) {
                    com.music.spotui.ui.look.RefreshRate.set(context, hz); onDismiss()
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(AppText.copy(alpha = 0.08f)).clickable(onClick = onDismiss),
            ) { Text(stringResource(R.string.update_channel_cancel), color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
