package com.wizlabs.update;

/** Pure decision function for the update popup (plan 3.3). No Android imports, unit-tested on the JVM. */
final class UpdatePolicy {
    private UpdatePolicy() {}

    enum Action { NONE, PROMPT_FLEXIBLE, PROMPT_STORE_LINK, RESTART_BAR }

    // Same values as com.google.android.play.core.install.model.InstallStatus (checked against app-update 2.1.0).
    static final int PENDING = 1, DOWNLOADING = 2, INSTALLING = 3, DOWNLOADED = 11;

    static final long DAY_MS = 24L * 60 * 60 * 1000;

    /** First match wins. The popup is always dismissible; nothing here can block the app. */
    static Action decide(long now, boolean updateAvailable, int installStatus,
                         boolean flexibleAllowed, int availableVc,
                         long lastPromptAt, int lastPromptVc,
                         boolean promptedThisProcess, boolean hostAllows) {
        if (installStatus == DOWNLOADED) return Action.RESTART_BAR;
        if (installStatus == PENDING || installStatus == DOWNLOADING || installStatus == INSTALLING) return Action.NONE;
        if (!updateAvailable) return Action.NONE;
        if (promptedThisProcess || !hostAllows) return Action.NONE;
        boolean due = availableVc != lastPromptVc || now - lastPromptAt >= DAY_MS || now < lastPromptAt;
        if (!due) return Action.NONE;
        return flexibleAllowed ? Action.PROMPT_FLEXIBLE : Action.PROMPT_STORE_LINK;
    }
}
