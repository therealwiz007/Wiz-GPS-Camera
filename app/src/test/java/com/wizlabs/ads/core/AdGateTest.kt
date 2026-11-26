package com.wizlabs.ads.core

import com.wizlabs.ads.AdsConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AdGateTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L
    private val screens = setOf("main", "manage", "find", "map", "settings")

    /** A state where everything else would allow every format. */
    private val open = AdState(
        now = now, graceStart = now - 30 * day, sessions = 50, sessionStartedAt = now - 600_000,
        consentOk = true, sdkLinked = true,
    )

    @Test fun offBuildBlocksEveryFormatOnEveryScreen() {
        val gate = AdGate(AdsConfig(enabled = false, noAdScreens = emptySet(), adScreens = screens))
        for (f in AdFormat.values()) for (s in screens + "unknown") {
            assertEquals("ads switched off in this build", gate.reason(open, f, s))
            assertFalse(gate.mayShow(f, s, open))
        }
        assertFalse(gate.adsAllowedAtAll(open))
    }

    @Test fun offWinsOverEverything() {
        val gate = AdGate(AdsConfig(enabled = false, noAdScreens = emptySet(), adScreens = screens))
        val s = open.copy(proOwned = true, sdkLinked = false, consentOk = false)
        assertEquals("ads switched off in this build", gate.reason(s, AdFormat.BANNER, null))
    }

    @Test fun onBuildRespectsNoAdScreens() {
        val gate = AdGate(AdsConfig(enabled = true, noAdScreens = setOf("manage", "find"), adScreens = setOf("main")))
        assertNull(gate.reason(open, AdFormat.BANNER, "main"))
        assertEquals("no-ad screen", gate.reason(open, AdFormat.BANNER, "manage"))
        assertEquals("no-ad screen", gate.reason(open, AdFormat.BANNER, "other"))
    }

    @Test fun onBuildWithoutConsentShowsNothing() {
        val gate = AdGate(AdsConfig(enabled = true, noAdScreens = emptySet(), adScreens = setOf("main")))
        assertEquals("no consent", gate.reason(open.copy(consentOk = false), AdFormat.BANNER, "main"))
    }
}
