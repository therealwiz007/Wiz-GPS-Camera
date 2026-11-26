package com.wizlabs.update;

import android.content.Context;
import android.content.SharedPreferences;

/** SharedPreferences "wiz_update": when the dialog was last shown, and for which versionCode. */
final class UpdatePrefs {
    private final SharedPreferences p;

    UpdatePrefs(Context ctx) { p = ctx.getSharedPreferences("wiz_update", Context.MODE_PRIVATE); }

    long lastPromptAt() { return p.getLong("last_prompt_at", 0L); }
    int lastPromptVc() { return p.getInt("last_prompt_vc", -1); }

    void markPrompted(long now, int vc) { p.edit().putLong("last_prompt_at", now).putInt("last_prompt_vc", vc).apply(); }

    void clear() { p.edit().clear().apply(); }
}
