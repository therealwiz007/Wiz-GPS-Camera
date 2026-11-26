package com.wizlabs.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReviewPolicyTest {
    private static final long D = ReviewPolicy.DAY_MS;
    private static final long NOW = 1_800_000_000_000L;
    private static final long OLD = NOW - 10 * D;

    @Test public void allRulesMetAsks() { assertTrue(ReviewPolicy.shouldAsk(NOW, OLD, 10, 4, 10, 4, 0, 0, false)); }
    @Test public void tooNewInstall() { assertFalse(ReviewPolicy.shouldAsk(NOW, NOW - 2 * D, 50, 9, 10, 4, 0, 0, false)); }
    @Test public void unknownInstallTime() { assertFalse(ReviewPolicy.shouldAsk(NOW, 0, 50, 9, 10, 4, 0, 0, false)); }
    @Test public void notEnoughEvents() { assertFalse(ReviewPolicy.shouldAsk(NOW, OLD, 9, 4, 10, 4, 0, 0, false)); }
    @Test public void notEnoughDays() { assertFalse(ReviewPolicy.shouldAsk(NOW, OLD, 10, 3, 10, 4, 0, 0, false)); }
    @Test public void askedRecently() { assertFalse(ReviewPolicy.shouldAsk(NOW, OLD, 10, 4, 10, 4, NOW - 89 * D, 1, false)); }
    @Test public void askedLongAgo() { assertTrue(ReviewPolicy.shouldAsk(NOW, OLD, 10, 4, 10, 4, NOW - 90 * D, 1, false)); }
    @Test public void clockBehindLastAsk() { assertFalse(ReviewPolicy.shouldAsk(NOW, OLD, 10, 4, 10, 4, NOW + D, 1, false)); }
    @Test public void maxThreeAsks() { assertFalse(ReviewPolicy.shouldAsk(NOW, OLD, 10, 4, 10, 4, NOW - 200 * D, 3, false)); }
    @Test public void notAfterUpdateUi() { assertFalse(ReviewPolicy.shouldAsk(NOW, OLD, 10, 4, 10, 4, 0, 0, true)); }
    @Test public void dayFollowsLocalMidnight() {
        long midnightUtc = 20_000L * D;
        int ist = (int) (5.5 * 60 * 60 * 1000);
        assertEquals(ReviewPolicy.dayOf(midnightUtc - 1, 0) + 1, ReviewPolicy.dayOf(midnightUtc, 0));
        // 20:00 UTC is already the next day in India.
        assertEquals(20_001L, ReviewPolicy.dayOf(midnightUtc + 20 * 60 * 60 * 1000L, ist));
    }
}
