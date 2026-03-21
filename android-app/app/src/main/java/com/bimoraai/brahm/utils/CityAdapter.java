package com.bimoraai.brahm.utils;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bimoraai.brahm.model.City;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the city-search dropdown / RecyclerView used during onboarding
 * and in secondary activities that require a place input.
 *
 * Each row shows:
 *   Line 1 — city name (bold, primary text colour)
 *   Line 2 — state, country (smaller, secondary text colour)
 *
 * Views are built programmatically — no separate XML needed.
 */
public class CityAdapter extends RecyclerView.Adapter<CityAdapter.VH> {

    // ── Listener ─────────────────────────────────────────────────────────────
    public interface OnCityClickListener {
        void onCityClick(City city);
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private final List<City>          cities   = new ArrayList<>();
    private final OnCityClickListener listener;

    public CityAdapter(List<City> initialList, OnCityClickListener listener) {
        this.listener = listener;
        if (initialList != null) cities.addAll(initialList);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Replace the entire dataset (called after each search API response).
     */
    public void updateList(List<City> newList) {
        cities.clear();
        if (newList != null) cities.addAll(newList);
        notifyDataSetChanged();
    }

    // ── Adapter overrides ─────────────────────────────────────────────────────

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(buildItemView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        City city = cities.get(position);

        holder.tvName.setText(city.getName());
        holder.tvSub.setText(city.getState() + ", " + city.getCountry());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCityClick(city);
        });
    }

    @Override
    public int getItemCount() { return cities.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvSub;

        VH(View itemView) {
            super(itemView);
            LinearLayout root = (LinearLayout) itemView;
            tvName = (TextView) root.getChildAt(0);
            tvSub  = (TextView) root.getChildAt(1);
        }
    }

    // ── View builder ──────────────────────────────────────────────────────────

    private static View buildItemView(Context ctx) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(ctx, 16);
        int padV = dp(ctx, 12);
        root.setPadding(padH, padV, padH, padV);

        // Ripple background
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        root.setBackgroundResource(tv.resourceId);
        root.setClickable(true);
        root.setFocusable(true);

        root.setLayoutParams(new RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        // City name — bold, 15sp
        TextView name = new TextView(ctx);
        name.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        resolveTextColor(ctx, name, com.google.android.material.R.attr.colorOnSurface);
        root.addView(name);

        // State, country — 12sp, secondary colour
        TextView sub = new TextView(ctx);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(ctx, 2);
        sub.setLayoutParams(subLp);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        resolveTextColor(ctx, sub, com.google.android.material.R.attr.colorOnSurfaceVariant);
        root.addView(sub);

        return root;
    }

    private static void resolveTextColor(Context ctx, TextView tv, int attrId) {
        TypedValue val = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrId, val, true)) {
            tv.setTextColor(val.data);
        }
    }

    private static int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            ctx.getResources().getDisplayMetrics());
    }
}
