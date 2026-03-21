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
 * Tab 3 — Yogas
 *
 * Shows yoga cards, each with: yoga name, description, and a strength badge.
 * Placeholder cards are displayed until the API is integrated.
 *
 * TODO: Observe ViewModel yoga list and call buildYogaCard() per yoga entry.
 */
public class KundaliYogasTabFragment extends Fragment {

    private LinearLayout yogaContainer; // holds the yoga cards

    /** Strength levels with display colours. */
    private enum Strength {
        STRONG  ("Strong",   "#4CAF50"),
        MODERATE("Moderate", "#FF9800"),
        WEAK    ("Weak",     "#F44336");

        final String label;
        final String hex;
        Strength(String label, String hex) { this.label = label; this.hex = hex; }
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

        // ── Header ────────────────────────────────────────────────────────────
        content.addView(buildHeader(ctx));

        // Keep a reference so onViewCreated can replace stubs with real data
        yogaContainer = content;

        // ── Placeholder yoga cards ────────────────────────────────────────────
        // These will be replaced by real data in onViewCreated if cached JSON exists
        content.addView(buildYogaCard(ctx,
            "Gajakesari Yoga",
            "Jupiter in kendra from Moon — blesses with wisdom, fame, and prosperity. " +
            "A highly auspicious yoga for leadership and philanthropy.",
            Strength.STRONG));

        content.addView(buildYogaCard(ctx,
            "Budhaditya Yoga",
            "Sun and Mercury conjunct — sharpens intellect, communication, and analytical ability. " +
            "Excellent for writers, speakers, and scholars.",
            Strength.MODERATE));

        content.addView(buildYogaCard(ctx,
            "Neech Bhang Raj Yoga",
            "Debilitation cancelled by special conditions — transforms weakness into strength. " +
            "Indicates rise after initial struggles.",
            Strength.WEAK));

        // Placeholder card for loading state
        content.addView(buildPlaceholderCard(ctx));

        // TODO: After API integration:
        // content.removeAllViews();
        // for (Yoga yoga : kundaliResponse.getYogas()) {
        //     content.addView(buildYogaCard(ctx, yoga.getName(), yoga.getDescription(),
        //                                   Strength.valueOf(yoga.getStrength().toUpperCase())));
        // }

        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadFromPrefs();
    }

    private void loadFromPrefs() {
        String json = new PrefsHelper(requireContext()).getKundaliJson();
        if (json.isEmpty() || yogaContainer == null) return;
        try {
            JsonObject kundali = JsonParser.parseString(json).getAsJsonObject();
            if (!kundali.has("yogas")) return;

            JsonArray yogas = kundali.getAsJsonArray("yogas");
            if (yogas.size() == 0) return;

            Context ctx = requireContext();
            // Remove the first child (header) and rebuild yoga cards
            View header = yogaContainer.getChildAt(0);
            yogaContainer.removeAllViews();
            if (header != null) yogaContainer.addView(header);

            for (JsonElement el : yogas) {
                JsonObject y    = el.getAsJsonObject();
                String name     = y.has("name")        ? y.get("name").getAsString()        : "—";
                String desc     = y.has("description") ? y.get("description").getAsString() : "";
                String strRaw   = y.has("strength")    ? y.get("strength").getAsString().toUpperCase() : "MODERATE";
                Strength str;
                try { str = Strength.valueOf(strRaw); } catch (Exception e) { str = Strength.MODERATE; }
                yogaContainer.addView(buildYogaCard(ctx, name, desc, str));
            }
        } catch (Exception ignored) { }
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    private View buildYogaCard(Context ctx, String name, String description, Strength strength) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 12));
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurface, tv, true);
        bg.setColor(tv.data);
        // Left border accent using strength colour
        bg.setStroke(dp(ctx, 3), Color.parseColor(strength.hex));
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 12);
        card.setLayoutParams(lp);

        // Top row: name + strength badge
        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(ctx, 8);
        topRow.setLayoutParams(rowLp);

        TextView tvName = new TextView(ctx);
        tvName.setText(name);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TypedValue onSurface = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface, onSurface, true);
        tvName.setTextColor(onSurface.data);
        topRow.addView(tvName, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Strength badge
        TextView tvBadge = new TextView(ctx);
        tvBadge.setText(strength.label);
        tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvBadge.setTextColor(Color.WHITE);
        tvBadge.setPadding(dp(ctx, 10), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(ctx, 12));
        badgeBg.setColor(Color.parseColor(strength.hex));
        tvBadge.setBackground(badgeBg);
        topRow.addView(tvBadge);

        card.addView(topRow);

        // Description
        TextView tvDesc = new TextView(ctx);
        tvDesc.setText(description);
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvDesc.setLineSpacing(dp(ctx, 2), 1f);
        TypedValue variant = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, variant, true);
        tvDesc.setTextColor(variant.data);
        card.addView(tvDesc);

        return card;
    }

    private View buildHeader(Context ctx) {
        TextView tv = new TextView(ctx);
        tv.setText("Yogas present in your chart");
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

    private View buildPlaceholderCard(Context ctx) {
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
        tvMsg.setText("Real yoga data will load after API integration.\n" +
                       "All yogas in your chart will appear here with\n" +
                       "name, description, and strength.");
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
