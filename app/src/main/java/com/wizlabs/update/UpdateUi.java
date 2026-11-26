package com.wizlabs.update;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.PowerManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The update dialog and the "Restart" bar, built in code (no XML, no resources), so the same
 * source works in a Views app, a Compose app and a plain-Activity WebView app (plan 3.6).
 */
final class UpdateUi {
    interface BarHost { int extraBottomPx(); }

    private final Activity activity;
    private Dialog dialog;
    private View bar;

    UpdateUi(Activity activity) { this.activity = activity; }

    boolean dialogShowing() { return dialog != null && dialog.isShowing(); }
    boolean barShowing() { return bar != null && bar.getParent() != null; }

    /** Always cancelable: Back, a tap outside and "Later" all just close it. */
    void showDialog(UpdatePalette pal, String appName, final Runnable onUpdate) {
        dismissDialog();
        final Dialog d = new Dialog(activity);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setCancelable(true);
        d.setCanceledOnTouchOutside(true);

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        card.setPadding(pad, pad, pad, dp(16));
        card.setBackground(rounded(pal.surface, pal.line, pal.cornerDp));

        TextView title = text(UpdateStrings.TITLE, 18, pal.text);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(title);
        TextView body = text(UpdateStrings.body(appName), 14, pal.text);
        body.setPadding(0, dp(10), 0, 0);
        card.addView(body);
        TextView hi = text(UpdateStrings.BODY_HI, 14, pal.muted);
        hi.setPadding(0, dp(4), 0, 0);
        card.addView(hi);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(18), 0, 0);
        TextView later = button(UpdateStrings.LATER, pal.muted, Color.TRANSPARENT, pal.text);
        later.setOnClickListener(v -> d.cancel());
        TextView update = button(UpdateStrings.UPDATE, pal.onAccent, pal.accent, pal.onAccent);
        update.setOnClickListener(v -> { d.dismiss(); onUpdate.run(); });
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        gap.setMarginStart(dp(8));
        row.addView(later, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
        row.addView(update, gap);
        card.addView(row);

        d.setContentView(card);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setDimAmount(0.45f);
            w.setWindowAnimations(0);
            int screen = activity.getResources().getDisplayMetrics().widthPixels;
            w.setLayout(Math.min(dp(320), screen - dp(32)), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (motionOn()) { card.setAlpha(0f); card.animate().alpha(1f).setDuration(150).start(); }
        dialog = d;
        d.show();
    }

    /** Bottom bar over the content, like a snackbar, clear of the navigation bar and keyboard. */
    void showBar(UpdatePalette pal, String note, final BarHost host, final Runnable onRestart) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;
        hideBar();
        LinearLayout b = new LinearLayout(activity);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(dp(18), dp(10), dp(10), dp(10));
        b.setBackground(rounded(pal.surface, pal.line, pal.cornerDp));
        b.setElevation(dp(6));
        b.setClickable(true);   // touches on the bar never fall through to the screen below

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t1 = text(UpdateStrings.BAR, 14, pal.text);
        t1.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        texts.addView(t1);
        texts.addView(text(UpdateStrings.BAR_HI, 13, pal.muted));
        if (note != null && !note.isEmpty()) texts.addView(text(note, 13, pal.muted));
        b.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView restart = button(UpdateStrings.RESTART, pal.onAccent, pal.accent, pal.onAccent);
        restart.setOnClickListener(v -> onRestart.run());
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        rp.setMarginStart(dp(12));
        b.addView(restart, rp);

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.leftMargin = dp(16);
        lp.rightMargin = dp(16);
        lp.bottomMargin = dp(16) + systemBottom(activity.getWindow().getDecorView().getRootWindowInsets()) + host.extraBottomPx();
        b.setOnApplyWindowInsetsListener((v, insets) -> {
            int m = dp(16) + systemBottom(insets) + host.extraBottomPx();
            ViewGroup.LayoutParams cur = v.getLayoutParams();
            if (cur instanceof FrameLayout.LayoutParams && ((FrameLayout.LayoutParams) cur).bottomMargin != m) {
                ((FrameLayout.LayoutParams) cur).bottomMargin = m;
                v.setLayoutParams(cur);
            }
            return insets;
        });
        ((FrameLayout) content).addView(b, lp);
        b.requestApplyInsets();
        if (motionOn()) {
            b.setAlpha(0f);
            b.setTranslationY(dp(12));
            b.animate().alpha(1f).translationY(0f).setDuration(150).start();
        }
        bar = b;
    }

    void dismissDialog() {
        if (dialog != null) { try { dialog.dismiss(); } catch (RuntimeException ignored) { } }
        dialog = null;
    }

    void hideBar() {
        if (bar != null && bar.getParent() instanceof ViewGroup) ((ViewGroup) bar.getParent()).removeView(bar);
        bar = null;
    }

    void dismissAll() { dismissDialog(); hideBar(); }

    /* ---- helpers ---- */

    private int systemBottom(WindowInsets in) {
        if (in == null) return 0;
        if (Build.VERSION.SDK_INT >= 30) {
            return in.getInsets(WindowInsets.Type.navigationBars() | WindowInsets.Type.ime()).bottom;
        }
        return in.getSystemWindowInsetBottom();
    }

    private boolean motionOn() {
        try {
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isPowerSaveMode()) return false;
        } catch (RuntimeException ignored) { }
        return Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setLineSpacing(0f, 1.15f);
        return t;
    }

    private TextView button(String label, int textColor, int fill, int rippleBase) {
        TextView t = text(label, 15, textColor);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setGravity(Gravity.CENTER);
        t.setMinWidth(dp(64));
        t.setPadding(dp(18), 0, dp(18), 0);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(24));
        shape.setColor(fill);
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(24));
        mask.setColor(Color.BLACK);
        int ripple = (rippleBase & 0x00FFFFFF) | 0x22000000;
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple), shape, mask));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    private GradientDrawable rounded(int fill, int stroke, int cornerDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(cornerDp));
        g.setStroke(Math.max(1, dp(1) / 2 + 1), stroke);
        return g;
    }

    private int dp(int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}
