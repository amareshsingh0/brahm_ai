package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivitySadeSatiBinding;
import com.bimoraai.brahm.utils.DateUtils;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Sade Sati Calculator screen.
 *
 * Auto-loads data on open using birth data from PrefsHelper via
 * GET /api/sade-sati.
 */
public class SadeSatiActivity extends AppCompatActivity {

    private ActivitySadeSatiBinding b;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySadeSatiBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        loadData();
    }

    private void setupToolbar() {
        if (b.btnBack != null) b.btnBack.setOnClickListener(v -> finish());
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
            .getSadeSati(
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
                            response.errorBody(), "Failed to load Sade Sati data.");
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

        if (b.tvMoonSign != null && data.has("moon_sign"))
            b.tvMoonSign.setText(data.get("moon_sign").getAsString());

        if (b.tvPhase != null && data.has("phase"))
            b.tvPhase.setText(formatPhase(data.get("phase").getAsString()));

        if (b.tvPeriod != null) {
            String start = data.has("phase_start") ? data.get("phase_start").getAsString() : "";
            String end   = data.has("phase_end")   ? data.get("phase_end").getAsString()   : "";
            if (!start.isEmpty() && !end.isEmpty()) {
                b.tvPeriod.setText(start + " – " + end);
            } else {
                b.tvPeriod.setText("Not active");
            }
        }

        if (b.tvRemedies != null) {
            if (data.has("remedies")) {
                JsonArray remedies = data.getAsJsonArray("remedies");
                StringBuilder sb = new StringBuilder();
                for (JsonElement r : remedies) sb.append("• ").append(r.getAsString()).append("\n");
                b.tvRemedies.setText(sb.toString().trim());
            } else {
                b.tvRemedies.setText("No remedies required.");
            }
        }

        // Effects / description
        if (b.tvEffects != null && data.has("effects"))
            b.tvEffects.setText(data.get("effects").getAsString());
    }

    private static String formatPhase(String phase) {
        if (phase == null) return "Not active";
        switch (phase.toUpperCase()) {
            case "RISING":  return "Rising Phase";
            case "PEAK":    return "Peak Phase";
            case "SETTING": return "Setting Phase";
            case "NONE":    return "Not active";
            default:        return phase;
        }
    }

    private void showError(String msg) {
        if (b.progressBar  != null) b.progressBar.setVisibility(View.GONE);
        if (b.contentLayout!= null) b.contentLayout.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        if (b.tvPhase != null) b.tvPhase.setText("—");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
