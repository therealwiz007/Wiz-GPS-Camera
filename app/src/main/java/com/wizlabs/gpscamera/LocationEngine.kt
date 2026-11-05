package com.wizlabs.gpscamera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.Manifest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.floor

fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val r = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, r)
    return r[0]
}

/** Open Location Code ("plus code"), 10 digits. */
object PlusCode {
    private const val ALPHABET = "23456789CFGHJMPQRVWX"

    fun encode(lat: Double, lon: Double): String {
        var la = floor((lat.coerceIn(-90.0, 90.0) + 90.0) * 8000.0).toLong().coerceIn(0L, 1_439_999L)
        var lo = floor((normalizeLon(lon) + 180.0) * 8000.0).toLong().coerceIn(0L, 2_879_999L)
        val chars = CharArray(10)
        for (i in 4 downTo 0) {
            chars[i * 2 + 1] = ALPHABET[(lo % 20).toInt()]
            lo /= 20
            chars[i * 2] = ALPHABET[(la % 20).toInt()]
            la /= 20
        }
        return String(chars, 0, 8) + "+" + String(chars, 8, 2)
    }

    private fun normalizeLon(lon: Double): Double {
        var l = lon
        while (l < -180.0) l += 360.0
        while (l >= 180.0) l -= 360.0
        return l
    }
}

/**
 * Resolves coordinates to an address. Successful lookups are cached on the device so that a
 * nearby address can still be shown when the network (or Geocoder) is unavailable.
 */
class AddressResolver(context: Context) {
    private class Entry(val lat: Double, val lon: Double, val text: String)

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("wiz_address_cache", Context.MODE_PRIVATE)
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val cache = ArrayList<Entry>()

    init {
        runCatching {
            val arr = JSONArray(prefs.getString("entries", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                cache.add(Entry(o.getDouble("lat"), o.getDouble("lon"), o.getString("text")))
            }
        }
    }

    /** [onDone] is always called on the main thread. fromCache = true means "nearby, not exact". */
    fun lookup(lat: Double, lon: Double, onDone: (text: String?, fromCache: Boolean) -> Unit) {
        val exact = nearest(lat, lon, 30f)
        if (exact != null) {
            onDone(exact, false)
            return
        }
        val deliver = { text: String? ->
            main.post {
                if (text != null) {
                    remember(lat, lon, text)
                    onDone(text, false)
                } else {
                    val near = nearest(lat, lon, 250f)
                    onDone(near, near != null)
                }
            }
        }
        try {
            if (!Geocoder.isPresent()) {
                deliver(null)
                return
            }
            val geocoder = Geocoder(app, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        deliver(format(addresses.firstOrNull()))
                    }

                    override fun onError(errorMessage: String?) {
                        deliver(null)
                    }
                })
            } else {
                io.execute {
                    val text = try {
                        format(legacyGeocode(geocoder, lat, lon))
                    } catch (e: Exception) {
                        null
                    }
                    deliver(text)
                }
            }
        } catch (e: Exception) {
            deliver(null)
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyGeocode(g: Geocoder, lat: Double, lon: Double): Address? =
        g.getFromLocation(lat, lon, 1)?.firstOrNull()

    /**
     * Returns "title\nstreet line", e.g. "Gara, Himachal Pradesh, India\nNH-21, Gara, Himachal Pradesh".
     * The stamp prints the first part large and the second part below it.
     */
    private fun format(a: Address?): String? {
        if (a == null) return null
        fun join(vararg parts: String?): String =
            parts.filterNotNull().map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(", ")
        val title = join(a.locality ?: a.subAdminArea ?: a.subLocality, a.adminArea, a.countryName)
        val street = join(a.thoroughfare ?: a.featureName, a.subLocality, a.locality, a.adminArea)
        val full = if (a.maxAddressLineIndex >= 0) a.getAddressLine(0) else null
        val line = street.ifBlank { full ?: "" }
        if (title.isBlank()) return (full ?: line).ifBlank { null }
        return if (line.isBlank() || line == title) title else "$title\n$line"
    }

    private fun nearest(lat: Double, lon: Double, maxM: Float): String? {
        var best: Entry? = null
        var bestD = Float.MAX_VALUE
        for (e in cache) {
            val d = distanceM(lat, lon, e.lat, e.lon)
            if (d <= maxM && d < bestD) {
                best = e
                bestD = d
            }
        }
        return best?.text
    }

    private fun remember(lat: Double, lon: Double, text: String) {
        cache.add(0, Entry(lat, lon, text))
        while (cache.size > 30) cache.removeAt(cache.size - 1)
        val arr = JSONArray()
        cache.forEach { e ->
            arr.put(JSONObject().put("lat", e.lat).put("lon", e.lon).put("text", e.text))
        }
        prefs.edit().putString("entries", arr.toString()).apply()
    }
}

/** Current temperature + conditions from Open-Meteo (no API key). Optional, off by default. */
object WeatherFetcher {
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun fetch(lat: Double, lon: Double, onDone: (String?) -> Unit) {
        io.execute {
            val result = try {
                val url = URL(
                    String.format(
                        Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code",
                        lat, lon
                    )
                )
                val c = url.openConnection() as HttpURLConnection
                c.connectTimeout = 6000
                c.readTimeout = 6000
                val body = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
                val cur = JSONObject(body).getJSONObject("current")
                val t = cur.getDouble("temperature_2m")
                val code = cur.getInt("weather_code")
                String.format(Locale.US, "%.0f °C, %s", t, describe(code))
            } catch (e: Exception) {
                null
            }
            main.post { onDone(result) }
        }
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Weather"
    }
}

