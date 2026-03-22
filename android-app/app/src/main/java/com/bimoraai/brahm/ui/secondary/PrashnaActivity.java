package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityPrashnaBinding;
import com.bimoraai.brahm.utils.DateUtils;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Prashna Kundali (Horary Astrology) screen.
 *
 * On "Ask Now" click: captures the current moment + location and calls
 * POST /api/prashna. Shows verdict banner (YES / NO / MIXED).
 */
public class PrashnaActivity extends AppCompatActivity {

    private ActivityPrashnaBinding b;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPrashnaBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        prefillLocation();
        setupAskButton();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void prefillLocation() {
        if (b.etLocation != null && !prefs.getBirthPlace().isEmpty())
            b.etLocation.setText(prefs.getBirthPlace());
        if (b.tvCurrentTime != null)
            b.tvCurrentTime.setText(
                DateUtils.formatFullDate(DateUtils.getCurrentIsoDate())
                + " " + DateUtils.getCurrentTime());
    }

    private void setupAskButton() {
        b.btnAskNow.setOnClickListener(v -> askPrashna());
    }

    // ── Prashna query ─────────────────────────────────────────────────────────

    private void askPrashna() {
        String question = b.etQuestion != null
                          ? b.etQuestion.getText().toString().trim() : "";
        if (question.isEmpty()) {
            Toast.makeText(this, "Please enter your question", Toast.LENGTH_SHORT).show();
            return;
        }

        b.btnAskNow.setEnabled(false);
        b.btnAskNow.setText("Calculating…");
        if (b.verdictBanner != null) b.verdictBanner.setVisibility(View.GONE);

        // Snapshot current time
        String currentDate = DateUtils.getCurrentIsoDate();
        String currentTime = DateUtils.getCurrentTime();
        if (b.tvCurrentTime != null)
            b.tvCurrentTime.setText(
                DateUtils.formatFullDate(currentDate) + " " + currentTime);

        JsonObject body = new JsonObject();
        body.addProperty("question",       question);
        body.addProperty("date",           currentDate);
        body.addProperty("time",           currentTime);
        body.addProperty("lat",            prefs.getLat());
        body.addProperty("lon",            prefs.getLon());
        body.addProperty("tz",             prefs.getTz());
        body.addProperty("question_type",  "general");

        ApiClient.getApiService(this)
            .getPrashna(body)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    b.btnAskNow.setEnabled(true);
                    b.btnAskNow.setText("Ask Now");

                    if (response.isSuccessful() && response.body() != null) {
                        showResult(response.body());
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Prashna analysis failed.");
                        Toast.makeText(PrashnaActivity.this, err, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    b.btnAskNow.setEnabled(true);
                    b.btnAskNow.setText("Ask Now");
                    Toast.makeText(PrashnaActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showResult(JsonObject data) {
        String verdict = data.has("prashna_verdict")
            ? data.get("prashna_verdict").getAsString() : "MIXED";
        String summary = data.has("summary")
            ? data.get("summary").getAsString() : "";

        showVerdict(verdict, summary);

        // Hora lord
        if (b.tvHoraLord != null && data.has("hora_lord"))
            b.tvHoraLord.setText("Hora Lord: " + data.get("hora_lord").getAsString());

        // Factors list
        if (b.tvFactors != null && data.has("prashna_factors")) {
            JsonArray factors = data.getAsJsonArray("prashna_factors");
            StringBuilder sb = new StringBuilder();
            for (JsonElement el : factors) sb.append("• ").append(el.getAsString()).append("\n");
            b.tvFactors.setText(sb.toString().trim());
        }
    }

    private void showVerdict(String verdict, String summary) {
        if (b == null || b.verdictBanner == null) return;
        b.verdictBanner.setVisibility(View.VISIBLE);

        if (b.tvVerdict       != null) b.tvVerdict.setText(verdict);
        if (b.tvVerdictSummary!= null) b.tvVerdictSummary.setText(summary);

        int bgColor;
        switch (verdict.toUpperCase()) {
            case "YES":   bgColor = android.graphics.Color.parseColor("#4CAF50"); break;
            case "NO":    bgColor = android.graphics.Color.parseColor("#F44336"); break;
            default:      bgColor = android.graphics.Color.parseColor("#FF9800"); break;
        }
        b.verdictBanner.setBackgroundColor(bgColor);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
