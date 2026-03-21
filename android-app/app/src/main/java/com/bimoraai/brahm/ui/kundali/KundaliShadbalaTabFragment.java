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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab 5 — Shadbala
 *
 * Displays a table showing the six Shadbala strength scores for the seven
 * classical planets. Data is loaded from the cached kundali JSON in PrefsHelper.
 *
 * Columns: Planet | Sthana Bala | Dig Bala | Kaala Bala | Chesta Bala | Naisargika | Total
 */
public class KundaliShadbalaTabFragment extends Fragment {

    private static final String[] PLANETS = {
        "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"
    };

    private static final int[] PLANET_COLORS = {
        0xFFFF6B35, 0xFFD4D4FF, 0xFFFF4444, 0xFF4CAF50,
        0xFFFFD700, 0xFFFF80AB, 0xFF9E9E9E
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

        // Header
        content.addView(buildHeader(ctx));

        // Column headers row
        content.addView(buildColumnHeaders(ctx));

        // Table via RecyclerView
        RecyclerView rv = new RecyclerView(ctx);
        rv.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rv.setNestedScrollingEnabled(false);
        rv.setLayoutManager(new LinearLayoutManager(ctx));
        rv.addItemDecoration(new DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL));

        List<float[]> shadbalaData = loadFromPrefs(ctx);
        ShadbalaAdapter adapter = new ShadbalaAdapter(shadbalaData);
        rv.setAdapter(adapter);
        content.addView(rv);

        // Info card — show tip about Rupas unit
        content.addView(buildInfoCard(ctx,
            "Shadbala values are in Rupas. Higher total = stronger planet.\n" +
            "Green ≥ 6  •  Amber 4–6  •  Red < 4"));

        return scroll;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private List<float[]> loadFromPrefs(Context ctx) {
        List<float[]> result = new ArrayList<>();
        // Initialize with zeros (placeholder "—")
        for (int i = 0; i < PLANETS.length; i++) {
            result.add(new float[]{0, 0, 0, 0, 0, 0});
        }

        String json = new PrefsHelper(ctx).getKundaliJson();
        if (json.isEmpty()) return result;

        try {
            JsonObject kundali = JsonParser.parseString(json).getAsJsonObject();
            if (!kundali.has("shadbala")) return result;

            JsonObject shadbala = kundali.getAsJsonObject("shadbala");
            for (int i = 0; i < PLANETS.length; i++) {
                String key = PLANETS[i].toLowerCase();
                if (!shadbala.has(key)) continue;
                JsonObject p = shadbala.getAsJsonObject(key);
                float sthana   = p.has("sthana_bala")    ? p.get("sthana_bala").getAsFloat()    : 0;
                float dig      = p.has("dig_bala")       ? p.get("dig_bala").getAsFloat()       : 0;
                float kaala    = p.has("kaala_bala")     ? p.get("kaala_bala").getAsFloat()     : 0;
                float chesta   = p.has("chesta_bala")    ? p.get("chesta_bala").getAsFloat()    : 0;
                float naisa    = p.has("naisargika_bala") ? p.get("naisargika_bala").getAsFloat() : 0;
                float total    = p.has("total")          ? p.get("total").getAsFloat()          : 0;
                result.set(i, new float[]{sthana, dig, kaala, chesta, naisa, total});
            }
        } catch (Exception ignored) {}

        return result;
    }

    // ── View builders ─────────────────────────────────────────────────────────

    private View buildHeader(Context ctx) {
        TextView tv = new TextView(ctx);
        tv.setText("Shadbala — Planetary Strength Scores");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TypedValue primary = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, primary, true);
        tv.setTextColor(primary.data);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View buildColumnHeaders(Context ctx) {
        LinearLayout row = buildRow(ctx, true);

        String[] cols = {"Planet", "Sthana", "Dig", "Kaala", "Chesta", "Naisa.", "Total"};
        float[]  wts  = {1.4f,      1f,       0.8f,  1f,      1f,       1f,       1f};

        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, tv, true);

        for (int i = 0; i < cols.length; i++) {
            TextView cell = new TextView(ctx);
            cell.setText(cols[i]);
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            cell.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            cell.setTextColor(tv.data);
            cell.setGravity(i == 0 ? Gravity.START : Gravity.CENTER);
            row.addView(cell, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, wts[i]));
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 4);
        row.setLayoutParams(lp);
        return row;
    }

    private View buildInfoCard(Context ctx, String message) {
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
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 12);
        card.setLayoutParams(lp);
        TextView tvMsg = new TextView(ctx);
        tvMsg.setText(message);
        tvMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvMsg.setLineSpacing(dp(ctx, 2), 1f);
        TypedValue variant = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, variant, true);
        tvMsg.setTextColor(variant.data);
        card.addView(tvMsg);
        return card;
    }

    private LinearLayout buildRow(Context ctx, boolean isHeader) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(ctx, 8), dp(ctx, isHeader ? 10 : 12),
                       dp(ctx, 8), dp(ctx, isHeader ? 10 : 12));
        return row;
    }

    // ── Inline Adapter ────────────────────────────────────────────────────────

    private class ShadbalaAdapter extends RecyclerView.Adapter<ShadbalaAdapter.VH> {

        private final List<float[]> data;

        ShadbalaAdapter(List<float[]> data) { this.data = new ArrayList<>(data); }

        public void updateData(List<float[]> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(buildRowView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            float[] scores = data.get(position);
            holder.tvPlanet.setText(PLANETS[position]);
            holder.tvPlanet.setTextColor(PLANET_COLORS[position]);

            String[] cells = new String[5];
            for (int i = 0; i < 5; i++) {
                cells[i] = scores[i] == 0 ? "—" : String.format("%.1f", scores[i]);
            }
            holder.tvSthana.setText(cells[0]);
            holder.tvDig.setText(cells[1]);
            holder.tvKaala.setText(cells[2]);
            holder.tvChesta.setText(cells[3]);
            holder.tvNaisa.setText(cells[4]);

            String total = scores[5] == 0 ? "—" : String.format("%.1f", scores[5]);
            holder.tvTotal.setText(total);
            if (scores[5] > 0) {
                holder.tvTotal.setTextColor(scores[5] >= 6 ? Color.parseColor("#4CAF50")
                    : scores[5] >= 4 ? Color.parseColor("#FF9800")
                    : Color.parseColor("#F44336"));
            }
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvPlanet, tvSthana, tvDig, tvKaala, tvChesta, tvNaisa, tvTotal;
            VH(View v) {
                super(v);
                LinearLayout row = (LinearLayout) v;
                tvPlanet = (TextView) row.getChildAt(0);
                tvSthana = (TextView) row.getChildAt(1);
                tvDig    = (TextView) row.getChildAt(2);
                tvKaala  = (TextView) row.getChildAt(3);
                tvChesta = (TextView) row.getChildAt(4);
                tvNaisa  = (TextView) row.getChildAt(5);
                tvTotal  = (TextView) row.getChildAt(6);
            }
        }

        private View buildRowView(Context ctx) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 8), dp(ctx, 12), dp(ctx, 8), dp(ctx, 12));
            row.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            float[] weights = {1.4f, 1f, 0.8f, 1f, 1f, 1f, 1f};
            TypedValue onSurface = new TypedValue();
            ctx.getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface, onSurface, true);

            for (int i = 0; i < 7; i++) {
                TextView tv = new TextView(ctx);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                tv.setGravity(i == 0 ? Gravity.START : Gravity.CENTER);
                tv.setTextColor(onSurface.data);
                if (i == 0) tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                row.addView(tv, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, weights[i]));
            }
            return row;
        }
    }

    private int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }
}
