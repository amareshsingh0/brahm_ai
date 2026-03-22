package com.bimoraai.brahm.ui.secondary;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityGemstoneBinding;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Gemstone Recommendations screen.
 *
 * Loads recommendations via GET /api/gemstones and builds a card per gem.
 */
public class GemstoneActivity extends AppCompatActivity {

    private ActivityGemstoneBinding b;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGemstoneBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        loadData();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadData() {
        if (b.progressBar  != null) b.progressBar.setVisibility(View.VISIBLE);
        if (b.contentLayout!= null) b.contentLayout.setVisibility(View.GONE);

        if (prefs.getBirthDate().isEmpty()) {
            showError("Birth data not found. Please complete onboarding.");
            return;
        }

        ApiClient.getApiService(this)
            .getGemstones(
                prefs.getLat(),
                prefs.getLon(),
                prefs.getTz(),
                prefs.getBirthDate(),
                prefs.getBirthTime()
            )
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    if (b.progressBar  != null) b.progressBar.setVisibility(View.GONE);
                    if (b.contentLayout!= null) b.contentLayout.setVisibility(View.VISIBLE);

                    if (response.isSuccessful() && response.body() != null) {
                        populateGemstones(response.body());
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Failed to load gemstone data.");
                        showError(err);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    if (b.progressBar  != null) b.progressBar.setVisibility(View.GONE);
                    if (b.contentLayout!= null) b.contentLayout.setVisibility(View.VISIBLE);
                    showError("Network error: " + t.getMessage());
                }
            });
    }

    private void populateGemstones(JsonObject data) {
        if (b == null) return;
        if (b.tvPlaceholder != null) b.tvPlaceholder.setVisibility(View.GONE);

        if (!data.has("gemstones")) {
            showError("No gemstone recommendations available.");
            return;
        }

        JsonArray gemstones = data.getAsJsonArray("gemstones");
        if (b.gemstoneContainer == null) {
            // Fallback: show in placeholder text
            StringBuilder sb = new StringBuilder();
            for (JsonElement el : gemstones) {
                JsonObject g = el.getAsJsonObject();
                sb.append(text(g, "gem")).append(" — ")
                  .append(text(g, "planet")).append("\n")
                  .append("Weight: ").append(text(g, "weight")).append(" ratti  |  ")
                  .append("Metal: ").append(text(g, "metal")).append("\n")
                  .append("Finger: ").append(text(g, "finger")).append("\n\n");
            }
            if (b.tvPlaceholder != null) {
                b.tvPlaceholder.setVisibility(View.VISIBLE);
                b.tvPlaceholder.setText(sb.toString().trim());
            }
            return;
        }

        b.gemstoneContainer.removeAllViews();
        for (JsonElement el : gemstones) {
            JsonObject g = el.getAsJsonObject();
            b.gemstoneContainer.addView(buildGemCard(g));
        }
    }

    /** Builds a card view for a single gemstone recommendation. */
    private View buildGemCard(JsonObject gem) {
        Context ctx = this;
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        TypedValue sv = new TypedValue();
        getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant, sv, true);
        bg.setColor(sv.data);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);

        card.addView(boldLabel(ctx, text(gem, "gem")
            + "  (" + text(gem, "planet") + ")", 16));
        card.addView(infoLine(ctx, "Weight",  text(gem, "weight") + " ratti"));
        card.addView(infoLine(ctx, "Metal",   text(gem, "metal")));
        card.addView(infoLine(ctx, "Finger",  text(gem, "finger")));
        card.addView(infoLine(ctx, "Quality", text(gem, "quality")));

        if (gem.has("benefits") && !gem.get("benefits").getAsString().isEmpty()) {
            TextView tv = new TextView(ctx);
            tv.setText(gem.get("benefits").getAsString());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tv.setPadding(0, dp(6), 0, 0);
            TypedValue ov = new TypedValue();
            getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceVariant, ov, true);
            tv.setTextColor(ov.data);
            card.addView(tv);
        }

        return card;
    }

    private TextView boldLabel(Context ctx, String text, int sp) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TypedValue cv = new TypedValue();
        getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurface, cv, true);
        tv.setTextColor(cv.data);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView infoLine(Context ctx, String label, String value) {
        TextView tv = new TextView(ctx);
        tv.setText(label + ": " + value);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        TypedValue ov = new TypedValue();
        getTheme().resolveAttribute(
            com.google.android.material.R.attr.colorOnSurfaceVariant, ov, true);
        tv.setTextColor(ov.data);
        return tv;
    }

    private TextView infoLine(Context ctx, String label, String value, int unused) {
        return infoLine(ctx, label, value);
    }

    private static String text(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : "—";
    }

    private void showError(String msg) {
        if (b.progressBar  != null) b.progressBar.setVisibility(View.GONE);
        if (b.contentLayout!= null) b.contentLayout.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        if (b.tvPlaceholder != null) {
            b.tvPlaceholder.setVisibility(View.VISIBLE);
            b.tvPlaceholder.setText(msg);
        }
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
            getResources().getDisplayMetrics());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
