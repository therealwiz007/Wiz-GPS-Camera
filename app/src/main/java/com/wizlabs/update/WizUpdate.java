package com.wizlabs.update;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

/**
 * Play In-App Updates, FLEXIBLE only (WIZ_3APPS_MASTER_PLAN.md section 3). The popup is always
 * dismissible: there is no IMMEDIATE flow, no staleness logic and nothing that blocks the app.
 * Every Play failure (offline, sideloaded, no Play Store) is silent.
 *
 * One instance per activity: create it in onCreate after the content view is set, then call
 * onResume / onPause / onDestroy.
 */
public final class WizUpdate {
    /** Version of the shared package; printed by wiz-update/sync.sh. Never edit the copy inside an app. */
    public static final String WIZ_UPDATE_VERSION = "1.0";

    public interface Host {
        /** Read at show time, so light/dark is current. */
        UpdatePalette palette();
        /** False while the user is busy (typing, an overlay open, a mock running...). */
        boolean mayPromptNow();
        String appName();
        /** Extra line on the restart bar, or null. */
        String restartNote();
        /** Extra space under the restart bar (an action strip, a banner), in px. */
        default int barBottomOffsetPx() { return 0; }
    }

    /** Debug-only test hook (plan 3.7): adb shell am start -n pkg/.MainActivity --ei wiz_fake_update 99 [--ez wiz_fake_reset true] */
    public static final String EXTRA_FAKE_UPDATE = "wiz_fake_update";
    public static final String EXTRA_FAKE_RESET = "wiz_fake_reset";

    private static final int REQUEST_CODE = 0x57A1;
    private static final long FIRST_CHECK_DELAY_MS = 1500;
    private static final long RECHECK_MS = 60L * 60 * 1000;

    // Process-wide: the dialog shows at most once per process, even across activity recreation.
    private static boolean promptedThisProcess;
    private static boolean shownThisProcess;
    private static long lastCheckAt;          // elapsedRealtime; 0 = not checked in this process
    private static int lastInstallStatus = InstallStatus.UNKNOWN;

    private Activity activity;
    private final Host host;
    private final UpdatePrefs prefs;
    private final UpdateUi ui;
    private final Handler main = new Handler(Looper.getMainLooper());
    private AppUpdateManager manager;
    private FakeAppUpdateManager fake;
    private boolean resumed;

    private final InstallStateUpdatedListener listener = state -> {
        lastInstallStatus = state.installStatus();
        if (state.installStatus() == InstallStatus.DOWNLOADED) main.post(this::showBarIfAlive);
    };
    private final Runnable checkRunnable = this::check;

    public WizUpdate(Activity activity, Host host, boolean debug) {
        this.activity = activity;
        this.host = host;
        this.prefs = new UpdatePrefs(activity.getApplicationContext());
        this.ui = new UpdateUi(activity);
        AppUpdateManager m = null;
        try {
            Intent i = activity.getIntent();
            if (debug && i != null && i.hasExtra(EXTRA_FAKE_UPDATE)) {
                if (i.getBooleanExtra(EXTRA_FAKE_RESET, false)) { prefs.clear(); promptedThisProcess = false; }
                fake = new FakeAppUpdateManager(activity.getApplicationContext());
                fake.setUpdateAvailable(i.getIntExtra(EXTRA_FAKE_UPDATE, 99));
                m = fake;
                lastCheckAt = 0;
            } else {
                m = AppUpdateManagerFactory.create(activity.getApplicationContext());
            }
        } catch (Throwable ignored) { }
        manager = m;
    }

    /** WizReview reads this: no review card in a process that already showed update UI. */
    public static boolean wasShownThisProcess() { return shownThisProcess; }

    public void onResume() {
        resumed = true;
        if (manager == null) return;
        try { manager.registerListener(listener); } catch (Throwable ignored) { }
        main.removeCallbacks(checkRunnable);
        long now = SystemClock.elapsedRealtime();
        if (lastCheckAt == 0) {
            main.postDelayed(checkRunnable, FIRST_CHECK_DELAY_MS);   // never compete with the first frame
        } else if (now - lastCheckAt > RECHECK_MS || inFlight(lastInstallStatus) || lastInstallStatus == InstallStatus.DOWNLOADED) {
            main.post(checkRunnable);
        }
    }

