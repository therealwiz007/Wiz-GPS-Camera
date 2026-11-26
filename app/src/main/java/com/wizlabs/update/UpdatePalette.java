package com.wizlabs.update;

/** Six ARGB colours and a corner radius from the app, read each time UI is shown (light/dark stays current). */
public final class UpdatePalette {
    public final int surface, line, text, muted, accent, onAccent, cornerDp;

    public UpdatePalette(int surface, int line, int text, int muted, int accent, int onAccent, int cornerDp) {
        this.surface = surface; this.line = line; this.text = text; this.muted = muted;
        this.accent = accent; this.onAccent = onAccent; this.cornerDp = cornerDp;
    }
}
