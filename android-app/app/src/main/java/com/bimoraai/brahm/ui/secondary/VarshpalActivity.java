package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityVarshpalBinding;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Calendar;

/**
 * Varshphal (Solar Return / Annual Horoscope) screen.
 *
 * Generates the Solar Return chart via POST /api/varshphal.
 */
public class VarshpalActivity extends AppCompatActivity {

    private ActivityVarshpalBinding b;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityVarshpalBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        prefillDefaults();
        setupGenerateButton();
    }

    private void setupToolbar() {
        if (b.btnBack != null) b.btnBack.setOnClickListener(v -> finish());
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void prefillDefaults() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        if (b.etYear  != null) b.etYear.setText(String.valueOf(currentYear));
        if (b.etPlace != null && !prefs.getBirthPlace().isEmpty())
            b.etPlace.setText(prefs.getBirthPlace());
    }

    private void setupGenerateButton() {
        b.btnGenerate.setOnClickListener(v -> generateVarshphal());
    }

    // ── Varshphal generation ──────────────────────────────────────────────────

    private void generateVarshphal() {
        String year = b.etYear != null ? b.etYear.getText().toString().trim() : "";
        if (year.isEmpty()) {
            Toast.makeText(this, "Please enter a year", Toast.LENGTH_SHORT).show();
            return;
        }

        if (prefs.getBirthDate().isEmpty()) {
            Toast.makeText(this, "Birth data not found. Please complete onboarding.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        b.btnGenerate.setEnabled(false);
        b.btnGenerate.setText("Generating…");

        JsonObject body = new JsonObject();
        body.addProperty("date",  prefs.getBirthDate());
        body.addProperty("time",  prefs.getBirthTime());
        body.addProperty("lat",   prefs.getLat());
        body.addProperty("lon",   prefs.getLon());
        body.addProperty("tz",    prefs.getTz());
        body.addProperty("year",  Integer.parseInt(year));

        ApiClient.getApiService(this)
            .getVarshphal(body)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    b.btnGenerate.setEnabled(true);
                    b.btnGenerate.setText("Generate Varshphal");

                    if (response.isSuccessful() && response.body() != null) {
                        showResult(response.body(), year);
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Varshphal generation failed.");
                        Toast.makeText(VarshpalActivity.this, err, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    b.btnGenerate.setEnabled(true);
                    b.btnGenerate.setText("Generate Varshphal");
                    Toast.makeText(VarshpalActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showResult(JsonObject data, String year) {
        if (b.cardResult != null) b.cardResult.setVisibility(View.VISIBLE);

        if (b.tvResultYear != null)
            b.tvResultYear.setText("Varshphal " + year);

        if (b.tvYearLord != null && data.has("year_lord"))
            b.tvYearLord.setText(data.get("year_lord").getAsString());

        if (b.tvMuntha != null && data.has("muntha"))
            b.tvMuntha.setText(data.get("muntha").getAsString());

        if (b.tvLagna != null && data.has("lagna"))
            b.tvLagna.setText(data.get("lagna").getAsString());

        if (b.tvSummary != null && data.has("summary"))
            b.tvSummary.setText(data.get("summary").getAsString());

        // Planet table
        if (b.rvPlanets != null && data.has("planets")) {
            JsonArray planets = data.getAsJsonArray("planets");
            StringBuilder sb = new StringBuilder();
            for (JsonElement el : planets) {
                JsonObject p = el.getAsJsonObject();
                String name  = p.has("name")  ? p.get("name").getAsString()  : "";
                String rashi = p.has("rashi") ? p.get("rashi").getAsString() : "";
                String house = p.has("house") ? p.get("house").getAsString() : "";
                sb.append(name).append(": ").append(rashi)
                  .append(" (H").append(house).append(")\n");
            }
            if (b.rvPlanets instanceof android.widget.TextView)
                ((android.widget.TextView) b.rvPlanets).setText(sb.toString().trim());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
