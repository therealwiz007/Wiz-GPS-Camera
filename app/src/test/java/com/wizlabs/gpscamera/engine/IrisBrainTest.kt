package com.wizlabs.gpscamera.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrisBrainTest {
    @Test fun waitingForPermission() {
        val s = IrisBrain.react(IrisEvent.WaitingPermission)
        assertEquals(Expression.NEUTRAL, s.expression); assertEquals(Action.STILL, s.action)
        assertEquals("I need precise location to stamp photos.", s.line)
    }
    @Test fun searchingScansThenSettlesAfter20s() {
        val early = IrisBrain.react(IrisEvent.SearchingGps(12, 5))
        assertEquals(Expression.SEARCHING, early.expression); assertEquals(Action.SCAN, early.action)
        assertEquals("Finding your position… ±12 m", early.line)
        assertEquals(Action.SCAN, IrisBrain.react(IrisEvent.SearchingGps(12, 19)).action)
        assertEquals(Action.STILL, IrisBrain.react(IrisEvent.SearchingGps(12, 20)).action)
    }
    @Test fun searchingWithoutAccuracy() {
        assertEquals("Finding your position…", IrisBrain.react(IrisEvent.SearchingGps(null)).line)
    }
    @Test fun fixLockedBounces() {
        val s = IrisBrain.react(IrisEvent.FixLocked(5))
        assertEquals(Expression.HAPPY, s.expression); assertEquals(Action.BOUNCE, s.action); assertEquals("Locked: ±5 m.", s.line)
    }
    @Test fun gpsOffWorried() {
        val s = IrisBrain.react(IrisEvent.GpsOff)
        assertEquals(Expression.WORRIED, s.expression); assertEquals(Action.STILL, s.action); assertEquals("Turn on GPS to continue.", s.line)
    }
    @Test fun galleryEmptySleepy() {
        val s = IrisBrain.react(IrisEvent.GalleryEmpty)
        assertEquals(Expression.SLEEPY, s.expression); assertEquals(Action.STILL, s.action); assertEquals("No photos yet.", s.line)
    }
    @Test fun exportDoneProud() {
        val s = IrisBrain.react(IrisEvent.ExportDone)
        assertEquals(Expression.PROUD, s.expression); assertEquals(Action.SPARKLE, s.action); assertEquals("Report saved to Downloads.", s.line)
    }
    @Test fun mockLocationWorried() {
        val s = IrisBrain.react(IrisEvent.MockLocation)
        assertEquals(Expression.WORRIED, s.expression); assertEquals(Action.STILL, s.action); assertEquals("Mock location is on.", s.line)
    }
    @Test fun onboardingPages() {
        assertEquals(Expression.NEUTRAL, IrisBrain.react(IrisEvent.Onboarding(0)).expression)
        assertEquals(Expression.HAPPY, IrisBrain.react(IrisEvent.Onboarding(1)).expression)
        assertEquals(Expression.PROUD, IrisBrain.react(IrisEvent.Onboarding(2)).expression)
        assertEquals(Expression.PROUD, IrisBrain.react(IrisEvent.Onboarding(9)).expression)
    }
    @Test fun everyLineFitsTheLimit() {
        val events = listOf(
            IrisEvent.WaitingPermission, IrisEvent.SearchingGps(120, 3), IrisEvent.FixLocked(999), IrisEvent.GpsOff,
            IrisEvent.GalleryEmpty, IrisEvent.ExportDone, IrisEvent.MockLocation,
            IrisEvent.Onboarding(0), IrisEvent.Onboarding(1), IrisEvent.Onboarding(2)
        )
        events.forEach { e -> val l = IrisBrain.react(e).line; assertTrue(l, l == null || l.length <= IrisBrain.MAX_LINE) }
        assertNull(null)
    }
}
