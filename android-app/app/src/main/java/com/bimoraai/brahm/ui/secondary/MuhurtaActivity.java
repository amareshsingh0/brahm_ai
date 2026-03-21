package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityMuhurtaBinding;
import com.bimoraai.brahm.utils.DateUtils;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Muhurta (Auspicious Timing) Finder screen.
 *
 * The user selects an activity type via chips and a date range, then taps
 * Find Muhurta. Results come from POST /api/muhurta/activity.
 */
public class MuhurtaActivity extends AppCompatActivity {

    private ActivityMuhurtaBinding b;
    private PrefsHelper prefs;
    private String selectedActivity = "";

    private static final String[] ACTIVITY_TYPES = {
        "marriage", "business", "travel", "grih_pravesh",
        "namkaran", "education", "medical", "vehicle"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMuhurtaBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        setupActivityChips();
        setupFindButton();
        prefillDate();
    }

    private void setupToolbar() {
        if (b.btnBack != null) b.btnBack.setOnClickListener(v -> finish());
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupActivityChips() {
        if (b.chipGroup == null) return;
        for (int i = 0; i < b.chipGroup.getChildCount(); i++) {
            View child = b.chipGroup.getChildAt(i);
            if (child instanceof com.google.android.material.chip.Chip) {
                final String activityType = i < ACTIVITY_TYPES.length
                    ? ACTIVITY_TYPES[i] : "general";
                child.setOnClickListener(v -> selectedActivity = activityType);
            }
        }
    }

    private void setupFindButton() {
        b.btnFindMuhurta.setOnClickListener(v -> findMuhurta());
    }

    private void prefillDate() {
        if (b.etDate != null) b.etDate.setText(DateUtils.getCurrentIsoDate());
    }

    // ── Muhurta search ────────────────────────────────────────────────────────

    private void findMuhurta() {
        if (selectedActivity.isEmpty()) {
            Toast.makeText(this, "Please select an activity type",
                Toast.LENGTH_SHORT).show();
            return;
        }

        String date = b.etDate != null ? b.etDate.getText().toString().trim() : "";
        if (date.isEmpty()) date = DateUtils.getCurrentIsoDate();

        b.btnFindMuhurta.setEnabled(false);
        b.btnFindMuhurta.setText("Finding…");

        JsonObject body = new JsonObject();
        body.addProperty("activity",  selectedActivity);
        body.addProperty("lat",       prefs.getLat());
        body.addProperty("lon",       prefs.getLon());
        body.addProperty("tz",        prefs.getTz());
        body.addProperty("date_from", date);
        body.addProperty("date_to",   date); // single-day search by default

        ApiClient.getApiService(this)
            .getMuhurta(body)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    b.btnFindMuhurta.setEnabled(true);
                    b.btnFindMuhurta.setText("Find Muhurta");

                    if (response.isSuccessful() && response.body() != null) {
                        showResult(response.body());
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "No muhurta found.");
                        Toast.makeText(MuhurtaActivity.this, err,
                            Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    b.btnFindMuhurta.setEnabled(true);
                    b.btnFindMuhurta.setText("Find Muhurta");
                    Toast.makeText(MuhurtaActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showResult(JsonObject data) {
        if (b.cardResult == null) return;
        b.cardResult.setVisibility(View.VISIBLE);

        // Build a human-readable muhurta list
        if (b.tvMuhurtaList != null) {
            if (data.has("muhurtas")) {
                JsonArray muhurtas = data.getAsJsonArray("muhurtas");
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : muhurtas) {
                    JsonObject m = el.getAsJsonObject();
                    String date   = m.has("date")   ? m.get("date").getAsString()   : "";
                    String start  = m.has("start")  ? m.get("start").getAsString()  : "";
                    String end    = m.has("end")    ? m.get("end").getAsString()    : "";
                    String quality= m.has("quality")? m.get("quality").getAsString(): "";
                    sb.append(date).append("  ").append(start).append(" – ").append(end);
                    if (!quality.isEmpty()) sb.append("  (").append(quality).append(")");
                    sb.append("\n\n");
                }
                b.tvMuhurtaList.setText(sb.toString().trim());
            } else if (data.has("message")) {
                b.tvMuhurtaList.setText(data.get("message").getAsString());
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
