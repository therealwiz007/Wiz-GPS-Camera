package com.wizlabs.gpscamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsSetupTest {
    @Test fun adsAreOff() = assertFalse(AdsSetup.ENABLED)
    @Test fun noSlotOnANoAdScreen() = AdsSetup.slots.forEach { assertFalse(it.id, it.screen in AdsSetup.noAdScreens) }
    @Test fun everyNoAdScreenExists() = assertTrue(AdsSetup.allScreens.containsAll(AdsSetup.noAdScreens))
    @Test fun slotScreensAreAdScreens() = AdsSetup.slots.forEach { assertTrue(it.screen in AdsSetup.screens) }
    @Test fun cameraAndGateAreNoAd() = assertTrue(AdsSetup.noAdScreens.containsAll(setOf("camera", "gate", "onboarding", "viewer", "map")))
    @Test fun slotsMatchHandoff() = assertEquals(listOf("gallery", "settings", "adfree"), AdsSetup.slots.map { it.id })
}
