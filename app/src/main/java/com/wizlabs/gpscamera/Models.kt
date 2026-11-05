package com.wizlabs.gpscamera

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import java.util.UUID

enum class StampTemplate(val label: String) {
    FULL("Full with map"),
    COMPACT("Compact"),
    TEXT_ONLY("Text only")
}

enum class StampPosition(val label: String) {
    BOTTOM("Bottom"),
    TOP("Top"),
    LEFT("Left")
}

enum class ClockFormat(val label: String) {
    H12("12-hour"),
    H24("24-hour")
}

enum class MockPolicy(val label: String) {
    FLAG("Flag on photo"),
    BLOCK("Block capture")
}

/** Photo shape. [ratio] is long side / short side. 1:1 is cropped from a 4:3 capture. */
enum class AspectChoice(val label: String, val ratio: Float) {
    R4_3("4:3", 4f / 3f),
    R16_9("16:9", 16f / 9f),
    R1_1("1:1", 1f);

    fun next(): AspectChoice {
        val all = values()
        return all[(ordinal + 1) % all.size]
    }
}

/** Everything the user can configure. Persisted by [SettingsRepo]. */
data class AppSettings(
    val note: String = "",
    val template: StampTemplate = StampTemplate.FULL,
    val position: StampPosition = StampPosition.BOTTOM,
    val showMap: Boolean = true,
    val showAddress: Boolean = true,
    val showCoords: Boolean = true,
    val showDateTime: Boolean = true,
    val showAccuracy: Boolean = false,
    val showAltitude: Boolean = false,
    val showHeading: Boolean = false,
    val showSpeed: Boolean = false,
    val showPlusCode: Boolean = false,
    val showWeather: Boolean = false,
    val showNote: Boolean = true,
    val showVerification: Boolean = false,
    val showLogo: Boolean = true,
    val dateFormat: String = "dd MMM yyyy",
    val clockFormat: ClockFormat = ClockFormat.H12,
    /** Empty string means "use the device time zone". */
    val timeZoneId: String = "",
    val showTimeZone: Boolean = false,
    val showSeconds: Boolean = false,
    val watermark: String = "",
    /** Bumped whenever the logo file changes so the UI reloads it. */
    val logoVersion: Int = 0,
    val gridOn: Boolean = false,
    val levelOn: Boolean = false,
    val timerSeconds: Int = 0,
    val burstCount: Int = 1,
    val megapixels: Int = 12,
    val aspect: AspectChoice = AspectChoice.R4_3,
    val mockPolicy: MockPolicy = MockPolicy.FLAG
)

