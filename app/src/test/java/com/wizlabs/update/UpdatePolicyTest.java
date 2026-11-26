package com.wizlabs.update;

import static com.wizlabs.update.UpdatePolicy.Action.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UpdatePolicyTest {
    private static final long H = 60L * 60 * 1000;
    private static final long NOW = 1_800_000_000_000L;
    private static final int UNKNOWN = 0, INSTALLED = 4, FAILED = 5;

    private static UpdatePolicy.Action d(long now, boolean avail, int status, boolean flex, int vc,
                                         long lastAt, int lastVc, boolean prompted, boolean host) {
        return UpdatePolicy.decide(now, avail, status, flex, vc, lastAt, lastVc, prompted, host);
    }

    @Test public void firstTimePromptsFlexible() { assertEquals(PROMPT_FLEXIBLE, d(NOW, true, UNKNOWN, true, 7, 0, -1, false, true)); }
    @Test public void flexibleNotAllowedGivesStoreLink() { assertEquals(PROMPT_STORE_LINK, d(NOW, true, UNKNOWN, false, 7, 0, -1, false, true)); }
    @Test public void notAvailableAfterPromptIsNone() { assertEquals(NONE, d(NOW, false, UNKNOWN, true, 0, NOW - 30 * H, 7, false, true)); }
    @Test public void sameVcWithin24hIsNone() { assertEquals(NONE, d(NOW, true, UNKNOWN, true, 7, NOW - 23 * H, 7, false, true)); }
    @Test public void sameVcAfter24hPrompts() { assertEquals(PROMPT_FLEXIBLE, d(NOW, true, UNKNOWN, true, 7, NOW - 24 * H, 7, false, true)); }
    @Test public void newVcWithin24hPrompts() { assertEquals(PROMPT_FLEXIBLE, d(NOW, true, UNKNOWN, true, 8, NOW - 1 * H, 7, false, true)); }
    @Test public void clockMovedBackPrompts() { assertEquals(PROMPT_FLEXIBLE, d(NOW, true, UNKNOWN, true, 7, NOW + 5 * H, 7, false, true)); }
    @Test public void oncePerProcess() { assertEquals(NONE, d(NOW, true, UNKNOWN, true, 8, 0, -1, true, true)); }
    @Test public void hostBusyIsNone() { assertEquals(NONE, d(NOW, true, UNKNOWN, true, 8, 0, -1, false, false)); }
    @Test public void downloadedShowsBarEvenWhenBusy() { assertEquals(RESTART_BAR, d(NOW, true, UpdatePolicy.DOWNLOADED, true, 8, NOW, 8, true, false)); }
    @Test public void downloadingIsNone() {
        assertEquals(NONE, d(NOW, true, UpdatePolicy.PENDING, true, 8, 0, -1, false, true));
        assertEquals(NONE, d(NOW, true, UpdatePolicy.DOWNLOADING, true, 8, 0, -1, false, true));
        assertEquals(NONE, d(NOW, true, UpdatePolicy.INSTALLING, true, 8, 0, -1, false, true));
    }
    @Test public void failedOrInstalledFallsBackToCadence() {
        assertEquals(PROMPT_FLEXIBLE, d(NOW, true, FAILED, true, 8, 0, -1, false, true));
        assertEquals(NONE, d(NOW, false, INSTALLED, true, 0, 0, -1, false, true));
    }
}
