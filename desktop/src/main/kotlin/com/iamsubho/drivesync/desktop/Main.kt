package com.iamsubho.drivesync.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.iamsubho.drivesync.desktop.ui.AppRoot
import com.iamsubho.drivesync.desktop.ui.DriveSyncTheme

fun main(args: Array<String>) = application {
    val appState = remember { AppState() }
    // --minimized: used by "Start with Windows" to boot straight into the tray.
    var windowVisible by remember { mutableStateOf("--minimized" !in args) }
    val windowState = rememberWindowState(size = DpSize(1000.dp, 720.dp))
    val trayIcon = rememberVectorPainter(Icons.Filled.CloudSync)

    // Closing the window hides to the tray; sync keeps running. Exit via the tray menu.
    Tray(
        icon = trayIcon,
        tooltip = "DriveSync",
        onAction = { windowVisible = true },
        menu = {
            Item("Open DriveSync") { windowVisible = true }
            Item("Sync all now") { appState.syncAllNow() }
            Separator()
            Item("Exit") { exitApplication() }
        },
    )

    Window(
        onCloseRequest = { windowVisible = false },
        visible = windowVisible,
        state = windowState,
        title = "DriveSync",
        icon = trayIcon,
    ) {
        DriveSyncTheme {
            AppRoot(appState)
        }
    }
}
