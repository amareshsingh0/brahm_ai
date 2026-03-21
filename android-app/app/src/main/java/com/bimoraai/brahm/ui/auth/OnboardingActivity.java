package com.bimoraai.brahm.ui.auth;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.api.models.KundaliRequest;
import com.bimoraai.brahm.databinding.ActivityOnboardingBinding;
import com.bimoraai.brahm.model.City;
import com.bimoraai.brahm.ui.main.MainActivity;
import com.bimoraai.brahm.utils.CityAdapter;
import com.bimoraai.brahm.utils.PrefsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding b;
    private double lat = 0, lon = 0, tz = 5.5;
    private String selectedCity = "";
    private CityAdapter cityAdapter;
    private PrefsHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        prefs = new PrefsHelper(this);

        setupCitySearch();
        setupDatePicker();
        setupTimePicker();
        setupGenerateButton();
    }

    private void setupCitySearch() {
        cityAdapter = new CityAdapter(new ArrayList<>(), city -> {
            selectedCity = city.getName();
            lat = city.getLat();
            lon = city.getLon();
            tz  = city.getTz();
            b.etPlace.setText(city.getName());
            b.tvLat.setText(String.format(Locale.US, "Lat: %.2f", lat));
            b.tvLon.setText(String.format(Locale.US, "Lon: %.2f", lon));
            b.tvTz.setText(String.format(Locale.US, "TZ: +%.1f", tz));
            b.rvCities.setVisibility(View.GONE);
            cityAdapter.updateList(new ArrayList<>());
        });
        b.rvCities.setLayoutManager(new LinearLayoutManager(this));
        b.rvCities.setAdapter(cityAdapter);

        b.etPlace.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                if (q.length() >= 2) {
                    searchCities(q);
                } else {
                    b.rvCities.setVisibility(View.GONE);
                }
            }
        });
    }

    private void searchCities(String query) {
        ApiClient.getApiService(this)
            .searchCities(query)
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    if (!response.isSuccessful() || response.body() == null) return;
                    JsonObject body = response.body();
                    if (!body.has("cities")) return;

                    List<City> cities = new ArrayList<>();
                    JsonArray arr = body.getAsJsonArray("cities");
                    for (JsonElement el : arr) {
                        JsonObject c = el.getAsJsonObject();
                        String name    = c.has("name")    ? c.get("name").getAsString()    : "";
                        double cityLat = c.has("lat")     ? c.get("lat").getAsDouble()     : 0;
                        double cityLon = c.has("lon")     ? c.get("lon").getAsDouble()     : 0;
                        double cityTz  = c.has("tz")      ? c.get("tz").getAsDouble()      : 5.5;
                        cities.add(new City(name, cityLat, cityLon, cityTz));
                    }
                    cityAdapter.updateList(cities);
                    b.rvCities.setVisibility(cities.isEmpty() ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    // Silently fail — city search is best-effort
                    b.rvCities.setVisibility(View.GONE);
                }
            });
    }

    private void setupDatePicker() {
        b.etDob.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                b.etDob.setText(date);
            }, cal.get(Calendar.YEAR) - 25, cal.get(Calendar.MONTH),
               cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupTimePicker() {
        b.etTob.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                String time = String.format(Locale.US, "%02d:%02d", hour, minute);
                b.etTob.setText(time);
            }, 12, 0, true).show();
        });
    }

    private void setupGenerateButton() {
        b.btnGenerate.setOnClickListener(v -> {
            String name = b.etName.getText().toString().trim();
            String dob  = b.etDob.getText().toString().trim();
            String tob  = b.etTob.getText().toString().trim();

            if (name.isEmpty()) { showError("Please enter your name"); return; }
            if (dob.isEmpty())  { showError("Please select date of birth"); return; }
            if (tob.isEmpty())  { showError("Please select time of birth"); return; }
            if (lat == 0 && lon == 0) { showError("Please select a birth place"); return; }

            generateKundali(name, dob, tob);
        });
    }

    private void generateKundali(String name, String dob, String tob) {
        showLoading(true);
        hideError();

        ApiClient.getApiService(this)
            .getKundali(new KundaliRequest(dob, tob, lat, lon, tz, name))
            .enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call,
                                       @NonNull Response<JsonObject> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        // Save birth data and kundali JSON to prefs
                        prefs.saveName(name);
                        prefs.saveBirthDate(dob);
                        prefs.saveBirthTime(tob);
                        prefs.saveBirthPlace(selectedCity);
                        prefs.saveLat(lat);
                        prefs.saveLon(lon);
                        prefs.saveTz(tz);
                        prefs.saveKundaliJson(response.body().toString());

                        startActivity(new Intent(OnboardingActivity.this,
                            MainActivity.class));
                        finish();
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(),
                            "Could not generate kundali. Please try again.");
                        showError(err);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call,
                                      @NonNull Throwable t) {
                    showLoading(false);
                    showError("Network error: " + t.getMessage());
                }
            });
    }

    private void showLoading(boolean show) {
        b.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnGenerate.setEnabled(!show);
    }

    private void showError(String msg) {
        b.tvError.setText(msg);
        b.tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        b.tvError.setVisibility(View.GONE);
    }
}
