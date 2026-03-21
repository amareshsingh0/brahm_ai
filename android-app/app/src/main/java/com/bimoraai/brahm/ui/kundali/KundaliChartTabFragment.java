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
 * Tab 0 — Kundali Chart
 *
 * Shows the custom KundaliChartView (South-Indian / North-Indian diamond canvas)
 * plus a horizontal summary row with lagna and key planet positions below.
 *
 * TODO: After API integration, observe the shared ViewModel for kundali data and
 *       call chartView.setData(kundaliData) to render real planetary positions.
 *       Also populate the planet summary row from the response.
 */
public class KundaliChartTabFragment extends Fragment {

    // Root views kept for post-creation data injection
    private KundaliChartView chartView;
    private LinearLayout     contentLayout;
    private TextView         tvSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return buildView(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadFromPrefs();
    }

    /**
     * Reads the cached kundali JSON from PrefsHelper and wires real data into
     * the chart view and summary row.
     */
    private void loadFromPrefs() {
        String json = new PrefsHelper(requireContext()).getKundaliJson();
        if (json.isEmpty()) return;
        try {
            JsonObject kundali = JsonParser.parseString(json).getAsJsonObject();

            // Chart view
            if (chartView != null) {
                if (kundali.has("grahas")) {
                    chartView.setPlanetsFromJson(kundali.getAsJsonArray("grahas"));
                }
                if (kundali.has("lagna")) {
                    JsonObject lagna = kundali.getAsJsonObject("lagna");
                    if (lagna.has("house"))
                        chartView.setLagnaHouse(lagna.get("house").getAsInt());
                }
                chartView.invalidate();
            }

            // Planet summary text
            if (tvSummary != null && kundali.has("grahas")) {
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : kundali.getAsJsonArray("grahas")) {
                    JsonObject g = el.getAsJsonObject();
                    String name  = g.has("name")  ? g.get("name").getAsString()  : "";
                    String rashi = g.has("rashi") ? g.get("rashi").getAsString() : "";
                    String house = g.has("house") ? g.get("house").getAsString() : "";
                    sb.append(name).append(": ").append(rashi)
                      .append(" (H").append(house).append(")  ");
                }
                tvSummary.setText(sb.toString().trim());
            }
        } catch (Exception e) {
            // Malformed cache — ignore
        }
    }

    // ── View ──────────────────────────────────────────────────────────────────

    private View buildView(Context ctx) {
        // Root scroll
        ScrollView scroll = new ScrollView(ctx);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16));
        scroll.addView(content);

        // ── Kundali Chart Canvas ─────────────────────────────────────────────
        chartView = new KundaliChartView(ctx);
        int chartSize = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.92f);
        LinearLayout.LayoutParams chartLp =
            new LinearLayout.LayoutParams(chartSize, chartSize);
        chartLp.gravity      = Gravity.CENTER_HORIZONTAL;
        chartLp.bottomMargin = dp(ctx, 20);
        chartView.setLayoutParams(chartLp);
        content.addView(chartView);

        // TODO: chartView.setData(kundaliData) — wire after API integration

        // ── Lagna / planet summary row ───────────────────────────────────────
        content.addView(sectionHeader(ctx, "Lagna & Planet Summary"));

        // Summary text — filled by loadFromPrefs()
        tvSummary = new TextView(ctx);
        tvSummary.setText("Loading planet summary…");
        tvSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvSummary.setLineSpacing(dp(ctx, 2), 1f);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = dp(ctx, 12);
        tvSummary.setLayoutParams(slp);
        TypedValue ov = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, ov, true);
        tvSummary.setTextColor(ov.data);
        content.addView(tvSummary);

        return scroll;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static TextView sectionHeader(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TypedValue primary = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, primary, true);
        tv.setTextColor(primary.data);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static View placeholderCard(Context ctx, String message) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 12));
        TypedValue surfaceVar = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant, surfaceVar, true);
        bg.setColor(surfaceVar.data);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private static int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            ctx.getResources().getDisplayMetrics());
    }
}
