package com.bimoraai.brahm.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bimoraai.brahm.model.ChatMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the AI chat RecyclerView in ChatFragment.
 *
 * Two ViewTypes:
 *   VIEW_TYPE_USER — right-aligned bubble, colorPrimary background.
 *   VIEW_TYPE_AI   — left-aligned bubble, colorSurfaceVariant background,
 *                    with an optional confidence chip below the message.
 *
 * Views are built programmatically.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI   = 1;

    private final List<ChatMessage> messages = new ArrayList<>();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Appends a message and scrolls the list to the bottom (via notifyItemInserted).
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /** Removes all messages from the list. */
    public void clearMessages() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Returns an unmodifiable view of the current message list.
     * Used by ChatFragment to build the history payload for the AI.
     */
    public java.util.List<ChatMessage> getMessages() {
        return java.util.Collections.unmodifiableList(messages);
    }

    /**
     * Replaces the content of the last message (the live AI streaming message)
     * without changing its position. Called on each SSE chunk.
     *
     * @param content Accumulated text so far.
     */
    public void updateLastMessage(String content) {
        if (messages.isEmpty()) return;
        int last = messages.size() - 1;
        ChatMessage old = messages.get(last);
        messages.set(last, new ChatMessage(content, old.isUser(), old.getConfidence()));
        notifyItemChanged(last);
    }

    /**
     * Replaces the last message object entirely — used when the stream finishes
     * and we want to set the final text + confidence level.
     *
     * @param message Completed AI message with confidence.
     */
    public void replaceLastMessage(ChatMessage message) {
        if (messages.isEmpty()) return;
        int last = messages.size() - 1;
        messages.set(last, message);
        notifyItemChanged(last);
    }

    // ── Adapter overrides ─────────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(buildBubbleView(parent.getContext(), viewType == VIEW_TYPE_USER));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvContent.setText(msg.getContent());

        // Confidence badge — only shown for AI messages that carry a rating
        if (holder.tvConfidence != null) {
            if (msg.hasConfidence()) {
                holder.tvConfidence.setVisibility(View.VISIBLE);
                holder.tvConfidence.setText(msg.getConfidence());
                applyConfidenceColor(holder.tvConfidence, msg.getConfidence());
            } else {
                holder.tvConfidence.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvContent;
        final TextView tvConfidence; // null for user messages

        VH(View itemView) {
            super(itemView);
            LinearLayout outer = (LinearLayout) itemView;
            LinearLayout bubble = (LinearLayout) outer.getChildAt(0);
            tvContent    = (TextView) bubble.getChildAt(0);
            // AI bubbles have a second child (confidence chip)
            tvConfidence = bubble.getChildCount() > 1
                           ? (TextView) bubble.getChildAt(1)
                           : null;
        }
    }

    // ── View builders ─────────────────────────────────────────────────────────

    /**
     * Builds the outer row + inner bubble. User bubbles are right-aligned,
     * AI bubbles are left-aligned and include a confidence chip placeholder.
     */
    private static View buildBubbleView(Context ctx, boolean isUser) {
        int r18 = dp(ctx, 18);
        int r4  = dp(ctx, 4);

        // Outer row
        LinearLayout outer = new LinearLayout(ctx);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setGravity(isUser ? Gravity.END : Gravity.START);
        outer.setPadding(dp(ctx, 12), dp(ctx, 4), dp(ctx, 12), dp(ctx, 4));
        outer.setLayoutParams(new RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        // Inner bubble
        LinearLayout bubble = new LinearLayout(ctx);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10));

        LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        bubble.setLayoutParams(bubbleLp);

        // Background with directional corner radii
        GradientDrawable bg = new GradientDrawable();
        if (isUser) {
            // Gradient purple: top-left,top-right,bottom-right,bottom-left
            bg.setColors(new int[]{Color.parseColor("#8B5CF6"), Color.parseColor("#7C3AED")});
            bg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setCornerRadii(new float[]{r18,r18, r4,r4, r18,r18, r18,r18});
        } else {
            bg.setColor(Color.parseColor("#1E1E28"));
            bg.setStroke(dp(ctx, 1), Color.parseColor("#2D2D3E"));
            bg.setCornerRadii(new float[]{r4,r4, r18,r18, r18,r18, r18,r18});
        }
        bubble.setBackground(bg);

        // Content text
        TextView tvContent = new TextView(ctx);
        tvContent.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvContent.setLineSpacing(0, 1.5f);
        tvContent.setTextColor(isUser ? Color.WHITE : Color.parseColor("#F4F4F5"));
        bubble.addView(tvContent);

        // Confidence badge — only for AI
        if (!isUser) {
            TextView tvConf = new TextView(ctx);
            LinearLayout.LayoutParams confLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            confLp.topMargin = dp(ctx, 6);
            tvConf.setLayoutParams(confLp);
            tvConf.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvConf.setPadding(dp(ctx, 8), dp(ctx, 3), dp(ctx, 8), dp(ctx, 3));
            tvConf.setTextColor(Color.WHITE);
            tvConf.setVisibility(View.GONE);

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(dp(ctx, 10));
            chipBg.setColor(Color.GRAY);
            tvConf.setBackground(chipBg);

            bubble.addView(tvConf);
        }

        outer.addView(bubble);
        return outer;
    }

    /**
     * Sets the confidence chip background colour and text.
     *   HIGH   → green  (#4CAF50)
     *   MEDIUM → amber  (#FF9800)
     *   LOW    → red    (#F44336)
     */
    private static void applyConfidenceColor(TextView tvConf, String confidence) {
        int color;
        switch (confidence.toUpperCase()) {
            case "HIGH":   color = Color.parseColor("#4CAF50"); break;
            case "MEDIUM": color = Color.parseColor("#FF9800"); break;
            case "LOW":    color = Color.parseColor("#F44336"); break;
            default:       color = Color.GRAY;                  break;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(tvConf.getContext() != null
            ? dp(tvConf.getContext(), 8) : 16);
        bg.setColor(color);
        tvConf.setBackground(bg);
        tvConf.setText(confidence.charAt(0)
            + confidence.substring(1).toLowerCase() + " confidence");
    }

    // ── Colour helpers ────────────────────────────────────────────────────────

    private static int resolvePrimaryColor(Context ctx) {
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorPrimary, tv, true);
        return tv.data;
    }

    private static int resolveSurfaceVariantColor(Context ctx) {
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
        return tv.data;
    }

    private static int resolveOnSurfaceColor(Context ctx) {
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface, tv, true);
        return tv.data;
    }

    private static int dp(Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            ctx.getResources().getDisplayMetrics());
    }
}
