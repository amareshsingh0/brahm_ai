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
 * Tab 2 — Dashas
 *
 * Layout:
 *   1. Large "Active Dasha" card — Mahadasha planet, period, Antardasha planet.
 *   2. Scrollable list of all 9 Vimshottari Mahadasha periods.
 *
 * TODO: Observe shared ViewModel for dasha data and replace stub values.
 */
public class KundaliDashasTabFragment extends Fragment {

    // References to mutable views set during buildActiveDashaCard()
    private TextView tvActiveMaha;
    private TextView tvActivePeriod;
    private TextView tvActiveAntar;
    private LinearLayout dashaListContainer; // the scrollable rows container

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();

        ScrollView scroll = new ScrollView(ctx);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16));
        scroll.addView(content);

        // ── Active Dasha card ─────────────────────────────────────────────────
        content.addView(buildSectionLabel(ctx, "Active Dasha"));
        content.addView(buildActiveDashaCard(ctx));

        // ── All Mahadashas ────────────────────────────────────────────────────
        content.addView(buildSectionLabel(ctx, "Vimshottari Mahadasha Sequence"));

        // Keep a reference so loadFromPrefs() can replace stub rows with real ones
        dashaListContainer = content;

        String[][] mahadashas = {
            {"Sun",     "6 years",  "—"},
            {"Moon",    "10 years", "—"},
            {"Mars",    "7 years",  "—"},
            {"Rahu",    "18 years", "—"},
            {"Jupiter", "16 years", "—"},
            {"Saturn",  "19 years", "—"},
            {"Mercury", "17 years", "—"},
            {"Ketu",    "7 years",  "—"},
            {"Venus",   "20 years", "—"},
        };

        for (String[] dasha : mahadashas) {
            content.addView(buildDashaRow(ctx, dasha[0], dasha[1], dasha[2]));
        }

        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadFromPrefs();
    }

    private void loadFromPrefs() {
        String json = new PrefsHelper(requireContext()).getKundaliJson();
        if (json.isEmpty()) return;
        try {
            JsonObject kundali = JsonParser.parseString(json).getAsJsonObject();
            if (!kundali.has("dashas")) return;

            JsonObject dashas = kundali.getAsJsonObject("dashas");

            // Active (current) mahadasha
            if (dashas.has("current_mahadasha")) {
                JsonObject current = dashas.getAsJsonObject("current_mahadasha");
                if (tvActiveMaha != null && current.has("planet"))
                    tvActiveMaha.setText(current.get("planet").getAsString());
                if (tvActivePeriod != null) {
                    String start = current.has("start") ? current.get("start").getAsString() : "—";
                    String end   = current.has("end")   ? current.get("end").getAsString()   : "—";
                    tvActivePeriod.setText("Period: " + start + " – " + end);
                }
            }

            // Active antardasha
            if (dashas.has("current_antardasha") && tvActiveAntar != null) {
                JsonObject antar = dashas.getAsJsonObject("current_antardasha");
                String ap = antar.has("planet") ? antar.get("planet").getAsString() : "—";
                tvActiveAntar.setText(ap);
            }

            // Mahadasha sequence
            if (dashas.has("mahadashas") && dashaListContainer != null) {
                dashaListContainer.removeAllViews();
                Context ctx = requireContext();
                JsonArray seq = dashas.getAsJsonArray("mahadashas");
                for (JsonElement el : seq) {
                    JsonObject d    = el.getAsJsonObject();
                    String planet   = d.has("planet") ? d.get("planet").getAsString() : "—";
                    String years    = d.has("years")  ? d.get("years").getAsString() + " years" : "—";
                    String period   = "";
                    if (d.has("start") && d.has("end"))
                        period = d.get("start").getAsString() + " – " + d.get("end").getAsString();
                    dashaListContainer.addView(buildDashaRow(ctx, planet, years, period));
                }
            }
        } catch (Exception ignored) { }
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    private View buildActiveDashaCard(Context ctx) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(ctx, 20), dp(ctx, 20), dp(ctx, 20), dp(ctx, 20));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 16));
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimaryContainer, tv, true);
        bg.setColor(tv.data);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 20);
        card.setLayoutParams(lp);

        // Mahadasha planet
        TextView tvLabel = new TextView(ctx);
        tvLabel.setText("Mahadasha");
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        TypedValue onPrimary = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnPrimaryContainer, onPrimary, true);
        tvLabel.setTextColor(onPrimary.data);
        card.addView(tvLabel);

        tvActiveMaha = new TextView(ctx);
        TextView tvMaha = tvActiveMaha;
        tvMaha.setText("—");
        tvMaha.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        tvMaha.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvMaha.setTextColor(onPrimary.data);
        LinearLayout.LayoutParams mahaLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mahaLp.bottomMargin = dp(ctx, 4);
        tvMaha.setLayoutParams(mahaLp);
        card.addView(tvMaha);

        // Period
        tvActivePeriod = new TextView(ctx);
        TextView tvPeriod = tvActivePeriod;
        tvPeriod.setText("Period: — to —");
        tvPeriod.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvPeriod.setTextColor(onPrimary.data);
        LinearLayout.LayoutParams periodLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        periodLp.bottomMargin = dp(ctx, 12);
        tvPeriod.setLayoutParams(periodLp);
        card.addView(tvPeriod);

        // Divider
        View divider = new View(ctx);
        divider.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 1));
        divLp.bottomMargin = dp(ctx, 12);
        divider.setLayoutParams(divLp);
        card.addView(divider);

        // Antardasha
        TextView tvAntarLabel = new TextView(ctx);
        tvAntarLabel.setText("Current Antardasha");
        tvAntarLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvAntarLabel.setTextColor(onPrimary.data);
        card.addView(tvAntarLabel);

        tvActiveAntar = new TextView(ctx);
        TextView tvAntar = tvActiveAntar;
        tvAntar.setText("—");
        tvAntar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvAntar.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvAntar.setTextColor(onPrimary.data);
        card.addView(tvAntar);

        // TODO: Populate from API — set tvMaha.setText(dasha.mahadasha),
        //       tvPeriod.setText(...), tvAntar.setText(dasha.antardasha)

        return card;
    }

    private View buildDashaRow(Context ctx, String planet, String duration, String period) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(ctx, 16), dp(ctx, 12), dp(ctx, 16), dp(ctx, 12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 10));
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
        bg.setColor(tv.data);
        row.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 8);
        row.setLayoutParams(lp);

        // Planet name
        TextView tvPlanet = new TextView(ctx);
        tvPlanet.setText(planet);
        tvPlanet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvPlanet.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TypedValue onSurface = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface, onSurface, true);
        tvPlanet.setTextColor(onSurface.data);
        row.addView(tvPlanet, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f));

        // Duration
        TextView tvDuration = new TextView(ctx);
        tvDuration.setText(duration);
        tvDuration.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        TypedValue variant = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, variant, true);
        tvDuration.setTextColor(variant.data);
        row.addView(tvDuration, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Period (start – end)
        TextView tvPeriod = new TextView(ctx);
        tvPeriod.setText(period);
        tvPeriod.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPeriod.setTextColor(variant.data);
        tvPeriod.setGravity(Gravity.END);
        row.addView(tvPeriod, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f));

        return row;
    }

    private TextView buildSectionLabel(Context ctx, String text) {
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

    private int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }
}
