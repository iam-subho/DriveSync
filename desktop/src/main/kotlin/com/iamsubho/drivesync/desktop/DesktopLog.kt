package com.iamsubho.drivesync.desktop

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Minimal file logger — packaged .exe apps have no visible stdout. */
object DesktopLog {
    @Volatile
    var file: File? = null

    fun init(baseDir: File) {
        file = File(baseDir, "desktop.log")
    }

    @Synchronized
    fun log(message: String, error: Throwable? = null) {
        try {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val text = buildString {
                append("[$ts] $message\n")
                error?.let { append(it.stackTraceToString()).append('\n') }
            }
            file?.appendText(text)
        } catch (_: Exception) {
        }
    }
}
