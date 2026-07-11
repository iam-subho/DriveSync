package com.iamsubho.drivesync.presentation.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

/** Display formatting helpers matching the design prototype's copy exactly. */
object Formatters {

    private const val KB = 1024L
    private const val MB = KB * 1024
    private const val GB = MB * 1024

    fun formatBytes(bytes: Long): String = when {
        bytes >= GB -> "%.1f GB".format(Locale.US, bytes.toDouble() / GB)
        bytes >= MB -> "%.1f MB".format(Locale.US, bytes.toDouble() / MB)
        else -> "${ceil(bytes.toDouble() / KB).toLong().coerceAtLeast(1)} KB"
    }

    /** Whole-GB storage figure like the prototype's "10 GB". */
    fun formatGb(bytes: Long): String = "${(bytes.toDouble() / GB).toLong()} GB"

    /** "10 GB of 15 GB used" — 0-byte total (unlimited plan) renders used only. */
    fun formatStorageLabel(usedBytes: Long, totalBytes: Long): String =
        if (totalBytes <= 0) "${formatGb(usedBytes)} used"
        else "${formatGb(usedBytes)} of ${formatGb(totalBytes)} used"

    fun storageFraction(usedBytes: Long, totalBytes: Long): Float =
        if (totalBytes <= 0) 0f else (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)

    fun formatSpeed(bytesPerSec: Long): String =
        "%.1f MB/s".format(Locale.US, bytesPerSec.toDouble() / MB)

    fun formatEta(seconds: Long): String =
        "~${ceil(seconds / 60.0).toLong().coerceAtLeast(1)} min remaining"

    /** "Today, 8:12 AM" / "Yesterday, 6:40 PM" / "Mar 3, 2026" */
    fun formatTimestamp(timestamp: Long): String {
        val time = SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
        return when {
            isSameDay(timestamp, System.currentTimeMillis()) -> "Today, $time"
            isSameDay(timestamp, System.currentTimeMillis() - DAY_MS) -> "Yesterday, $time"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestamp))
        }
    }

    fun formatDate(timestamp: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestamp))

    fun accountColorIndex(email: String, paletteSize: Int): Int =
        abs(email.hashCode()) % paletteSize

    /** Human-readable path for a SAF tree URI, e.g. "/storage/emulated/0/DCIM/Camera". */
    fun decodeTreePath(uriString: String): String {
        val segment = android.net.Uri.parse(uriString).lastPathSegment ?: return uriString
        return when {
            segment.startsWith("primary:") -> "/storage/emulated/0/${segment.removePrefix("primary:")}"
            else -> segment
        }
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000

    private fun isSameDay(a: Long, b: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = a }
        val calB = Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }
}
