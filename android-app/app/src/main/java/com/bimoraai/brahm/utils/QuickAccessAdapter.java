package com.bimoraai.brahm.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bimoraai.brahm.R;
import java.util.List;

/**
 * Grid adapter for the quick-access tiles on the Home screen.
 *
 * Each tile is built programmatically (no separate XML needed):
 *   vertical LinearLayout
 *     └── ImageView  (40 dp, tinted with colorPrimary)
 *     └── TextView   (label, 10 sp, centered)
 *
 * Designed to sit inside a GridLayoutManager with 4 columns.
 */
public class QuickAccessAdapter extends RecyclerView.Adapter<QuickAccessAdapter.VH> {

    // ── Listener interface ──────────────────────────────────────────────────
    public interface OnItemClickListener {
        void onItemClick(QuickAccessItem item);
    }

    // ── Fields ──────────────────────────────────────────────────────────────
    private final List<QuickAccessItem> items;
    private final OnItemClickListener   listener;

    public QuickAccessAdapter(List<QuickAccessItem> items, OnItemClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    // ── Adapter overrides ───────────────────────────────────────────────────

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(buildItemView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        QuickAccessItem item = items.get(position);

        // Icon
        holder.icon.setImageResource(item.getIconRes());

        // Tint icon with colorPrimary
        TypedValue tv = new TypedValue();
        holder.icon.getContext().getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, tv, true);
        holder.icon.setColorFilter(tv.data);

        // Label
        holder.label.setText(item.getLabel());

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ──────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView  label;

        VH(View itemView) {
            super(itemView);
            LinearLayout root = (LinearLayout) itemView;
            icon  = (ImageView) root.getChildAt(0);
            label = (TextView)  root.getChildAt(1);
        }
    }

    // ── View builder ────────────────────────────────────────────────────────

    private static View buildItemView(Context ctx) {
        // Root — vertical LinearLayout
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(ctx, 8), dp(ctx, 12), dp(ctx, 8), dp(ctx, 12));

        // Ripple / clickable
        TypedValue outValue = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,
            outValue, true);
        root.setBackgroundResource(outValue.resourceId);
        root.setClickable(true);
        root.setFocusable(true);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        root.setLayoutParams(lp);

        // Icon
        ImageView icon = new ImageView(ctx);
        int size = dp(ctx, 40);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(size, size);
        iconLp.bottomMargin = dp(ctx, 6);
        icon.setLayoutParams(iconLp);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        root.addView(icon);

        // Label
        TextView tv = new TextView(ctx);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setGravity(Gravity.CENTER);
        tv.setMaxLines(2);

        // Use colorOnSurface if available, else white
        TypedValue colorTv = new TypedValue();
        if (ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface, colorTv, true)) {
            tv.setTextColor(colorTv.data);
        } else {
            tv.setTextColor(Color.WHITE);
        }
        root.addView(tv);

        return root;
    }

    private static int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            ctx.getResources().getDisplayMetrics());
    }
}
