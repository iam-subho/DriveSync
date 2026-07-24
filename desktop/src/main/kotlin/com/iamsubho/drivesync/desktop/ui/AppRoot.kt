package com.iamsubho.drivesync.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamsubho.drivesync.desktop.AppState
import kotlinx.coroutines.delay

@Composable
fun AppRoot(appState: AppState) {
    val config by appState.config.collectAsState()
    val provider by appState.provider.collectAsState()
    val notice by appState.notice.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(4_000)
            appState.consumeNotice()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DsColors.Bg),
    ) {
        when {
            !config.oauth.isConfigured || (provider == null && config.account == null) ->
                OnboardingScreen(appState)
            showSettings -> SettingsScreen(appState, onBack = { showSettings = false })
            else -> MainScreen(appState, onOpenSettings = { showSettings = true })
        }

        notice?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DsColors.Snack)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(message, fontSize = 14.sp, color = DsColors.SnackFg)
            }
        }
    }
}

/** First-run: paste the Desktop OAuth client, then sign in with Google. */
@Composable
fun OnboardingScreen(appState: AppState) {
    val config by appState.config.collectAsState()
    val busy by appState.busy.collectAsState()
    val authUrl by appState.authUrl.collectAsState()
    var clientId by remember { mutableStateOf(config.oauth.clientId) }
    var clientSecret by remember { mutableStateOf(config.oauth.clientSecret) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CloudSync, contentDescription = null,
            tint = DsColors.Primary, modifier = Modifier.size(56.dp),
        )
        Text(
            "DriveSync for Windows",
            fontSize = 26.sp, fontWeight = FontWeight.Bold, color = DsColors.TextPrimary,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "Keep folders on this PC in sync with your Google Drive.",
            fontSize = 14.sp, color = DsColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
        )

        Column(modifier = Modifier.widthIn(max = 560.dp)) {
            Text(
                "1 · Paste your Google OAuth Desktop client",
                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary,
            )
            Text(
                "Google Cloud Console → APIs & Services → Credentials → Create credentials → " +
                    "OAuth client ID → Application type: Desktop app. Paste its ID and secret here " +
                    "(stored only on this PC).",
                fontSize = 12.5.sp, color = DsColors.TextTertiary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = clientId,
                onValueChange = { clientId = it },
                label = { Text("Client ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = clientSecret,
                onValueChange = { clientSecret = it },
                label = { Text("Client secret") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            Text(
                "2 · Sign in with Google",
                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        appState.saveOAuthConfig(clientId, clientSecret)
                        appState.signIn()
                    },
                    enabled = clientId.isNotBlank() && clientSecret.isNotBlank() && !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = DsColors.Primary),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(if (busy) "Waiting for Google…" else "Sign in with Google", color = Color.White)
                }
                if (busy) {
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { appState.cancelSignIn() }) {
                        Text("Cancel", color = DsColors.Error)
                    }
                } else {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Your browser opens for Google's consent screen.",
                        fontSize = 12.sp, color = DsColors.TextTertiary,
                    )
                }
            }

            // Manual fallback: the consent URL is always available here, so a failed
            // browser launch can never strand the user.
            authUrl?.let { url ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DsColors.Card)
                        .padding(14.dp),
                ) {
                    Text(
                        "Browser didn't open? Use this link:",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary,
                    )
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            url,
                            fontSize = 11.sp, color = DsColors.TextSecondary,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        TextButton(onClick = {
                            com.iamsubho.drivesync.desktop.auth.DesktopAuth.openBrowser(url)
                        }) { Text("Open in browser", color = DsColors.Primary) }
                        TextButton(onClick = {
                            val selection = java.awt.datatransfer.StringSelection(url)
                            java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                .setContents(selection, selection)
                            appState.showNotice("Link copied")
                        }) { Text("Copy link", color = DsColors.Primary) }
                    }
                }
            }
        }
    }
}

/** Settings: OAuth client + account. */
@Composable
fun SettingsScreen(appState: AppState, onBack: () -> Unit) {
    val config by appState.config.collectAsState()
    var clientId by remember { mutableStateOf(config.oauth.clientId) }
    var clientSecret by remember { mutableStateOf(config.oauth.clientSecret) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = DsColors.Primary) }
            Text(
                "Settings",
                fontSize = 20.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Column(modifier = Modifier.widthIn(max = 560.dp).padding(top = 16.dp)) {
            Text("Google account", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            ) {
                Text(
                    config.account?.email ?: "Not signed in",
                    fontSize = 14.sp, color = DsColors.TextSecondary,
                )
                Spacer(Modifier.width(16.dp))
                if (config.account != null) {
                    TextButton(onClick = { appState.signOut(); onBack() }) {
                        Text("Sign out", color = DsColors.Error)
                    }
                } else {
                    TextButton(onClick = { appState.signIn() }) {
                        Text("Sign in", color = DsColors.Primary)
                    }
                }
            }

            Text("Startup", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary)
            if (com.iamsubho.drivesync.desktop.WindowsStartup.isSupported) {
                var startWithWindows by remember {
                    mutableStateOf(com.iamsubho.drivesync.desktop.WindowsStartup.isEnabled())
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                ) {
                    androidx.compose.material3.Switch(
                        checked = startWithWindows,
                        onCheckedChange = { wanted ->
                            if (com.iamsubho.drivesync.desktop.WindowsStartup.setEnabled(wanted)) {
                                startWithWindows = wanted
                            } else {
                                appState.showNotice("Could not update Windows startup setting")
                            }
                        },
                    )
                    Text(
                        "Start DriveSync when Windows starts (minimized to tray)",
                        fontSize = 13.5.sp, color = DsColors.TextSecondary,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            } else {
                Text(
                    "Start-with-Windows is available in the installed app (MSI/portable build).",
                    fontSize = 12.5.sp, color = DsColors.TextTertiary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                )
            }

            Text("OAuth Desktop client", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary)
            OutlinedTextField(
                value = clientId, onValueChange = { clientId = it },
                label = { Text("Client ID") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = clientSecret, onValueChange = { clientSecret = it },
                label = { Text("Client secret") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = { appState.saveOAuthConfig(clientId, clientSecret); onBack() },
                colors = ButtonDefaults.buttonColors(containerColor = DsColors.Primary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Save", color = Color.White) }
        }
    }
}
