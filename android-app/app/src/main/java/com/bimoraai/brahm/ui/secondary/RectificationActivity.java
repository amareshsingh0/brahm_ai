package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityRectificationBinding;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Birth Time Rectification screen.
 *
 * Submits birth date + life events to POST /api/rectification and displays
 * the rectified birth time with a confidence rating.
 */
public class RectificationActivity extends AppCompatActivity {

    private ActivityRectificationBinding b;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRectificationBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        prefillBirthData();
        setupRectifyButton();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void prefillBirthData() {
        if (b.etBirthDate  != null && !prefs.getBirthDate().isEmpty())
            b.etBirthDate.setText(prefs.getBirthDate());
        if (b.etBirthPlace != null && !prefs.getBirthPlace().isEmpty())
            b.etBirthPlace.setText(prefs.getBirthPlace());
        if (b.etApproxTime != null && !prefs.getBirthTime().isEmpty())
            b.etApproxTime.setText(prefs.getBirthTime());
    }

    private void setupRectifyButton() {
        b.btnRectify.setOnClickListener(v -> rectify());
    }

    // ── Rectification ─────────────────────────────────────────────────────────

    private void rectify() {
        String date  = b.etBirthDate  != null ? b.etBirthDate.getText().toString().trim()  : "";
        String place = b.etBirthPlace != null ? b.etBirthPlace.getText().toString().trim() : "";

        if (date.isEmpty() || place.isEmpty()) {
            Toast.makeText(this, "Birth date and place are required",
                Toast.LENGTH_SHORT).show();
            return;
        }

        b.btnRectify.setEnabled(false);
        b.btnRectify.setText("Rectifying…");
        if (b.cardResult != null) b.cardResult.setVisibility(View.GONE);

        // Approximate time window: ±3 hours from the stated time
        String approxTime = b.etApproxTime != null
            ? b.etApproxTime.getText().toString().trim() : "12:00";
        if (approxTime.isEmpty()) approxTime = "12:00";

        // Parse approx time to build a 6-hour window
        String[] parts   = approxTime.split(":");
        int hour         = parts.length > 0 ? safeInt(parts[0]) : 12;
        int minute       = parts.length > 1 ? safeInt(parts[1]) : 0;
        String timeFrom  = String.format(java.util.Locale.US, "%02d:%02d",
            Math.max(0, hour - 3), minute);
        String timeTo    = String.format(java.util.Locale.US, "%02d:%02d",
            Math.min(23, hour + 3), minute);

        // Collect life events from the events input field (one event per line)
        JsonArray lifeEvents = new JsonArray();
        if (b.etLifeEvents != null) {
            String raw = b.etLifeEvents.getText().toString().trim();
            if (!raw.isEmpty()) {
                for (String line : raw.split("\n")) {
                    if (!line.trim().isEmpty()) {
                        JsonObject ev = new JsonObject();
                        ev.addProperty("description", line.trim());
                        lifeEvents.add(ev);
                    }
                }
            }
        }

        JsonObject body = new JsonObject();
        body.addProperty("date",              date);
        body.addProperty("approx_time_from",  timeFrom);
        body.addProperty("approx_time_to",    timeTo);
        body.addProperty("lat",               prefs.getLat());
        body.addProperty("lon",               prefs.getLon());
        body.addProperty("tz",                prefs.getTz());
        body.add("life_events",               lifeEvents);

        ApiClient.getApiService(this)
            .getRectification(body)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    b.btnRectify.setEnabled(true);
                    b.btnRectify.setText("Rectify Birth Time");

                    if (response.isSuccessful() && response.body() != null) {
                        showResult(response.body());
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Rectification failed.");
                        Toast.makeText(RectificationActivity.this, err,
                            Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    b.btnRectify.setEnabled(true);
                    b.btnRectify.setText("Rectify Birth Time");
                    Toast.makeText(RectificationActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showResult(JsonObject data) {
        if (b.cardResult != null) b.cardResult.setVisibility(View.VISIBLE);

        if (b.tvRectifiedTime != null && data.has("rectified_time"))
            b.tvRectifiedTime.setText(data.get("rectified_time").getAsString());

        if (b.tvRectifiedLagna != null && data.has("lagna"))
            b.tvRectifiedLagna.setText(data.get("lagna").getAsString());

        if (b.tvExplanation != null && data.has("reasoning"))
            b.tvExplanation.setText(data.get("reasoning").getAsString());

        // Confidence badge
        if (b.tvConfidence != null && data.has("confidence")) {
            String conf = data.get("confidence").getAsString();
            b.tvConfidence.setText(conf);
            int color;
            switch (conf.toUpperCase()) {
                case "HIGH":   color = android.graphics.Color.parseColor("#4CAF50"); break;
                case "MEDIUM": color = android.graphics.Color.parseColor("#FF9800"); break;
                default:       color = android.graphics.Color.parseColor("#F44336"); break;
            }
            b.tvConfidence.setTextColor(color);
        }
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