    public void onPause() {
        resumed = false;
        main.removeCallbacks(checkRunnable);
        if (manager == null) return;
        try { manager.unregisterListener(listener); } catch (Throwable ignored) { }
    }

    public void onDestroy() {
        resumed = false;
        main.removeCallbacksAndMessages(null);
        ui.dismissAll();
        activity = null;
    }

    private boolean alive() {
        Activity a = activity;
        return a != null && resumed && !a.isFinishing() && !a.isDestroyed();
    }

    private void check() {
        if (!alive() || manager == null) return;
        try {
            manager.getAppUpdateInfo()
                    .addOnSuccessListener(this::onInfo)
                    .addOnFailureListener(e -> lastCheckAt = SystemClock.elapsedRealtime());
        } catch (Throwable t) {
            lastCheckAt = SystemClock.elapsedRealtime();
        }
    }

    private void onInfo(AppUpdateInfo info) {
        lastCheckAt = SystemClock.elapsedRealtime();
        lastInstallStatus = info.installStatus();
        if (!alive()) return;
        long now = System.currentTimeMillis();
        boolean flexibleAllowed;
        try { flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)); }
        catch (Throwable t) { flexibleAllowed = false; }
        boolean hostAllows;
        try { hostAllows = host.mayPromptNow(); } catch (Throwable t) { hostAllows = false; }
        UpdatePolicy.Action action = UpdatePolicy.decide(now,
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
                info.installStatus(), flexibleAllowed, info.availableVersionCode(),
                prefs.lastPromptAt(), prefs.lastPromptVc(), promptedThisProcess, hostAllows);
        switch (action) {
            case RESTART_BAR:
                showBarIfAlive();
                break;
            case PROMPT_FLEXIBLE:
            case PROMPT_STORE_LINK:
                if (ui.dialogShowing()) return;
                final boolean flexible = action == UpdatePolicy.Action.PROMPT_FLEXIBLE;
                prefs.markPrompted(now, info.availableVersionCode());   // on show, not on tap
                promptedThisProcess = true;
                shownThisProcess = true;
                ui.showDialog(host.palette(), host.appName(), () -> {
                    if (flexible) startFlexible(info); else openStore();
                });
                break;
            default:
                break;
        }
    }

    private void startFlexible(AppUpdateInfo info) {
        Activity a = activity;
        if (a == null || manager == null) return;
        boolean started;
        try {
            started = manager.startUpdateFlowForResult(info, a,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(), REQUEST_CODE);
        } catch (Throwable t) {   // IntentSender.SendIntentException and anything else
            started = false;
        }
        if (!started) { openStore(); return; }
        if (fake != null) {   // debug hook: play the user's and Play's part so the bar can be checked
            fake.userAcceptsUpdate();
            fake.downloadStarts();
            main.postDelayed(() -> { if (fake != null) fake.downloadCompletes(); }, 1500);
        }
    }

    private void showBarIfAlive() {
        if (!alive() || ui.barShowing()) return;
        shownThisProcess = true;
        String note;
        try { note = host.restartNote(); } catch (Throwable t) { note = null; }
        ui.showBar(host.palette(), note, () -> {
            try { return host.barBottomOffsetPx(); } catch (Throwable t) { return 0; }
        }, this::restart);
    }

    private void restart() {
        if (manager == null) return;
        try { manager.completeUpdate(); } catch (Throwable ignored) { }
        if (fake != null) {
            fake.installCompletes();
            lastInstallStatus = InstallStatus.INSTALLED;
            ui.hideBar();
        }
    }

    private void openStore() {
        Activity a = activity;
        if (a == null) return;
        String pkg = a.getPackageName();
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (ActivityNotFoundException e) {
            try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg))); }
            catch (RuntimeException ignored) { }
        }
    }

    private static boolean inFlight(int s) {
        return s == InstallStatus.PENDING || s == InstallStatus.DOWNLOADING || s == InstallStatus.INSTALLING;
    }
}
