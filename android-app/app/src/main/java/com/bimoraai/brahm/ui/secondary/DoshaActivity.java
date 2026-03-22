package com.bimoraai.brahm.ui.secondary;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityDoshaBinding;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dosha Analysis screen — Manglik / Kaal Sarp / Pitra Dosha etc.
 *
 * Auto-loads via GET /api/dosha using birth data from PrefsHelper.
 */
public class DoshaActivity extends AppCompatActivity {

    private ActivityDoshaBinding b;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityDoshaBinding.inflate(getLayoutInflater());
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
            .getDosha(
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
                        populateData(response.body());
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Failed to load dosha data.");
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

    private void populateData(JsonObject data) {
        if (b == null) return;

        // ── Manglik ──────────────────────────────────────────────────────────
        if (data.has("manglik")) {
            JsonObject manglik = data.getAsJsonObject("manglik");
            String status = manglik.has("status")
                ? manglik.get("status").getAsString() : "NO";
            if (b.tvManglikStatus != null) b.tvManglikStatus.setText(statusLabel(status));
            applyDoshaCardColor(status);
        }

        // ── Kaal Sarp ────────────────────────────────────────────────────────
        if (data.has("kaal_sarp")) {
            JsonObject ks = data.getAsJsonObject("kaal_sarp");
            boolean present = ks.has("present") && ks.get("present").getAsBoolean();
            String type = ks.has("type") ? ks.get("type").getAsString() : "";
            if (b.tvKaalSarpStatus != null) {
                b.tvKaalSarpStatus.setText(present
                    ? "Yes — " + type
                    : "No");
            }
        }

        // ── Pitra ────────────────────────────────────────────────────────────
        if (data.has("pitra")) {
            JsonObject pitra = data.getAsJsonObject("pitra");
            boolean present = pitra.has("present") && pitra.get("present").getAsBoolean();
            if (b.tvPitraStatus != null)
                b.tvPitraStatus.setText(present ? "Yes" : "No");
        }

        // ── Remedies ─────────────────────────────────────────────────────────
        if (b.tvRemedies != null) {
            StringBuilder sb = new StringBuilder();
            appendRemedies(sb, data, "manglik");
            appendRemedies(sb, data, "kaal_sarp");
            appendRemedies(sb, data, "pitra");
            b.tvRemedies.setText(sb.length() > 0 ? sb.toString().trim()
                                                  : "No specific remedies required.");
        }
    }

    private void appendRemedies(StringBuilder sb, JsonObject data, String key) {
        if (!data.has(key)) return;
        JsonObject section = data.getAsJsonObject(key);
        if (!section.has("remedies")) return;
        JsonArray arr = section.getAsJsonArray("remedies");
        for (JsonElement r : arr) sb.append("• ").append(r.getAsString()).append("\n");
    }

    private String statusLabel(String status) {
        switch (status.toUpperCase()) {
            case "YES":     return "Yes (Manglik)";
            case "PARTIAL": return "Partial Manglik";
            case "NO":      return "No";
            default:        return status;
        }
    }

    private void applyDoshaCardColor(String status) {
        if (b.cardManglik == null) return;
        int color;
        switch (status.toUpperCase()) {
            case "YES":     color = Color.parseColor("#F44336"); break; // red
            case "PARTIAL": color = Color.parseColor("#FF9800"); break; // amber
            default:        color = Color.parseColor("#4CAF50"); break; // green
        }
        b.cardManglik.setCardBackgroundColor(color);
    }

    private void showError(String msg) {
        if (b.progressBar  != null) b.progressBar.setVisibility(View.GONE);
        if (b.contentLayout!= null) b.contentLayout.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
