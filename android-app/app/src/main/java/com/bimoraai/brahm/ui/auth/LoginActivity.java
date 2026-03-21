package com.bimoraai.brahm.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bimoraai.brahm.R;
import com.bimoraai.brahm.api.ApiClient;
import com.bimoraai.brahm.api.models.SendOtpRequest;
import com.bimoraai.brahm.api.models.SendOtpResponse;
import com.bimoraai.brahm.api.models.VerifyOtpRequest;
import com.bimoraai.brahm.api.models.VerifyOtpResponse;
import com.bimoraai.brahm.databinding.ActivityLoginBinding;
import com.bimoraai.brahm.ui.main.MainActivity;
import com.bimoraai.brahm.utils.PrefsHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private String phone = "";
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setupPhoneInput();
        setupOtpInputs();
        setupClickListeners();
    }

    private void setupPhoneInput() {
        b.btnSendOtp.setOnClickListener(v -> {
            phone = b.etPhone.getText().toString().trim();
            if (phone.length() < 10) {
                showError("Enter a valid phone number");
                return;
            }
            // Normalize: add +91 if not present
            if (!phone.startsWith("+")) phone = "+91" + phone;
            sendOtp(phone);
        });
    }

    private void setupOtpInputs() {
        // Auto-advance OTP boxes
        EditText[] otpBoxes = { b.otp1, b.otp2, b.otp3, b.otp4, b.otp5, b.otp6 };
        for (int i = 0; i < otpBoxes.length; i++) {
            final int idx = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && idx < otpBoxes.length - 1) {
                        otpBoxes[idx + 1].requestFocus();
                    }
                    if (s.length() == 0 && idx > 0) {
                        otpBoxes[idx - 1].requestFocus();
                    }
                }
            });
        }
    }

    private void setupClickListeners() {
        b.btnVerifyOtp.setOnClickListener(v -> {
            String otp = getOtpValue();
            if (otp.length() < 6) { showError("Enter complete OTP"); return; }
            verifyOtp(phone, otp);
        });

        b.tvResend.setOnClickListener(v -> sendOtp(phone));
    }

    // ── API calls ─────────────────────────────────────────────────────────────

    private void sendOtp(String phoneNumber) {
        showLoading(true);
        hideError();

        ApiClient.getApiService(this)
            .sendOtp(new SendOtpRequest(phoneNumber))
            .enqueue(new Callback<SendOtpResponse>() {
                @Override
                public void onResponse(@NonNull Call<SendOtpResponse> call,
                                       @NonNull Response<SendOtpResponse> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null
                            && response.body().isSent()) {
                        showOtpLayout(phoneNumber);
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Failed to send OTP. Try again.");
                        showError(err);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<SendOtpResponse> call,
                                      @NonNull Throwable t) {
                    showLoading(false);
                    showError("Network error: " + t.getMessage());
                }
            });
    }

    private void verifyOtp(String phoneNumber, String otp) {
        showLoading(true);
        hideError();

        ApiClient.getApiService(this)
            .verifyOtp(new VerifyOtpRequest(phoneNumber, otp))
            .enqueue(new Callback<VerifyOtpResponse>() {
                @Override
                public void onResponse(@NonNull Call<VerifyOtpResponse> call,
                                       @NonNull Response<VerifyOtpResponse> response) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        VerifyOtpResponse body = response.body();

                        PrefsHelper prefs = new PrefsHelper(LoginActivity.this);
                        prefs.saveToken(body.getAccessToken());
                        prefs.saveName(body.getName());
                        prefs.savePlan(body.getPlan());
                        prefs.savePhone(phoneNumber);

                        // Navigate: onboarding if first-time, main if returning
                        if (prefs.getBirthDate().isEmpty()) {
                            startActivity(new Intent(LoginActivity.this,
                                OnboardingActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this,
                                MainActivity.class));
                        }
                        finish();
                    } else {
                        String err = ApiClient.parseError(
                            response.errorBody(), "Invalid OTP. Please try again.");
                        showError(err);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<VerifyOtpResponse> call,
                                      @NonNull Throwable t) {
                    showLoading(false);
                    showError("Network error: " + t.getMessage());
                }
            });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showOtpLayout(String phoneNumber) {
        b.layoutPhone.setVisibility(View.GONE);
        b.layoutOtp.setVisibility(View.VISIBLE);
        b.tvOtpSentTo.setText("OTP sent to " + phoneNumber);
        b.otp1.requestFocus();
        startResendTimer();
    }

    private void startResendTimer() {
        b.tvResend.setEnabled(false);
        b.tvResend.setAlpha(0.5f);
        resendTimer = new CountDownTimer(30000, 1000) {
            @Override public void onTick(long ms) {
                b.tvResend.setText(getString(R.string.resend_in, (int)(ms / 1000)));
            }
            @Override public void onFinish() {
                b.tvResend.setText(R.string.resend_otp);
                b.tvResend.setEnabled(true);
                b.tvResend.setAlpha(1f);
            }
        }.start();
    }

    private String getOtpValue() {
        return b.otp1.getText().toString()
             + b.otp2.getText().toString()
             + b.otp3.getText().toString()
             + b.otp4.getText().toString()
             + b.otp5.getText().toString()
             + b.otp6.getText().toString();
    }

    private void showLoading(boolean show) {
        b.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        b.btnSendOtp.setEnabled(!show);
        b.btnVerifyOtp.setEnabled(!show);
    }

    private void showError(String msg) {
        b.tvError.setText(msg);
        b.tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        b.tvError.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}
