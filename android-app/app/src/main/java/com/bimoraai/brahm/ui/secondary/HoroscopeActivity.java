package com.bimoraai.brahm.ui.secondary;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.ActivityHoroscopeBinding;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonObject;
import com.google.android.material.chip.Chip;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Daily/Weekly Horoscope screen.
 *
 * Loads horoscope from GET /api/horoscope/{rashi} for the selected sign.
 */
public class HoroscopeActivity extends AppCompatActivity {

    private ActivityHoroscopeBinding b;
    private PrefsHelper prefs;
    private int selectedRashiIndex = 0;

    private static final String[] RASHI_NAMES_EN = {
        "mesh", "vrishabha", "mithuna", "karka",
        "simha", "kanya", "tula", "vrishchika",
        "dhanu", "makara", "kumbha", "meena"
    };

    private static final String[] RASHI_DISPLAY = {
        "Mesh \u2648", "Vrishabha \u2649", "Mithuna \u264A", "Karka \u264B",
        "Simha \u264C", "Kanya \u264D", "Tula \u264E", "Vrishchika \u264F",
        "Dhanu \u2650", "Makara \u2651", "Kumbha \u2652", "Meena \u2653"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityHoroscopeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        prefs = new PrefsHelper(this);

        setupToolbar();
        setupRashiChips();
        loadHoroscope(selectedRashiIndex);
    }

    private void setupToolbar() {
        if (b.btnBack != null) b.btnBack.setOnClickListener(v -> finish());
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupRashiChips() {
        if (b.chipGroupRashi == null) return;
        b.chipGroupRashi.removeAllViews();

        for (int i = 0; i < RASHI_DISPLAY.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(RASHI_DISPLAY[i]);
            chip.setCheckable(true);
            chip.setChecked(i == selectedRashiIndex);

            final int idx = i;
            chip.setOnClickListener(v -> {
                selectedRashiIndex = idx;
                for (int j = 0; j < b.chipGroupRashi.getChildCount(); j++) {
                    View child = b.chipGroupRashi.getChildAt(j);
                    if (child instanceof Chip) ((Chip) child).setChecked(j == idx);
                }
                loadHoroscope(idx);
            });

            b.chipGroupRashi.addView(chip);
        }
    }

    // ── Horoscope loading ─────────────────────────────────────────────────────

    private void loadHoroscope(int rashiIndex) {
        if (b.progressBar   != null) b.progressBar.setVisibility(View.VISIBLE);
        if (b.cardHoroscope != null) b.cardHoroscope.setVisibility(View.GONE);

        // Use lowercase slug for the API path
        String rashiSlug = RASHI_NAMES_EN[rashiIndex];

        ApiClient.getApiService(this)
            .getHoroscope(rashiSlug)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    if (b.progressBar   != null) b.progressBar.setVisibility(View.GONE);
                    if (b.cardHoroscope != null) b.cardHoroscope.setVisibility(View.VISIBLE);

                    if (response.isSuccessful() && response.body() != null) {
                        populateHoroscope(response.body());
                    } else {
                        populateFallback(rashiSlug);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    if (b.progressBar   != null) b.progressBar.setVisibility(View.GONE);
                    if (b.cardHoroscope != null) b.cardHoroscope.setVisibility(View.VISIBLE);
                    populateFallback(rashiSlug);
                }
            });
    }

    private void populateHoroscope(JsonObject data) {
        if (b == null) return;

        if (b.tvRashiName != null && data.has("rashi"))
            b.tvRashiName.setText(data.get("rashi").getAsString());

        // Summary — try multiple field names
        String summary = field(data, "summary", field(data, "prediction",
            field(data, "description", "—")));
        if (b.tvHoroscopeSummary != null) b.tvHoroscopeSummary.setText(summary);

        if (b.tvLuckyNumber != null)
            b.tvLuckyNumber.setText(field(data, "lucky_number", "—"));
        if (b.tvLuckyColor  != null)
            b.tvLuckyColor.setText(field(data, "lucky_color", "—"));
        if (b.tvLuckyDay    != null)
            b.tvLuckyDay.setText(field(data, "lucky_day", "—"));

        // Individual sections if present
        if (b.tvLove    != null) b.tvLove.setText(field(data, "love", ""));
        if (b.tvCareer  != null) b.tvCareer.setText(field(data, "career", ""));
        if (b.tvHealth  != null) b.tvHealth.setText(field(data, "health", ""));
        if (b.tvFinance != null) b.tvFinance.setText(field(data, "finance", ""));
    }

    private void populateFallback(String rashi) {
        if (b == null) return;
        if (b.tvRashiName       != null) b.tvRashiName.setText(capitalize(rashi));
        if (b.tvHoroscopeSummary!= null) b.tvHoroscopeSummary.setText("—");
        if (b.tvLuckyNumber     != null) b.tvLuckyNumber.setText("—");
        if (b.tvLuckyColor      != null) b.tvLuckyColor.setText("—");
        if (b.tvLuckyDay        != null) b.tvLuckyDay.setText("—");
    }

    private static String field(JsonObject obj, String key, String fallback) {
        return (obj.has(key) && !obj.get(key).isJsonNull())
            ? obj.get(key).getAsString() : fallback;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