class SettingsRepo(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wiz_settings", Context.MODE_PRIVATE)

    var settings by mutableStateOf(load())
        private set

    fun update(change: (AppSettings) -> AppSettings) {
        val next = change(settings)
        settings = next
        save(next)
    }

    private inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private fun load(): AppSettings {
        val d = AppSettings()
        fun b(key: String, def: Boolean) = prefs.getBoolean(key, def)
        fun i(key: String, def: Int) = prefs.getInt(key, def)
        fun s(key: String, def: String) = prefs.getString(key, def) ?: def
        return AppSettings(
            note = s("note", d.note),
            template = enumOf(prefs.getString("template", null), d.template),
            position = enumOf(prefs.getString("position", null), d.position),
            showMap = b("showMap", d.showMap),
            showAddress = b("showAddress", d.showAddress),
            showCoords = b("showCoords", d.showCoords),
            showDateTime = b("showDateTime", d.showDateTime),
            showAccuracy = b("showAccuracy", d.showAccuracy),
            showAltitude = b("showAltitude", d.showAltitude),
            showHeading = b("showHeading", d.showHeading),
            showSpeed = b("showSpeed", d.showSpeed),
            showPlusCode = b("showPlusCode", d.showPlusCode),
            showWeather = b("showWeather", d.showWeather),
            showNote = b("showNote", d.showNote),
            showVerification = b("showVerification", d.showVerification),
            showLogo = b("showLogo", d.showLogo),
            dateFormat = s("dateFormat", d.dateFormat),
            clockFormat = enumOf(prefs.getString("clockFormat", null), d.clockFormat),
            timeZoneId = s("timeZoneId", d.timeZoneId),
            showTimeZone = b("showTimeZone", d.showTimeZone),
            showSeconds = b("showSeconds", d.showSeconds),
            watermark = s("watermark", d.watermark),
            logoVersion = i("logoVersion", d.logoVersion),
            gridOn = b("gridOn", d.gridOn),
            levelOn = b("levelOn", d.levelOn),
            timerSeconds = i("timerSeconds", d.timerSeconds),
            burstCount = i("burstCount", d.burstCount),
            megapixels = i("megapixels", d.megapixels),
            aspect = enumOf(prefs.getString("aspect", null), d.aspect),
            mockPolicy = enumOf(prefs.getString("mockPolicy", null), d.mockPolicy)
        )
    }

    private fun save(n: AppSettings) {
        prefs.edit()
            .putString("note", n.note)
            .putString("template", n.template.name)
            .putString("position", n.position.name)
            .putBoolean("showMap", n.showMap)
            .putBoolean("showAddress", n.showAddress)
            .putBoolean("showCoords", n.showCoords)
            .putBoolean("showDateTime", n.showDateTime)
            .putBoolean("showAccuracy", n.showAccuracy)
            .putBoolean("showAltitude", n.showAltitude)
            .putBoolean("showHeading", n.showHeading)
            .putBoolean("showSpeed", n.showSpeed)
            .putBoolean("showPlusCode", n.showPlusCode)
            .putBoolean("showWeather", n.showWeather)
            .putBoolean("showNote", n.showNote)
            .putBoolean("showVerification", n.showVerification)
            .putBoolean("showLogo", n.showLogo)
            .putString("dateFormat", n.dateFormat)
            .putString("clockFormat", n.clockFormat.name)
            .putString("timeZoneId", n.timeZoneId)
            .putBoolean("showTimeZone", n.showTimeZone)
            .putBoolean("showSeconds", n.showSeconds)
            .putString("watermark", n.watermark)
            .putInt("logoVersion", n.logoVersion)
            .putBoolean("gridOn", n.gridOn)
            .putBoolean("levelOn", n.levelOn)
            .putInt("timerSeconds", n.timerSeconds)
            .putInt("burstCount", n.burstCount)
            .putInt("megapixels", n.megapixels)
            .putString("aspect", n.aspect.name)
            .putString("mockPolicy", n.mockPolicy.name)
            .apply()
    }
}

/** One GPS fix, with enough detail to judge how fresh and trustworthy it is. */
data class GpsFix(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float?,
    val altitudeM: Double?,
    val speedMps: Float?,
    val isMock: Boolean,
    val elapsedRealtimeNanos: Long
) {
    fun ageSeconds(): Int =
        ((SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000_000L).toInt().coerceAtLeast(0)
}

/** A frozen snapshot of everything that gets printed on (and embedded in) one photo. */
data class StampData(
    val lat: Double?,
    val lon: Double?,
    val accuracyM: Float?,
    val fixAgeS: Int?,
    val altitudeM: Double?,
    val speedKmh: Float?,
    val headingDeg: Float?,
    val headingTrue: Boolean,
    val address: String?,
    val addressCached: Boolean,
    val weather: String?,
    val plusCode: String?,
    val timeMs: Long,
    val timeStatus: TimeSync.Status,
    val clockDeviationMs: Long,
    val mock: Boolean,
    val note: String,
    val photoId: String
)

fun newPhotoId(): String =
    "WGC-" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)

fun buildStampData(
    s: AppSettings,
    fix: GpsFix?,
    address: String?,
    addressCached: Boolean,
    weather: String?,
    magneticHeading: Float?,
    declination: Float?,
    photoId: String
): StampData {
    val t = TimeSync.now()
    val heading = magneticHeading?.let { ((it + (declination ?: 0f)) % 360f + 360f) % 360f }
    return StampData(
        lat = fix?.lat,
        lon = fix?.lon,
        accuracyM = fix?.accuracyM,
        fixAgeS = fix?.ageSeconds(),
        altitudeM = fix?.altitudeM,
        speedKmh = fix?.speedMps?.let { it * 3.6f },
        headingDeg = heading,
        headingTrue = declination != null,
        address = if (fix != null) address else null,
        addressCached = addressCached,
        weather = if (fix != null) weather else null,
        plusCode = fix?.let { PlusCode.encode(it.lat, it.lon) },
        timeMs = t.millis,
        timeStatus = t.status,
        clockDeviationMs = t.deviationMs,
        mock = fix?.isMock == true,
        note = s.note.trim(),
        photoId = photoId
    )
}
