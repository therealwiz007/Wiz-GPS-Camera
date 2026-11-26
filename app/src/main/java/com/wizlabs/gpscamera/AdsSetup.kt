package com.wizlabs.gpscamera

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wizlabs.ads.AdsConfig
import com.wizlabs.ads.WizAds

/**
 * Ads-ready, ad-SDK-free (house rule 7). [ENABLED] is false and the shared `com.wizlabs.ads` copy is the
 * `--no-sdk` build (0.3), so no ad SDK, consent form, request or network call exists. Every slot renders nothing
 * and takes zero height. Google AdMob comes in a later release, which re-syncs wiz-ads and flips [ENABLED].
 */
object AdsSetup {
    const val ENABLED = false

    /** Every screen name the app uses. */
    val allScreens = setOf(
        "camera", "gate", "stamp_sheet", "viewer", "select", "search", "map", "export_sheet", "onboarding",
        "gallery", "settings"
    )

    /** Screens where a slot may appear at all. */
    val screens = setOf("gallery", "settings")

    /** Never any ad here (handoff §16A). */
    val noAdScreens = setOf("camera", "gate", "stamp_sheet", "viewer", "select", "search", "map", "export_sheet", "onboarding")

    enum class Format { NATIVE, BANNER, REWARDED_ROW }
    data class Slot(val id: String, val format: Format, val screen: String)

    val slots = listOf(
        Slot("gallery", Format.NATIVE, "gallery"),
        Slot("settings", Format.BANNER, "settings"),
        Slot("adfree", Format.REWARDED_ROW, "settings"),
    )

    fun install(context: Context) = WizAds.install(
        context,
        AdsConfig(enabled = ENABLED, adScreens = screens, noAdScreens = noAdScreens, debug = BuildConfig.DEBUG),
    )
}

/**
 * One ad slot. With ads off it returns before composing anything (zero height). In debug builds it throws if it is
 * placed on a no-ad screen, so a misplaced slot is caught while developing.
 */
@Composable
fun AdSlot(slot: String, modifier: Modifier = Modifier) {
    val s = AdsSetup.slots.first { it.id == slot }
    if (BuildConfig.DEBUG) check(s.screen !in AdsSetup.noAdScreens) { "ad slot '$slot' placed on no-ad screen '${s.screen}'" }
    if (!AdsSetup.ENABLED) return
}