/**
 * Continuous GPS: keeps a live [fix], resolves the address (with an offline cache), reads the
 * magnetic declination and, if enabled, fetches the weather.
 */
class LocationEngine(context: Context) {
    private val app = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(app)
    private val resolver = AddressResolver(app)

    var fix by mutableStateOf<GpsFix?>(null)
        private set
    var address by mutableStateOf<String?>(null)
        private set
    var addressCached by mutableStateOf(false)
        private set
    var weather by mutableStateOf<String?>(null)
        private set

    var declination: Float? = null
        private set
    var weatherEnabled = false
        set(value) {
            field = value
            if (value) fix?.let { maybeFetchWeather(it.lat, it.lon, force = false) }
        }

    private var running = false
    private var lookupBusy = false
    private var lookupLat = Double.NaN
    private var lookupLon = Double.NaN
    private var lookupAt = 0L
    private var addressLat = Double.NaN
    private var addressLon = Double.NaN
    private var weatherBusy = false
    private var weatherLat = Double.NaN
    private var weatherLon = Double.NaN
    private var weatherAt = 0L

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onLocation(it) }
        }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !hasPermission()) return
        running = true
        try {
            // Seed with the last known location; its real age is tracked via elapsedRealtimeNanos.
            client.lastLocation.addOnSuccessListener { loc -> if (loc != null) onLocation(loc) }
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .build()
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            running = false
        }
    }

    fun stop() {
        if (!running) return
        running = false
        client.removeLocationUpdates(callback)
    }

    /** Ask for a fresh fix right now and re-resolve address/weather. */
    @SuppressLint("MissingPermission")
    fun refresh() {
        if (!hasPermission()) return
        lookupLat = Double.NaN
        weatherAt = 0L
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc -> if (loc != null) onLocation(loc) }
        } catch (e: SecurityException) {
            // permission revoked while running; ignore
        }
        fix?.let { maybeLookup(it.lat, it.lon) }
    }

    private fun isMock(l: Location): Boolean =
        if (Build.VERSION.SDK_INT >= 31) l.isMock else legacyMock(l)

    @Suppress("DEPRECATION")
    private fun legacyMock(l: Location): Boolean = l.isFromMockProvider

    private fun onLocation(l: Location) {
        val prev = fix
        if (prev != null && l.elapsedRealtimeNanos < prev.elapsedRealtimeNanos) return
        fix = GpsFix(
            lat = l.latitude,
            lon = l.longitude,
            accuracyM = if (l.hasAccuracy()) l.accuracy else null,
            altitudeM = if (l.hasAltitude()) l.altitude else null,
            speedMps = if (l.hasSpeed()) l.speed else null,
            isMock = isMock(l),
            elapsedRealtimeNanos = l.elapsedRealtimeNanos
        )
        declination = try {
            GeomagneticField(
                l.latitude.toFloat(), l.longitude.toFloat(),
                (if (l.hasAltitude()) l.altitude else 0.0).toFloat(),
                System.currentTimeMillis()
            ).declination
        } catch (e: Exception) {
            null
        }
        maybeLookup(l.latitude, l.longitude)
        if (weatherEnabled) maybeFetchWeather(l.latitude, l.longitude, force = false)
    }

    private fun maybeLookup(lat: Double, lon: Double) {
        if (lookupBusy) return
        val now = SystemClock.elapsedRealtime()
        val moved = lookupLat.isNaN() || distanceM(lookupLat, lookupLon, lat, lon) > 40f
        val retry = address == null && now - lookupAt > 15_000L
        if (!moved && !retry) return
        lookupBusy = true
        resolver.lookup(lat, lon) { text, fromCache ->
            lookupBusy = false
            lookupAt = SystemClock.elapsedRealtime()
            if (text != null) {
                lookupLat = lat
                lookupLon = lon
                addressLat = lat
                addressLon = lon
                address = text
                addressCached = fromCache
            } else {
                // Keep the previous address only if we have not moved far from where it was resolved.
                val stillNear = address != null && !addressLat.isNaN() &&
                    distanceM(addressLat, addressLon, lat, lon) < 250f
                if (stillNear) {
                    addressCached = true
                } else {
                    address = null
                    addressCached = false
                }
            }
        }
    }

    private fun maybeFetchWeather(lat: Double, lon: Double, force: Boolean) {
        if (weatherBusy) return
        val now = SystemClock.elapsedRealtime()
        val stale = weatherAt == 0L || now - weatherAt > 15 * 60_000L
        val moved = weatherLat.isNaN() || distanceM(weatherLat, weatherLon, lat, lon) > 3000f
        if (!force && !stale && !moved) return
        weatherBusy = true
        WeatherFetcher.fetch(lat, lon) { text ->
            weatherBusy = false
            weatherAt = SystemClock.elapsedRealtime()
            weatherLat = lat
            weatherLon = lon
            if (text != null) weather = text
        }
    }
}
