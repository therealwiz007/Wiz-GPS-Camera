package com.wizlabs.gpscamera

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Network time check. The app learns the true time from the HTTP `Date` header of a reliable
 * server and anchors it to the monotonic clock ([SystemClock.elapsedRealtime]), which the user
 * cannot change. Photos are then stamped with the anchored time, and a warning is printed if the
 * device clock disagrees. The anchor survives going offline (but not a reboot).
 */
object TimeSync {
    enum class Status { NETWORK, ANCHORED, DEVICE }

    data class Now(val millis: Long, val status: Status, val deviationMs: Long)

    private const val PREFS = "wiz_timesync"
    private const val FRESH_MS = 10 * 60_000L

    private val io = Executors.newSingleThreadExecutor()

    @Volatile private var anchorServer = 0L
    @Volatile private var anchorElapsed = 0L
    @Volatile private var lastSyncElapsed = 0L

    fun init(context: Context) {
        val app = context.applicationContext
        val boot = bootCount(app)
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (boot != -1 && p.getInt("boot", -2) == boot) {
            val server = p.getLong("server", 0L)
            val elapsed = p.getLong("elapsed", 0L)
            if (server > 0L && SystemClock.elapsedRealtime() >= elapsed) {
                anchorServer = server
                anchorElapsed = elapsed
                lastSyncElapsed = elapsed
            }
        }
    }

    fun now(): Now {
        val sys = System.currentTimeMillis()
        val server = anchorServer
        val elapsed = anchorElapsed
        val tick = SystemClock.elapsedRealtime()
        if (server > 0L && tick >= elapsed) {
            val corrected = server + (tick - elapsed)
            val fresh = tick - lastSyncElapsed < FRESH_MS
            return Now(corrected, if (fresh) Status.NETWORK else Status.ANCHORED, sys - corrected)
        }
        return Now(sys, Status.DEVICE, 0L)
    }

    /** Fire-and-forget; safe to call repeatedly. */
    fun sync(context: Context) {
        val app = context.applicationContext
        io.execute {
            try {
                val c = URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection
                c.requestMethod = "HEAD"
                c.connectTimeout = 5000
                c.readTimeout = 5000
                c.instanceFollowRedirects = false
                val t0 = SystemClock.elapsedRealtime()
                c.connect()
                c.responseCode
                val date = c.date
                val t1 = SystemClock.elapsedRealtime()
                c.disconnect()
                if (date > 0L) {
                    // The header has one-second resolution, so add half a second on average.
                    anchorServer = date + 500L
                    anchorElapsed = t0 + (t1 - t0) / 2
                    lastSyncElapsed = SystemClock.elapsedRealtime()
                    val boot = bootCount(app)
                    if (boot != -1) {
                        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putInt("boot", boot)
                            .putLong("server", anchorServer)
                            .putLong("elapsed", anchorElapsed)
                            .apply()
                    }
                }
            } catch (e: Exception) {
                // offline: keep whatever anchor we have
            }
        }
    }

    private fun bootCount(context: Context): Int = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    } catch (e: Exception) {
        -1
    }
}
