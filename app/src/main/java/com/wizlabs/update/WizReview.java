package com.wizlabs.update;

import android.app.Activity;
import android.content.Context;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Play In-App Review (plan 2.6). Never from a button and never after a "do you like it?" question.
 * The app reports real use with {@link #event}, and calls {@link #maybeAsk} at a moment when a task
 * has just finished. Every failure is silent; on sideloaded or emulator installs nothing happens.
 * Does not depend on the update classes: the caller passes WizUpdate.wasShownThisProcess().
 */
public final class WizReview {
    private WizReview() {}

    private static final long EVENT_GAP_MS = 60_000;
    private static final Map<String, Long> lastEventAt = new HashMap<>();
    private static boolean inFlight;

    /** Counts one use event. The same key counts at most once a minute, so typing cannot inflate it. */
    public static void event(Context ctx, String key) {
        try {
            long now = System.currentTimeMillis();
            Long last = lastEventAt.get(key);
            if (last != null && now - last >= 0 && now - last < EVENT_GAP_MS) return;
            lastEventAt.put(key, now);
            new ReviewPrefs(ctx.getApplicationContext())
                    .recordEvent(ReviewPolicy.dayOf(now, TimeZone.getDefault().getOffset(now)));
        } catch (Throwable ignored) { }
    }

    public static void maybeAsk(final Activity activity, int minEvents, int minActiveDays, boolean updateUiShownThisProcess) {
        if (inFlight || activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            final Context app = activity.getApplicationContext();
            final ReviewPrefs prefs = new ReviewPrefs(app);
            long now = System.currentTimeMillis();
            long installedAt = app.getPackageManager().getPackageInfo(app.getPackageName(), 0).firstInstallTime;
            if (!ReviewPolicy.shouldAsk(now, installedAt, prefs.events(), prefs.activeDays(), minEvents, minActiveDays,
                    prefs.lastAskAt(), prefs.asks(), updateUiShownThisProcess)) return;
            inFlight = true;
            final ReviewManager rm = ReviewManagerFactory.create(app);
            rm.requestReviewFlow().addOnCompleteListener(req -> {
                if (!req.isSuccessful() || activity.isFinishing() || activity.isDestroyed()) { inFlight = false; return; }
                ReviewInfo info = req.getResult();
                try {
                    rm.launchReviewFlow(activity, info).addOnCompleteListener(done -> {
                        // The API never says whether a card appeared: count the attempt either way.
                        prefs.recordAsk(System.currentTimeMillis());
                        inFlight = false;
                    });
                } catch (Throwable t) { inFlight = false; }
            });
        } catch (Throwable t) {
            inFlight = false;
        }
    }
}
