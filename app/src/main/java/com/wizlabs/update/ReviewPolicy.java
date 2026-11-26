package com.wizlabs.update;

/** Pure decision function for the in-app review card (plan 2.6). No Android imports. */
final class ReviewPolicy {
    private ReviewPolicy() {}

    static final long DAY_MS = 24L * 60 * 60 * 1000;
    static final long MIN_INSTALL_AGE_MS = 3 * DAY_MS;
    static final long MIN_ASK_GAP_MS = 90 * DAY_MS;
    static final int MAX_ASKS = 3;

    static boolean shouldAsk(long now, long installedAt, int events, int activeDays,
                             int minEvents, int minActiveDays,
                             long lastAskAt, int asks, boolean updateUiShownThisProcess) {
        if (updateUiShownThisProcess) return false;
        if (installedAt <= 0 || now - installedAt < MIN_INSTALL_AGE_MS) return false;
        if (events < minEvents || activeDays < minActiveDays) return false;
        if (asks >= MAX_ASKS) return false;
        if (lastAskAt > 0 && (now < lastAskAt || now - lastAskAt < MIN_ASK_GAP_MS)) return false;
        return true;
    }

    /** Local calendar day number, so "active days" follow the user's own midnight. */
    static long dayOf(long now, int tzOffsetMs) { return Math.floorDiv(now + tzOffsetMs, DAY_MS); }
}
