package com.wizlabs.gpscamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {
    /** Worked example from Snyder, "Map Projections: A Working Manual" (USGS 1395), p. 269. */
    @Test
    fun transverseMercatorMatchesSnyderWorkedExample() {
        // Clarke 1866 ellipsoid, as used in the book's example.
        val (e, n) = Geo.transverseMercator(40.5, -73.5, -75.0, 6378206.4, 0.00676866)
        assertEquals(500000.0 + 127106.5, e, 0.5)
        assertEquals(4484124.4, n, 0.5)
    }

    @Test
    fun utmZoneAndBand() {
        val u = Geo.toUtm(40.5, -73.5)!!
        assertEquals(18, u.zone)
        assertEquals('T', u.band)
    }

    @Test
    fun mgrsEiffelTower() {
        assertTrue(Geo.toMgrs(48.8584, 2.2945)!!.startsWith("31U DQ 4825"))
    }

    @Test
    fun mgrsStatueOfLiberty() {
        assertTrue(Geo.toMgrs(40.6892, -74.0445)!!.startsWith("18T WL 807"))
    }

    @Test
    fun utmSouthernHemisphereUsesFalseNorthing() {
        val u = Geo.toUtm(-33.8568, 151.2153)!!
        assertEquals(56, u.zone)
        assertEquals('H', u.band)
        assertTrue(u.northing in 6_200_000.0..6_300_000.0)
    }

    @Test
    fun norwayException() {
        assertEquals(32, Geo.toUtm(60.0, 5.0)!!.zone)
    }

    @Test
    fun utmUndefinedNearPoles() {
        assertNull(Geo.toUtm(85.0, 10.0))
        // Falls back to decimal on the stamp.
        assertTrue(Geo.format(85.0, 10.0, CoordFormat.UTM).startsWith("Lat "))
    }

    @Test
    fun dmsFormatting() {
        assertEquals("28°36'46.4\"N", Geo.dms(28.6129, 'N', 'S'))
        assertEquals("77°13'46.2\"E", Geo.dms(77.2295, 'E', 'W'))
        assertEquals("33°51'24.5\"S", Geo.dms(-33.8568, 'N', 'S'))
    }

    @Test
    fun dmsNeverPrintsSixtySeconds() {
        assertEquals("10°01'00.0\"N", Geo.dms(10.0166666, 'N', 'S'))
    }

    @Test
    fun units() {
        assertEquals("100 m", Geo.distance(100.0, Units.METRIC))
        assertEquals("328 ft", Geo.distance(100.0, Units.IMPERIAL))
        assertEquals("62 mph", Geo.speed(100f, Units.IMPERIAL))
    }

    @Test
    fun projectFolderNameIsSafe() {
        assertEquals("Site A Block 2", ProjectStore.folderName("Site A/Block:2"))
        assertEquals("Project", ProjectStore.folderName("  ///  "))
    }
}
