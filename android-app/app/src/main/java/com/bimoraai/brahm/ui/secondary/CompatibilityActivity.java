package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityCompatibilityBinding;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Compatibility (Kundali Milan) screen.
 *
 * Tab 0 — Guna Milan (Ashtakoot)
 * Tab 1 — Nakshatra Compatibility
 *
 * Person 1 is pre-filled from PrefsHelper. Person 2 is entered manually.
 */
public class CompatibilityActivity extends AppCompatActivity {

    private ActivityCompatibilityBinding b;
    private PrefsHelper prefs;
    private int selectedTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCompatibilityBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        setupTabListener();
        setupGenerateButton();
        prefillOwnDetails();
    }

    private void setupToolbar() {
        if (b.btnBack != null) b.btnBack.setOnClickListener(v -> finish());
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupTabListener() {
        if (b.tabGunaMilan != null) {
            b.tabGunaMilan.setOnClickListener(v -> {
                selectedTab = 0;
                highlightTab(true);
            });
        }
        if (b.tabNakshatra != null) {
            b.tabNakshatra.setOnClickListener(v -> {
                selectedTab = 1;
                highlightTab(false);
            });
        }
        highlightTab(true);
    }

    private void highlightTab(boolean gunaMilanSelected) {
        if (b.tabGunaMilan == null || b.tabNakshatra == null) return;
        b.tabGunaMilan.setAlpha(gunaMilanSelected ? 1.0f : 0.5f);
        b.tabNakshatra.setAlpha(gunaMilanSelected ? 0.5f : 1.0f);
    }

    private void setupGenerateButton() {
        b.btnGenerate.setOnClickListener(v -> generateCompatibility());
    }

    private void prefillOwnDetails() {
        if (!prefs.getBirthDate().isEmpty()  && b.etDobPerson1 != null)
            b.etDobPerson1.setText(prefs.getBirthDate());
        if (!prefs.getBirthTime().isEmpty()  && b.etTobPerson1 != null)
            b.etTobPerson1.setText(prefs.getBirthTime());
        if (!prefs.getBirthPlace().isEmpty() && b.etPlacePerson1 != null)
            b.etPlacePerson1.setText(prefs.getBirthPlace());
    }

    // ── Compatibility generation ──────────────────────────────────────────────

    private void generateCompatibility() {
        if (b.etDobPerson1 == null || b.etDobPerson2 == null) return;

        String dob1 = b.etDobPerson1.getText().toString().trim();
        String tob1 = b.etTobPerson1 != null ? b.etTobPerson1.getText().toString().trim() : "";
        String dob2 = b.etDobPerson2.getText().toString().trim();
        String tob2 = b.etTobPerson2 != null ? b.etTobPerson2.getText().toString().trim() : "";

        if (dob1.isEmpty() || dob2.isEmpty()) {
            Toast.makeText(this, "Enter birth dates for both persons",
                Toast.LENGTH_SHORT).show();
            return;
        }

        b.btnGenerate.setEnabled(false);
        b.btnGenerate.setText("Calculating…");

        // Build person1 — use saved lat/lon for person 1 (the logged-in user)
        JsonObject p1 = new JsonObject();
        p1.addProperty("date", dob1);
        p1.addProperty("time", tob1.isEmpty() ? "12:00" : tob1);
        p1.addProperty("lat",  prefs.getLat());
        p1.addProperty("lon",  prefs.getLon());
        p1.addProperty("tz",   prefs.getTz());

        // Build person2 — default to same location if not specified
        JsonObject p2 = new JsonObject();
        p2.addProperty("date", dob2);
        p2.addProperty("time", tob2.isEmpty() ? "12:00" : tob2);
        p2.addProperty("lat",  prefs.getLat());
        p2.addProperty("lon",  prefs.getLon());
        p2.addProperty("tz",   prefs.getTz());

        JsonObject requestBody = new JsonObject();
        requestBody.add("person1", p1);
        requestBody.add("person2", p2);
        requestBody.addProperty("type", selectedTab == 0 ? "guna_milan" : "nakshatra");

        ApiClient.getApiService(this)
            .getCompatibility(requestBody)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    b.btnGenerate.setEnabled(true);
                    b.btnGenerate.setText("Generate Compatibility");

                    if (response.isSuccessful() && response.body() != null) {
                        showCompatibilityResult(response.body());
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Compatibility check failed.");
                        Toast.makeText(CompatibilityActivity.this, err,
                            Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    b.btnGenerate.setEnabled(true);
                    b.btnGenerate.setText("Generate Compatibility");
                    Toast.makeText(CompatibilityActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showCompatibilityResult(JsonObject data) {
        if (b.cardResult == null) return;
        b.cardResult.setVisibility(View.VISIBLE);

        if (b.tvScore != null && data.has("total_score"))
            b.tvScore.setText(data.get("total_score").getAsString() + " / 36");

        if (b.tvRecommendation != null && data.has("recommendation"))
            b.tvRecommendation.setText(data.get("recommendation").getAsString());

        // Populate guna milan table if available
        if (b.rvResults != null && data.has("guna_milan")) {
            StringBuilder sb = new StringBuilder();
            JsonArray gunas = data.getAsJsonArray("guna_milan");
            for (JsonElement el : gunas) {
                JsonObject g = el.getAsJsonObject();
                String koot   = g.has("koot")   ? g.get("koot").getAsString()   : "";
                String points = g.has("points") ? g.get("points").getAsString() : "";
                String max    = g.has("max")    ? g.get("max").getAsString()    : "";
                sb.append(koot).append(": ").append(points)
                  .append("/").append(max).append("\n");
            }
            // Set to a summary TextView if rvResults is a TextView
            if (b.rvResults instanceof android.widget.TextView)
                ((android.widget.TextView) b.rvResults).setText(sb.toString().trim());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
