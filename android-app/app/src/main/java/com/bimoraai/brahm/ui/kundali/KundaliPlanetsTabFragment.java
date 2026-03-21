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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bimoraai.brahm.model.GrahaData;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab 1 — Planets
 *
 * Displays a RecyclerView with one row per planet (9 Grahas: Sun → Ketu).
 * Each row shows: coloured planet name | rashi | house | degree | retro badge.
 *
 * TODO: Observe the shared ViewModel for the kundali response and call
 *       adapter.updateData(kundaliResponse.getPlanets()) to populate real data.
 */
public class KundaliPlanetsTabFragment extends Fragment {

    // Placeholder dataset used until API is integrated
    private static final String[] PLANET_NAMES = {
        "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu"
    };

    // Traditional planet colours (accent tones on dark background)
    private static final int[] PLANET_COLORS = {
        0xFFFF6B35,  // Sun    — orange
        0xFFD4D4FF,  // Moon   — silver-lavender
        0xFFFF4444,  // Mars   — red
        0xFF4CAF50,  // Mercury— green
        0xFFFFD700,  // Jupiter— gold
        0xFFFF80AB,  // Venus  — pink
        0xFF9E9E9E,  // Saturn — grey
        0xFF7B1FA2,  // Rahu   — deep purple
        0xFF795548,  // Ketu   — brown
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();

        RecyclerView rv = new RecyclerView(ctx);
        rv.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setLayoutManager(new LinearLayoutManager(ctx));
        rv.addItemDecoration(new DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL));

        // Start with placeholders, then try to load from cached JSON
        List<GrahaData> placeholders = buildPlaceholders();
        PlanetAdapter adapter = new PlanetAdapter(placeholders);
        rv.setAdapter(adapter);

        // Load real planet data from PrefsHelper cache
        String json = new PrefsHelper(ctx).getKundaliJson();
        if (!json.isEmpty()) {
            try {
                JsonObject kundali = JsonParser.parseString(json).getAsJsonObject();
                if (kundali.has("grahas")) {
                    List<GrahaData> planets = parseGrahas(kundali.getAsJsonArray("grahas"));
                    if (!planets.isEmpty()) adapter.updateData(planets);
                }
            } catch (Exception ignored) { }
        }

