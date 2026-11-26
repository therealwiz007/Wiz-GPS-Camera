package com.wizlabs.gpscamera

import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    private fun ok(a: Int, b: Int, min: Double) {
        val r = contrastRatio(a, b)
        assertTrue("contrast %.2f < $min".format(r), r >= min)
    }
    @Test fun darkText() {
        ok(GpsDarkHex.SILVER, GpsDarkHex.BG, 4.5)
        ok(GpsDarkHex.MUTED, GpsDarkHex.BG, 4.5)
        ok(GpsDarkHex.MUTED, GpsDarkHex.SURFACE, 4.5)
        ok(GpsDarkHex.ON_TEAL, GpsDarkHex.TEAL, 4.5)
        ok(GpsDarkHex.TEAL, GpsDarkHex.BG, 3.0)
    }
    @Test fun lightText() {
        ok(GpsLightHex.SILVER, GpsLightHex.BG, 4.5)
        ok(GpsLightHex.MUTED, GpsLightHex.BG, 4.5)
        ok(GpsLightHex.ON_TEAL, GpsLightHex.TEAL_DEEP, 4.5)
        assertTrue(contrastRatio(GpsLightHex.ON_TEAL, GpsLightHex.TEAL) in 4.3..4.4)
    }
}
