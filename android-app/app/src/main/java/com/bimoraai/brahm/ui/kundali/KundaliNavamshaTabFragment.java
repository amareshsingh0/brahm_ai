package com.bimoraai.brahm.ui.kundali;

import android.content.Context;
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
 * Tab 6 — Navamsha (D-9 Chart)
 *
 * Shows the D-9 Navamsha chart using KundaliChartView, D-9 lagna,
 * and each planet's navamsha rashi. Data loaded from cached kundali JSON.
 */
public class KundaliNavamshaTabFragment extends Fragment {

    private static final String[] PLANET_NAMES = {
        "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu"
    };

    private static final int[] PLANET_COLORS = {
        0xFFFF6B35, 0xFFD4D4FF, 0xFFFF4444, 0xFF4CAF50,
        0xFFFFD700, 0xFFFF80AB, 0xFF9E9E9E, 0xFF7B1FA2, 0xFF795548
    };

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

        // ── D-9 Chart label ───────────────────────────────────────────────────
        content.addView(buildLabel(ctx, "D-9 Navamsha Chart"));

        // ── KundaliChartView for D-9 ──────────────────────────────────────────
        KundaliChartView chartView = new KundaliChartView(ctx);
        int chartSize = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
        LinearLayout.LayoutParams chartLp =
            new LinearLayout.LayoutParams(chartSize, chartSize);
        chartLp.gravity      = Gravity.CENTER_HORIZONTAL;
        chartLp.bottomMargin = dp(ctx, 20);
        chartView.setLayoutParams(chartLp);
        content.addView(chartView);

        // ── Load D-9 data from cache ───────────────────────────────────────────
        content.addView(buildLabel(ctx, "Navamsha Lagna & Planets"));

        String[] navamshaData = loadFromPrefs(ctx, chartView);
        String lagnaRashi = navamshaData[0]; // first element is lagna
        content.addView(buildLagnaCard(ctx, lagnaRashi));

        // Planet rows
        LinearLayout grid = buildPlanetGrid(ctx, navamshaData);
        content.addView(grid);

        // Note
        content.addView(buildNote(ctx,
            "Vargottama: A planet in the same rashi in both D-1 and D-9 is " +
            "considered extra strong."));

        return scroll;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    /**
     * Loads D-9 navamsha data from cached kundali JSON.
     * Returns String[10]: index 0 = lagna rashi, indices 1-9 = planet rashis (Sun…Ketu).
     * Also populates chartView with D-9 planet positions.
     */
    private String[] loadFromPrefs(Context ctx, KundaliChartView chartView) {
        String[] result = new String[10];
        result[0] = "—"; // lagna
        for (int i = 1; i < 10; i++) result[i] = "—";

        String json = new PrefsHelper(ctx).getKundaliJson();
        if (json.isEmpty()) return result;

        try {
            JsonObject kundali = JsonParser.parseString(json).getAsJsonObject();
            if (!kundali.has("navamsha")) return result;

            JsonObject navamsha = kundali.getAsJsonObject("navamsha");

            // Lagna
            if (navamsha.has("lagna")) {
                JsonObject lag = navamsha.getAsJsonObject("lagna");
                result[0] = lag.has("rashi") ? lag.get("rashi").getAsString() : "—";
            }

            // Planets
            if (navamsha.has("grahas")) {
                JsonArray grahas = navamsha.getAsJsonArray("grahas");
                chartView.setPlanetsFromJson(grahas);

                for (JsonElement el : grahas) {
                    JsonObject g = el.getAsJsonObject();
                    String name  = g.has("name")  ? g.get("name").getAsString()  : "";
                    String rashi = g.has("rashi") ? g.get("rashi").getAsString() : "—";
                    for (int i = 0; i < PLANET_NAMES.length; i++) {
                        if (PLANET_NAMES[i].equalsIgnoreCase(name)) {
                            result[i + 1] = rashi;
                            break;
                        }
                    }
                }
                chartView.invalidate();
            }
        } catch (Exception ignored) {}

        return result;
    }

    // ── View builders ─────────────────────────────────────────────────────────

    private View buildLabel(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TypedValue primary = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, primary, true);
        tv.setTextColor(primary.data);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 10);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View buildLagnaCard(Context ctx, String lagnaRashi) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 12));
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimaryContainer, tv, true);
        bg.setColor(tv.data);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 16);
        card.setLayoutParams(lp);

        TypedValue onPrimary = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnPrimaryContainer, onPrimary, true);

        TextView tvLabel = new TextView(ctx);
        tvLabel.setText("D-9 Lagna:");
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvLabel.setTextColor(onPrimary.data);
        card.addView(tvLabel, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvValue = new TextView(ctx);
        tvValue.setText("  " + lagnaRashi);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvValue.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvValue.setTextColor(onPrimary.data);
        card.addView(tvValue, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return card;
    }

    private LinearLayout buildPlanetGrid(Context ctx, String[] navamshaData) {
        LinearLayout grid = new LinearLayout(ctx);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.bottomMargin = dp(ctx, 16);
        grid.setLayoutParams(gridLp);

        TypedValue onSurface = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface, onSurface, true);
        TypedValue surfaceVar = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant, surfaceVar, true);

        for (int i = 0; i < PLANET_NAMES.length; i++) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10));

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(ctx, 8));
            bg.setColor(surfaceVar.data);
            row.setBackground(bg);

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(ctx, 6);
            row.setLayoutParams(rowLp);

            TextView tvPlanet = new TextView(ctx);
            tvPlanet.setText(PLANET_NAMES[i]);
            tvPlanet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvPlanet.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvPlanet.setTextColor(PLANET_COLORS[i]);
            row.addView(tvPlanet, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            String rashiText = (i + 1 < navamshaData.length) ? navamshaData[i + 1] : "—";
            TextView tvRashi = new TextView(ctx);
            tvRashi.setText(rashiText);
            tvRashi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvRashi.setTextColor(onSurface.data);
            tvRashi.setGravity(Gravity.END);
            row.addView(tvRashi, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            grid.addView(row);
        }

        return grid;
    }

    private View buildNote(Context ctx, String text) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(ctx, 14), dp(ctx, 14), dp(ctx, 14), dp(ctx, 14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 10));
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
        bg.setColor(tv.data);
        card.setBackground(bg);
        card.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView tvNote = new TextView(ctx);
        tvNote.setText(text);
        tvNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvNote.setLineSpacing(dp(ctx, 2), 1f);
        TypedValue variant = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, variant, true);
        tvNote.setTextColor(variant.data);
        card.addView(tvNote);
        return card;
    }

    private int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }
}
