package com.wizlabs.update;

import android.content.Context;
import android.content.SharedPreferences;

/** SharedPreferences "wiz_review": events, active_days, last_active_day, last_ask_at, asks. */
final class ReviewPrefs {
    private final SharedPreferences p;

    ReviewPrefs(Context ctx) { p = ctx.getSharedPreferences("wiz_review", Context.MODE_PRIVATE); }

    int events() { return p.getInt("events", 0); }
    int activeDays() { return p.getInt("active_days", 0); }
    long lastAskAt() { return p.getLong("last_ask_at", 0L); }
    int asks() { return p.getInt("asks", 0); }

    void recordEvent(long day) {
        SharedPreferences.Editor e = p.edit().putInt("events", events() + 1);
        if (p.getLong("last_active_day", Long.MIN_VALUE) != day) {
            e.putInt("active_days", activeDays() + 1).putLong("last_active_day", day);
        }
        e.apply();
    }

    void recordAsk(long now) { p.edit().putLong("last_ask_at", now).putInt("asks", asks() + 1).apply(); }
}
