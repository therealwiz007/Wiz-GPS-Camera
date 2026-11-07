package com.wizlabs.gpscamera

import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Coordinate and unit formatting used on the stamp, in reports and in exports. */
object Geo {
    data class Utm(val zone: Int, val band: Char, val easting: Double, val northing: Double)

    private const val A = 6378137.0                 // WGS84 semi-major axis
    private const val F = 1 / 298.257223563         // WGS84 flattening
    private const val K0 = 0.9996
    private const val BANDS = "CDEFGHJKLMNPQRSTUVWX"

    fun format(lat: Double, lon: Double, f: CoordFormat): String = when (f) {
        CoordFormat.DECIMAL -> String.format(Locale.US, "Lat %.6f°  Long %.6f°", lat, lon)
        CoordFormat.DMS -> "${dms(lat, 'N', 'S')}  ${dms(lon, 'E', 'W')}"
        CoordFormat.UTM -> toUtm(lat, lon)?.let {
            String.format(Locale.US, "UTM %d%c  %.0fE  %.0fN", it.zone, it.band, it.easting, it.northing)
        } ?: format(lat, lon, CoordFormat.DECIMAL)
        CoordFormat.MGRS -> toMgrs(lat, lon)?.let { "MGRS $it" } ?: format(lat, lon, CoordFormat.DECIMAL)
    }

    /** e.g. 28°36'46.4"N */
    fun dms(value: Double, pos: Char, neg: Char): String {
        val a = abs(value)
        var d = floor(a).toInt()
        val mFull = (a - d) * 60.0
        var m = floor(mFull).toInt()
        var s = (mFull - m) * 60.0
        // Avoid printing 60.0" after rounding.
        if (s >= 59.95) {
            s = 0.0
            m += 1
            if (m == 60) {
                m = 0
                d += 1
            }
        }
        return String.format(Locale.US, "%d°%02d'%04.1f\"%c", d, m, s, if (value >= 0) pos else neg)
    }

    /** UTM is defined from 80°S to 84°N; outside that range this returns null. */
    fun toUtm(lat: Double, lon: Double): Utm? {
        if (lat < -80.0 || lat > 84.0) return null
        var lonN = lon
        while (lonN < -180.0) lonN += 360.0
        while (lonN >= 180.0) lonN -= 360.0
        var zone = floor((lonN + 180.0) / 6.0).toInt() + 1
        // Norway and Svalbard exceptions.
        if (lat >= 56.0 && lat < 64.0 && lonN >= 3.0 && lonN < 12.0) zone = 32
        if (lat >= 72.0 && lat < 84.0) {
            zone = when {
                lonN >= 0.0 && lonN < 9.0 -> 31
                lonN >= 9.0 && lonN < 21.0 -> 33
                lonN >= 21.0 && lonN < 33.0 -> 35
                lonN >= 33.0 && lonN < 42.0 -> 37
                else -> zone
            }
        }
        val band = BANDS[((lat + 80.0) / 8.0).toInt().coerceIn(0, BANDS.length - 1)]
        val (easting, northing) = transverseMercator(lat, lonN, ((zone - 1) * 6 - 180 + 3).toDouble(), A, F * (2 - F))
        return Utm(zone, band, easting, northing)
    }

    /**
     * Transverse Mercator (Snyder, USGS Professional Paper 1395, eqs. 8-9 to 8-10) with UTM's
     * scale factor and false easting/northing. Returns easting to northing in metres.
     */
    internal fun transverseMercator(lat: Double, lon: Double, centralLon: Double, a: Double, e2: Double): Pair<Double, Double> {
        val ep2 = e2 / (1 - e2)
        val phi = Math.toRadians(lat)
        val lambda0 = Math.toRadians(centralLon)
        val lambda = Math.toRadians(lon)
        val n = a / sqrt(1 - e2 * sin(phi) * sin(phi))
        val t = tan(phi) * tan(phi)
        val c = ep2 * cos(phi) * cos(phi)
        val aa = cos(phi) * (lambda - lambda0)
        val e4 = e2 * e2
        val e6 = e4 * e2
        val m = a * (
            (1 - e2 / 4 - 3 * e4 / 64 - 5 * e6 / 256) * phi -
                (3 * e2 / 8 + 3 * e4 / 32 + 45 * e6 / 1024) * sin(2 * phi) +
                (15 * e4 / 256 + 45 * e6 / 1024) * sin(4 * phi) -
                (35 * e6 / 3072) * sin(6 * phi)
            )
        val easting = K0 * n * (
            aa + (1 - t + c) * aa * aa * aa / 6 +
                (5 - 18 * t + t * t + 72 * c - 58 * ep2) * aa * aa * aa * aa * aa / 120
            ) + 500000.0
        var northing = K0 * (
            m + n * tan(phi) * (
                aa * aa / 2 + (5 - t + 9 * c + 4 * c * c) * aa * aa * aa * aa / 24 +
                    (61 - 58 * t + t * t + 600 * c - 330 * ep2) * aa * aa * aa * aa * aa * aa / 720
                )
            )
        if (lat < 0) northing += 10_000_000.0
        return easting to northing
    }

    /** Military Grid Reference System, 1 m precision, e.g. "43R GM 12345 67890". */
    fun toMgrs(lat: Double, lon: Double): String? {
        val u = toUtm(lat, lon) ?: return null
        val set = (u.zone - 1) % 6 + 1
        val colLetters = when (set % 3) {
            1 -> "ABCDEFGH"
            2 -> "JKLMNPQR"
            else -> "STUVWXYZ"
        }
        val rowLetters = "ABCDEFGHJKLMNPQRSTUV"
        val col = (floor(u.easting / 100000.0).toInt() - 1).coerceIn(0, 7)
        val rowOffset = if (set % 2 == 0) 5 else 0
        val row = (floor(u.northing / 100000.0).toInt() + rowOffset) % 20
        val e = floor(u.easting % 100000.0).toInt()
        val nn = floor(u.northing % 100000.0).toInt()
        return String.format(
            Locale.US, "%d%c %c%c %05d %05d",
            u.zone, u.band, colLetters[col], rowLetters[row], e, nn
        )
    }

    fun distance(m: Double, units: Units): String = when (units) {
        Units.METRIC -> "${m.toInt()} m"
        Units.IMPERIAL -> "${(m * 3.28084).toInt()} ft"
    }

    fun speed(kmh: Float, units: Units): String = when (units) {
        Units.METRIC -> "${kmh.toInt()} km/h"
        Units.IMPERIAL -> "${(kmh * 0.621371f).toInt()} mph"
    }
}
