@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.wizlabs.gpscamera

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Sora (headings) and Roboto Mono (coordinates, time, accuracy): variable fonts, Latin subset, OFL. */
val SoraFamily = FontFamily(
    Font(R.font.gps_sora, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.gps_sora, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.gps_sora, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

val MonoFamily = FontFamily(
    Font(R.font.gps_mono, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.gps_mono, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500)))
)

/** Scale from handoff §11: Display 32/38 · H1 22/28 · H2 17/22 · Body 15/22 · Label 12/16 caps · Mono 14/18, 12/16. */
val GpsTypography = Typography(
    displaySmall = TextStyle(fontFamily = SoraFamily, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = SoraFamily, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = SoraFamily, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = SoraFamily, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = SoraFamily, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
)

val MonoBody = TextStyle(fontFamily = MonoFamily, fontSize = 14.sp, lineHeight = 18.sp)
val MonoSmall = TextStyle(fontFamily = MonoFamily, fontSize = 12.sp, lineHeight = 16.sp)
