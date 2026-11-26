package com.wizlabs.gpscamera

import androidx.compose.ui.graphics.Color

// "Midnight Survey" design tokens (handoff §10). The app is dark-only in 3.0.0: the camera and gallery
// are always dark by design, and Settings follows. The light column is kept for contrast checks only.
val GpsBg = Color(0xFF060B18)
val GpsSurface = Color(0xFF0D1528)
val GpsSurfaceHi = Color(0xFF15203A)
val GpsLine = Color(0xFF1B2742)
val GpsTeal = Color(0xFF00E5C7)
val GpsTealDeep = Color(0xFF00B39C)
val GpsOnTeal = Color(0xFF03201C)
val GpsSilver = Color(0xFFEAF2FF)
val GpsMuted = Color(0xFF8796B5)
val GpsAmber = Color(0xFFFFB547)
val GpsBad = Color(0xFFFF5C6C)
val GpsGood = Color(0xFF3BE38A)
/** Control background over the camera preview. */
val GpsGlass = Color(0x990D1528)

/** Light column of the token table. Not used by the UI yet; checked by ContrastTest. */
object GpsLightHex {
    const val BG = 0xF4F7FB
    const val SILVER = 0x0A1426
    const val MUTED = 0x56637C
    const val TEAL = 0x00897A
    const val ON_TEAL = 0xFFFFFF
    // White on #00897A is only 4.32:1 (handoff said 4.5). Light buttons must use TEAL_DEEP.
    const val TEAL_DEEP = 0x00695E
}

object GpsDarkHex {
    const val BG = 0x060B18
    const val SURFACE = 0x0D1528
    const val SILVER = 0xEAF2FF
    const val MUTED = 0x8796B5
    const val TEAL = 0x00E5C7
    const val ON_TEAL = 0x03201C
}

/** WCAG contrast ratio between two 0xRRGGBB colours. */
fun contrastRatio(a: Int, b: Int): Double {
    fun lum(c: Int): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch((c shr 16) and 0xFF) + 0.7152 * ch((c shr 8) and 0xFF) + 0.0722 * ch(c and 0xFF)
    }
    val l1 = lum(a)
    val l2 = lum(b)
    return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
}
