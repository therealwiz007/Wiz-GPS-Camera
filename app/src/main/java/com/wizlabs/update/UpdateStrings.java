package com.wizlabs.update;

/** Texts from WIZ_3APPS_MASTER_PLAN.md 3.5. English with one Hindi line, whatever the phone language. */
final class UpdateStrings {
    private UpdateStrings() {}

    static final String TITLE = "Update available";
    static String body(String app) { return "A newer version of " + app + " is ready on Google Play."; }
    static final String BODY_HI = "नया वर्ज़न Google Play पर उपलब्ध है।";
    static final String UPDATE = "Update";
    static final String LATER = "Later";
    static final String BAR = "Update ready. Restart to finish.";
    static final String BAR_HI = "अपडेट तैयार है।";
    static final String RESTART = "Restart";
}
