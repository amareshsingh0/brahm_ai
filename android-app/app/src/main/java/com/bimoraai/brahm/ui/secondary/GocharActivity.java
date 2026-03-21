package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityGocharBinding;
import com.bimoraai.brahm.utils.DateUtils;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Gochar (Transit) Analysis screen.
 *
 * Auto-loads current planet positions on open via GET /api/gochar.
 * btnAnalyze posts birth data to POST /api/gochar/analyze for personalised analysis.
 */
public class GocharActivity extends AppCompatActivity {

    private ActivityGocharBinding b;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGocharBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        setupAnalyzeButton();
        loadCurrentTransits();
    }

    private void setupToolbar() {
        if (b.btnBack != null) b.btnBack.setOnClickListener(v -> finish());
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupAnalyzeButton() {
        b.btnAnalyze.setOnClickListener(v -> analyzeTransits());
    }

    // ── Auto-load current planet positions ────────────────────────────────────

    private void loadCurrentTransits() {
        ApiClient.getApiService(this)
            .getGochar(prefs.getLat(), prefs.getLon(), prefs.getTz())
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null || !response.isSuccessful()
                            || response.body() == null) return;
                    populatePlanetGrid(response.body());
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    // Non-fatal — planet grid stays empty
                }
            });
    }

    /**
     * Populates the planet position grid with transit data.
     * The layout is expected to have a tvPlanetGrid or similar view.
     * Extend to populate individual TextViews per planet as the layout evolves.
     */
    private void populatePlanetGrid(JsonObject data) {
        // The response contains a "planets" array with name/rashi/degree.
        // For now, if the layout has a summary TextView, write a compact listing.
        if (b.tvTransitSummary == null) return;

        if (data.has("planets")) {
            StringBuilder sb = new StringBuilder();
            for (com.google.gson.JsonElement el : data.getAsJsonArray("planets")) {
                JsonObject p = el.getAsJsonObject();
                String name  = p.has("name")  ? p.get("name").getAsString()  : "";
                String rashi = p.has("rashi") ? p.get("rashi").getAsString() : "";
                sb.append(name).append(": ").append(rashi).append("\n");
            }
            b.tvTransitSummary.setText(sb.toString().trim());
        }
    }

    // ── Personalised transit analysis ────────────────────────────────────────

    private void analyzeTransits() {
        if (prefs.getBirthDate().isEmpty()) {
            Toast.makeText(this, "Birth data not found. Please complete onboarding.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        b.btnAnalyze.setEnabled(false);
        b.btnAnalyze.setText("Analysing…");

        // Build request body
        JsonObject body = new JsonObject();
        body.addProperty("birth_date",  prefs.getBirthDate());
        body.addProperty("birth_time",  prefs.getBirthTime());
        body.addProperty("birth_place", prefs.getBirthPlace());
        body.addProperty("lat",         prefs.getLat());
        body.addProperty("lon",         prefs.getLon());
        body.addProperty("tz",          prefs.getTz());
        body.addProperty("date",        DateUtils.getCurrentIsoDate());

        ApiClient.getApiService(this)
            .analyzeGochar(body)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    b.btnAnalyze.setEnabled(true);
                    b.btnAnalyze.setText("Analyse Transits");

                    if (response.isSuccessful() && response.body() != null) {
                        showResult(response.body());
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Analysis failed. Try again.");
                        Toast.makeText(GocharActivity.this, err, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    b.btnAnalyze.setEnabled(true);
                    b.btnAnalyze.setText("Analyse Transits");
                    Toast.makeText(GocharActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showResult(JsonObject data) {
        if (b.cardResult == null) return;
        b.cardResult.setVisibility(View.VISIBLE);

        if (b.tvTransitSummary != null && data.has("summary"))
            b.tvTransitSummary.setText(data.get("summary").getAsString());

        if (b.tvAshtamaSaturn != null && data.has("ashtama_shani"))
            b.tvAshtamaSaturn.setText(
                data.get("ashtama_shani").getAsBoolean() ? "Active" : "None");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
