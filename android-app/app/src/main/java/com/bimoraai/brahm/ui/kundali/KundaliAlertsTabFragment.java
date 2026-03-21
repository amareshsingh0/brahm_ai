package com.bimoraai.brahm.ui.kundali;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Tab 4 — Alerts
 *
 * Shows coloured alert cards for planetary war, combustion, and other
 * critical placements detected by kundali_service.py's _detect_alerts().
 *
 * Severity levels:
 *   HIGH    — red border (#F44336)
 *   MEDIUM  — amber border (#FF9800)
 *   LOW     — blue border (#2196F3)
 */
public class KundaliAlertsTabFragment extends Fragment {

    private enum Severity {
        HIGH   ("#F44336", "⚠"),
        MEDIUM ("#FF9800", "!"),
        LOW    ("#2196F3", "ℹ");

        final String hex;
        final String icon;
        Severity(String hex, String icon) { this.hex = hex; this.icon = icon; }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();

        ScrollView scroll = new ScrollView(ctx);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16));
        scroll.addView(content);

        // Header
        content.addView(buildHeader(ctx));

        // Load real alert data from cached kundali JSON
        loadFromPrefs(ctx, content);

        return scroll;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadFromPrefs(Context ctx, LinearLayout container) {
        String json = new PrefsHelper(ctx).getKundaliJson();
        if (json.isEmpty()) {
            container.addView(buildInfoCard(ctx,
                "Generate your Kundali to see planetary alerts."));
            return;
        }
        try {
            JsonObject kundali = JsonParser.parseString(json).getAsJsonObject();
            JsonArray alerts = kundali.has("alerts")
                ? kundali.getAsJsonArray("alerts") : new JsonArray();

            if (alerts.size() == 0) {
                container.addView(buildInfoCard(ctx,
                    "No critical alerts detected in your chart."));
                return;
            }

            for (JsonElement el : alerts) {
                JsonObject a = el.getAsJsonObject();
                String title  = a.has("title")    ? a.get("title").getAsString()   : "Alert";
                String body   = a.has("message")  ? a.get("message").getAsString()
                              : a.has("body")      ? a.get("body").getAsString()    : "";
                String sevStr = a.has("severity")
                    ? a.get("severity").getAsString().toUpperCase() : "LOW";
                Severity sev;
                try { sev = Severity.valueOf(sevStr); } catch (Exception e) { sev = Severity.LOW; }
                container.addView(buildAlertCard(ctx, title, body, sev));
            }
        } catch (Exception e) {
            container.addView(buildInfoCard(ctx, "Could not parse alert data."));
        }
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    private View buildAlertCard(Context ctx, String title, String body, Severity severity) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 12));
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurface, tv, true);
        bg.setColor(tv.data);
        bg.setStroke(dp(ctx, 3), Color.parseColor(severity.hex));
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 12);
        card.setLayoutParams(lp);

        // Title row: severity icon + title
        LinearLayout titleRow = new LinearLayout(ctx);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(ctx, 8);
        titleRow.setLayoutParams(rowLp);

        // Severity icon badge
        TextView tvIcon = new TextView(ctx);
        tvIcon.setText(severity.icon);
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvIcon.setTextColor(Color.WHITE);
        tvIcon.setPadding(dp(ctx, 8), dp(ctx, 4), dp(ctx, 8), dp(ctx, 4));
        tvIcon.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(Color.parseColor(severity.hex));
        tvIcon.setBackground(iconBg);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
            dp(ctx, 28), dp(ctx, 28));
        iconLp.rightMargin = dp(ctx, 10);
        tvIcon.setLayoutParams(iconLp);
        titleRow.addView(tvIcon);

        // Title text
        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(Color.parseColor(severity.hex));
        titleRow.addView(tvTitle, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(titleRow);

        // Body
        TextView tvBody = new TextView(ctx);
        tvBody.setText(body);
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvBody.setLineSpacing(dp(ctx, 2), 1f);
        TypedValue variant = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, variant, true);
        tvBody.setTextColor(variant.data);
        card.addView(tvBody);

        return card;
    }

    private View buildHeader(Context ctx) {
        TextView tv = new TextView(ctx);
        tv.setText("Planetary Alerts");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TypedValue primary = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, primary, true);
        tv.setTextColor(primary.data);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 12);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View buildInfoCard(Context ctx, String message) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 12));
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
        bg.setColor(tv.data);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 12);
        card.setLayoutParams(lp);
        TextView tvMsg = new TextView(ctx);
        tvMsg.setText(message);
        tvMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvMsg.setLineSpacing(dp(ctx, 2), 1f);
        TypedValue variant = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, variant, true);
        tvMsg.setTextColor(variant.data);
        card.addView(tvMsg);
        return card;
    }

    private int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }
}
