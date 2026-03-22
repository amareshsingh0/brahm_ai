package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityKpBinding;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * KP (Krishnamurti Paddhati) System screen.
 *
 * Shows sub-lord analysis via POST /api/kp.
 * Tab 0 — Planets; Tab 1 — Cusps.
 */
public class KPActivity extends AppCompatActivity {

    private ActivityKpBinding b;
    private PrefsHelper prefs;
    private int selectedTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityKpBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        prefillBirthData();
        setupTabToggle();
        setupGenerateButton();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void prefillBirthData() {
        if (b.etDate  != null && !prefs.getBirthDate().isEmpty())
            b.etDate.setText(prefs.getBirthDate());
        if (b.etTime  != null && !prefs.getBirthTime().isEmpty())
            b.etTime.setText(prefs.getBirthTime());
        if (b.etPlace != null && !prefs.getBirthPlace().isEmpty())
            b.etPlace.setText(prefs.getBirthPlace());
    }

    private void setupTabToggle() {
        if (b.btnTabPlanets != null) b.btnTabPlanets.setOnClickListener(v -> {
            selectedTab = 0; showTab(true);
        });
        if (b.btnTabCusps != null) b.btnTabCusps.setOnClickListener(v -> {
            selectedTab = 1; showTab(false);
        });
        showTab(true);
    }

    private void showTab(boolean planets) {
        if (b.layoutPlanets != null)
            b.layoutPlanets.setVisibility(planets ? View.VISIBLE : View.GONE);
        if (b.layoutCusps != null)
            b.layoutCusps.setVisibility(planets ? View.GONE : View.VISIBLE);
        if (b.btnTabPlanets != null) b.btnTabPlanets.setAlpha(planets ? 1f : 0.5f);
        if (b.btnTabCusps   != null) b.btnTabCusps.setAlpha(planets ? 0.5f : 1f);
    }

    private void setupGenerateButton() {
        b.btnGenerate.setOnClickListener(v -> generateKP());
    }

    // ── KP generation ─────────────────────────────────────────────────────────

    private void generateKP() {
        String date  = b.etDate  != null ? b.etDate.getText().toString().trim()  : "";
        String time  = b.etTime  != null ? b.etTime.getText().toString().trim()  : "";
        String place = b.etPlace != null ? b.etPlace.getText().toString().trim() : "";

        if (date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill in birth date and time",
                Toast.LENGTH_SHORT).show();
            return;
        }

        b.btnGenerate.setEnabled(false);
        b.btnGenerate.setText("Generating…");

        JsonObject body = new JsonObject();
        body.addProperty("date",  date);
        body.addProperty("time",  time);
        body.addProperty("lat",   prefs.getLat());
        body.addProperty("lon",   prefs.getLon());
        body.addProperty("tz",    prefs.getTz());
        body.addProperty("name",  "KP Chart");

        ApiClient.getApiService(this)
            .getKP(body)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    b.btnGenerate.setEnabled(true);
                    b.btnGenerate.setText("Generate KP Chart");

                    if (response.isSuccessful() && response.body() != null) {
                        populateKPData(response.body());
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "KP generation failed.");
                        Toast.makeText(KPActivity.this, err, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    b.btnGenerate.setEnabled(true);
                    b.btnGenerate.setText("Generate KP Chart");
                    Toast.makeText(KPActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void populateKPData(JsonObject data) {
        if (b.cardResult != null) b.cardResult.setVisibility(View.VISIBLE);

        // Build a simple text table for planets
        if (b.tvKPPlanets != null && data.has("planets")) {
            JsonArray planets = data.getAsJsonArray("planets");
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-10s %-10s %-10s %-10s%n",
                "Planet", "Star Lord", "Sub Lord", "Sub-Sub"));
            for (JsonElement el : planets) {
                JsonObject p = el.getAsJsonObject();
                sb.append(String.format("%-10s %-10s %-10s %-10s%n",
                    field(p, "name"),
                    field(p, "star_lord"),
                    field(p, "sub_lord"),
                    field(p, "sub_sub_lord")));
            }
            b.tvKPPlanets.setText(sb.toString());
        }

        // Build a simple text table for cusps
        if (b.tvKPCusps != null && data.has("cusps")) {
            JsonArray cusps = data.getAsJsonArray("cusps");
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-6s %-12s %-10s %-10s%n",
                "House", "Sign", "Star Lord", "Sub Lord"));
            for (JsonElement el : cusps) {
                JsonObject c = el.getAsJsonObject();
                sb.append(String.format("%-6s %-12s %-10s %-10s%n",
                    field(c, "house"),
                    field(c, "sign"),
                    field(c, "star_lord"),
                    field(c, "sub_lord")));
            }
            b.tvKPCusps.setText(sb.toString());
        }
    }

    private static String field(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : "—";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
