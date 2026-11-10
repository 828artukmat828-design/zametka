package com.mytasks.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/** Мелкие помощники для построения интерфейса из кода. */
final class Ui {

    static final int PRIMARY = 0xFF5B3FE0;
    static final int PRIMARY_DARK = 0xFF4A2FC4;
    static final int PRIMARY_LIGHT = 0xFFEDE9FE;
    static final int BG = 0xFFF6F5FB;
    static final int WHITE = 0xFFFFFFFF;
    static final int TEXT = 0xFF1B1B2F;
    static final int MUTED = 0xFF6B6B80;
    static final int RED = 0xFFE5484D;
    static final int GREEN = 0xFF1E8E5A;

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Ui() {
    }

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Положительное число считается dp, отрицательное (MATCH/WRAP) и 0 остаются как есть. */
    private static int sz(Context c, int v) {
        return v > 0 ? dp(c, v) : v;
    }

    static GradientDrawable bg(Context c, int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    static LinearLayout.LayoutParams lp(Context c, int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(sz(c, w), sz(c, h));
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    static LinearLayout.LayoutParams lpw(Context c, int w, int h, float weight) {
        return new LinearLayout.LayoutParams(sz(c, w), sz(c, h), weight);
    }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return t;
    }

    static TextView button(Context c, String s, boolean primary) {
        TextView t = text(c, s, 15, primary ? WHITE : PRIMARY, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 14), dp(c, 13), dp(c, 14), dp(c, 13));
        t.setBackground(bg(c, primary ? PRIMARY : PRIMARY_LIGHT, 14));
        t.setClickable(true);
        return t;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 14), dp(c, 14), dp(c, 14), dp(c, 14));
        l.setBackground(bg(c, WHITE, 16));
        return l;
    }

    /** Полоска прогресса: done из total. */
    static View progress(Context c, int done, int total) {
        LinearLayout track = new LinearLayout(c);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(bg(c, PRIMARY_LIGHT, 5));

        View fill = new View(c);
        fill.setBackground(bg(c, PRIMARY, 5));
        View rest = new View(c);

        float d = total == 0 ? 0f : done;
        float r = total == 0 ? 1f : (total - done);
        track.addView(fill, new LinearLayout.LayoutParams(0, MATCH, d));
        track.addView(rest, new LinearLayout.LayoutParams(0, MATCH, r));
        return track;
    }

    static String money(long v) {
        return String.format(Locale.US, "%,d", v).replace(',', ' ') + " сўм";
    }
}
