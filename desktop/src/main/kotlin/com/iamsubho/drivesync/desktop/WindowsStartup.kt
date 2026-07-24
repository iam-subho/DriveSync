package com.iamsubho.drivesync.desktop

/**
 * "Start with Windows" via the per-user registry Run key. A true Windows *service* is not
 * viable for this app: services run in session 0 with no UI or browser (OAuth sign-in would
 * be impossible) and jpackage apps can't register as services without an external wrapper.
 * Auto-start at login + system tray gives the same practical result while the user is
 * logged in.
 */
object WindowsStartup {

    private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val VALUE_NAME = "DriveSync"

    /** Path of the packaged DriveSync.exe launcher; null when running from Gradle/IDE. */
    private fun launcherPath(): String? {
        val command = ProcessHandle.current().info().command().orElse(null) ?: return null
        val lower = command.lowercase()
        return command.takeIf {
            lower.endsWith(".exe") && !lower.endsWith("java.exe") && !lower.endsWith("javaw.exe")
        }
    }

    val isSupported: Boolean
        get() = System.getProperty("os.name", "").startsWith("Windows") && launcherPath() != null

    fun isEnabled(): Boolean = try {
        val process = ProcessBuilder("reg", "query", RUN_KEY, "/v", VALUE_NAME)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor() == 0 && output.contains(VALUE_NAME)
    } catch (e: Exception) {
        false
    }

    fun setEnabled(enabled: Boolean): Boolean = try {
        if (enabled) {
            val exe = launcherPath() ?: return false
            ProcessBuilder(
                "reg", "add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ",
                "/d", "\"$exe\" --minimized", "/f",
            ).start().waitFor() == 0
        } else {
            ProcessBuilder("reg", "delete", RUN_KEY, "/v", VALUE_NAME, "/f")
                .start().waitFor() == 0
        }
    } catch (e: Exception) {
        false
    }
}
