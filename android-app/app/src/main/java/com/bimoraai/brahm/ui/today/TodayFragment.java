package com.bimoraai.brahm.ui.today;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.FragmentTodayBinding;
import com.bimoraai.brahm.ui.secondary.MuhurtaActivity;
import com.bimoraai.brahm.utils.DateUtils;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Today / Panchang screen.
 *
 * Fetches live Panchang data from GET /api/panchang using the user's
 * saved coordinates and today's date.
 */
public class TodayFragment extends Fragment {

    private FragmentTodayBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentTodayBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDate();
        setupClickListeners();
        loadPanchang();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupDate() {
        if (b == null) return;
        String today = DateUtils.formatFullDate(DateUtils.getCurrentIsoDate());
        b.tvDate.setText(today);
    }

    private void setupClickListeners() {
        if (b == null) return;
        b.btnMuhurta.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), MuhurtaActivity.class)));
    }

    // ── Panchang data ─────────────────────────────────────────────────────────

    private void loadPanchang() {
        if (b == null) return;

        // Show loading state
        showLoading(true);

        PrefsHelper prefs  = new PrefsHelper(requireContext());
        double lat  = prefs.getLat();
        double lon  = prefs.getLon();
        double tz   = prefs.getTz();
        String date = DateUtils.getCurrentIsoDate();

        ApiClient.getApiService(requireContext())
            .getPanchang(lat, lon, tz, date)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (b == null) return;
                    showLoading(false);

                    if (response.isSuccessful() && response.body() != null) {
                        populatePanchang(response.body());
                    } else {
                        // Show stub fallbacks so screen is not empty
                        populateStubs();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    if (b == null) return;
                    showLoading(false);
                    populateStubs();
                }
            });
    }

    private void populatePanchang(JsonObject p) {
        if (b == null) return;

        if (p.has("tithi"))      b.tvTithi.setText(p.get("tithi").getAsString());
        if (p.has("nakshatra"))  b.tvNakshatra.setText(p.get("nakshatra").getAsString());
        if (p.has("yoga"))       b.tvYoga.setText(p.get("yoga").getAsString());
        if (p.has("karana"))     b.tvKarana.setText(p.get("karana").getAsString());
        if (p.has("sunrise"))    b.tvSunrise.setText(p.get("sunrise").getAsString());
        if (p.has("sunset"))     b.tvSunset.setText(p.get("sunset").getAsString());
        if (p.has("rahu_kaal"))  b.tvRahuKaalTime.setText(p.get("rahu_kaal").getAsString());
    }

    /** Fallback stubs shown when the API is unreachable. */
    private void populateStubs() {
        if (b == null) return;
        b.tvTithi.setText("—");
        b.tvNakshatra.setText("—");
        b.tvYoga.setText("—");
        b.tvKarana.setText("—");
        b.tvSunrise.setText("—");
        b.tvSunset.setText("—");
        b.tvRahuKaalTime.setText("—");
    }

    private void showLoading(boolean show) {
        if (b == null) return;
        // If the layout has a progressBar, toggle it
        if (b.progressBar != null) {
            b.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
