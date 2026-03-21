package com.bimoraai.brahm.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.bimoraai.brahm.R;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.databinding.FragmentHomeBinding;
import com.bimoraai.brahm.ui.kundali.KundaliChartView;
import com.bimoraai.brahm.ui.secondary.*;
import com.bimoraai.brahm.utils.DateUtils;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.bimoraai.brahm.utils.QuickAccessAdapter;
import com.bimoraai.brahm.utils.QuickAccessItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding b;
    private PrefsHelper prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new PrefsHelper(requireContext());
        setupGreeting();
        setupQuickAccess();
        setupClickListeners();
        loadPanchangSummary();
        loadKundaliPreview();
    }

    private void setupGreeting() {
        String name = prefs.getName();
        b.tvUserName.setText(name.isEmpty() ? "Friend" : name);

        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good morning,"
                        : hour < 17 ? "Good afternoon,"
                        : "Good evening,";
        b.tvGreeting.setText(greeting);
    }

    private void setupQuickAccess() {
        List<QuickAccessItem> items = Arrays.asList(
            new QuickAccessItem("Gochar",       R.drawable.ic_planet,    GocharActivity.class),
            new QuickAccessItem("Compatibility",R.drawable.ic_heart,     CompatibilityActivity.class),
            new QuickAccessItem("Muhurta",      R.drawable.ic_clock,     MuhurtaActivity.class),
            new QuickAccessItem("Horoscope",    R.drawable.ic_star,      HoroscopeActivity.class),
            new QuickAccessItem("Sade Sati",    R.drawable.ic_saturn,    SadeSatiActivity.class),
            new QuickAccessItem("KP System",    R.drawable.ic_grid,      KPActivity.class),
            new QuickAccessItem("Prashna",      R.drawable.ic_question,  PrashnaActivity.class),
            new QuickAccessItem("Palmistry",    R.drawable.ic_hand,      PalmistryActivity.class)
        );

        QuickAccessAdapter adapter = new QuickAccessAdapter(items, item ->
            startActivity(new Intent(requireContext(), item.getTargetActivity())));

        b.rvQuickAccess.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        b.rvQuickAccess.setAdapter(adapter);
    }

    private void setupClickListeners() {
        b.tvViewChart.setOnClickListener(v ->
            requireActivity().getOnBackPressedDispatcher().onBackPressed());

        b.cardKundaliPreview.setOnClickListener(v ->
            requireActivity().findViewById(R.id.bottomNavView).performClick());

        b.cardAskAI.setOnClickListener(v -> { /* handled by bottom nav */ });
    }

    // ── Panchang summary ──────────────────────────────────────────────────────

    private void loadPanchangSummary() {
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
                    if (b == null || !response.isSuccessful()
                            || response.body() == null) return;

                    JsonObject p = response.body();
                    if (b.tvTithi != null && p.has("tithi"))
                        b.tvTithi.setText(p.get("tithi").getAsString());
                    if (b.tvNakshatra != null && p.has("nakshatra"))
                        b.tvNakshatra.setText(p.get("nakshatra").getAsString());
                    if (b.tvRahuKaal != null && p.has("rahu_kaal"))
                        b.tvRahuKaal.setText(p.get("rahu_kaal").getAsString());
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    // Silently fail — home screen stubs remain
                }
            });
    }

    // ── Kundali mini-chart preview ────────────────────────────────────────────

    /**
     * Loads the cached kundali JSON from prefs and sets planet data on the
     * KundaliChartView preview card on the home screen (if the view exists).
     */
    private void loadKundaliPreview() {
        String json = prefs.getKundaliJson();
        if (json.isEmpty()) return;

        try {
            JsonObject kundali = JsonParser.parseString(json).getAsJsonObject();

            // Find the KundaliChartView inside cardKundaliPreview
            if (b.cardKundaliPreview == null) return;
            KundaliChartView miniChart = b.cardKundaliPreview.findViewWithTag("miniChart");
            if (miniChart == null) return;

            if (kundali.has("grahas")) {
                JsonArray grahas = kundali.getAsJsonArray("grahas");
                miniChart.setPlanetsFromJson(grahas);
            }
            if (kundali.has("lagna")) {
                JsonObject lagna = kundali.getAsJsonObject("lagna");
                if (lagna.has("house")) {
                    miniChart.setLagnaHouse(lagna.get("house").getAsInt());
                }
            }
            miniChart.invalidate();
        } catch (Exception e) {
            // Malformed cached JSON — ignore
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