        return rv;
    }

    // ── Data builders ─────────────────────────────────────────────────────────

    private static List<GrahaData> parseGrahas(JsonArray arr) {
        List<GrahaData> list = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject g = el.getAsJsonObject();
            String  rashi     = g.has("rashi")     ? g.get("rashi").getAsString()     : "—";
            int     house     = g.has("house")     ? g.get("house").getAsInt()        : 0;
            double  degree    = g.has("degree")    ? g.get("degree").getAsDouble()    : 0.0;
            boolean retro     = g.has("retro")     && g.get("retro").getAsBoolean();
            String  nakshatra = g.has("nakshatra") ? g.get("nakshatra").getAsString() : "";
            String  status    = g.has("status")    ? g.get("status").getAsString()    : "";
            // GrahaData(rashi, house, degree, retro, nakshatra, status)
            list.add(new GrahaData(rashi, house, degree, retro, nakshatra, status));
        }
        return list;
    }

    private static List<GrahaData> buildPlaceholders() {
        List<GrahaData> list = new ArrayList<>();
        // Stub entries — all zeroed; will be replaced by real API data
        for (String name : PLANET_NAMES) {
            list.add(new GrahaData("—", 0, 0.0, false, "—", ""));
        }
        return list;
    }

    // ── Inline adapter ────────────────────────────────────────────────────────

    private class PlanetAdapter extends RecyclerView.Adapter<PlanetAdapter.VH> {

        private final List<GrahaData> data;

        PlanetAdapter(List<GrahaData> data) { this.data = data; }

        public void updateData(List<GrahaData> newData) {
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
            GrahaData g = data.get(position);

            holder.tvPlanet.setText(PLANET_NAMES[position]);
            holder.tvPlanet.setTextColor(PLANET_COLORS[position]);

            // Show "—" when stub data is loaded
            holder.tvRashi.setText(g.getRashi().isEmpty() ? "—" : g.getRashi());
            holder.tvHouse.setText(g.getHouse() == 0 ? "—" : String.valueOf(g.getHouse()));
            holder.tvDegree.setText(g.getHouse() == 0 ? "—" : g.getDegreeFormatted());

            if (g.isRetro()) {
                holder.tvRetro.setVisibility(View.VISIBLE);
            } else {
                holder.tvRetro.setVisibility(View.GONE);
            }

            // Status badge (Exalted / Debilitated / Own Sign)
            if (!g.getStatus().isEmpty()) {
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvStatus.setText(g.getStatus());
                holder.tvStatus.setTextColor(statusColor(g.getStatus()));
            } else {
                holder.tvStatus.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return data.size(); }

        private int statusColor(String status) {
            switch (status.toLowerCase()) {
                case "exalted":     return Color.parseColor("#4CAF50");
                case "debilitated": return Color.parseColor("#F44336");
                case "own sign":    return Color.parseColor("#2196F3");
                default:            return Color.GRAY;
            }
        }

        // ── Row view builder ────────────────────────────────────────────────

        private View buildRowView(Context ctx) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 16), dp(ctx, 12), dp(ctx, 16), dp(ctx, 12));
            row.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

            // Planet name — weight 1.5
            TextView tvPlanet = makeCell(ctx, "", 14, 0, true);
            row.addView(tvPlanet, weightParam(ctx, 1.5f));

            // Rashi — weight 1.5
            TextView tvRashi = makeCell(ctx, "", 13, 0, false);
            row.addView(tvRashi, weightParam(ctx, 1.5f));

            // House — weight 0.6
            TextView tvHouse = makeCell(ctx, "", 13, Gravity.CENTER, false);
            row.addView(tvHouse, weightParam(ctx, 0.6f));

            // Degree — weight 1
            TextView tvDegree = makeCell(ctx, "", 12, Gravity.CENTER, false);
            row.addView(tvDegree, weightParam(ctx, 1f));

            // Retro badge
            TextView tvRetro = new TextView(ctx);
            tvRetro.setText("(R)");
            tvRetro.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvRetro.setTextColor(Color.parseColor("#FF9800"));
            tvRetro.setPadding(dp(ctx, 4), 0, dp(ctx, 4), 0);
            tvRetro.setVisibility(View.GONE);
            row.addView(tvRetro);

            // Status badge
            TextView tvStatus = new TextView(ctx);
            tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvStatus.setPadding(dp(ctx, 4), dp(ctx, 2), dp(ctx, 4), dp(ctx, 2));
            tvStatus.setVisibility(View.GONE);
            row.addView(tvStatus);

            return row;
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvPlanet, tvRashi, tvHouse, tvDegree, tvRetro, tvStatus;
            VH(View v) {
                super(v);
                LinearLayout row = (LinearLayout) v;
                tvPlanet = (TextView) row.getChildAt(0);
                tvRashi  = (TextView) row.getChildAt(1);
                tvHouse  = (TextView) row.getChildAt(2);
                tvDegree = (TextView) row.getChildAt(3);
                tvRetro  = (TextView) row.getChildAt(4);
                tvStatus = (TextView) row.getChildAt(5);
            }
        }

        private TextView makeCell(Context ctx, String text, int spSize,
                                  int gravity, boolean bold) {
            TextView tv = new TextView(ctx);
            tv.setText(text);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, spSize);
            if (bold) tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            if (gravity != 0) tv.setGravity(gravity);
            TypedValue val = new TypedValue();
            ctx.getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface, val, true);
            tv.setTextColor(val.data);
            return tv;
        }

        private LinearLayout.LayoutParams weightParam(Context ctx, float weight) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            return lp;
        }

        private int dp(Context ctx, int dp) {
            return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                ctx.getResources().getDisplayMetrics());
        }
    }
}
