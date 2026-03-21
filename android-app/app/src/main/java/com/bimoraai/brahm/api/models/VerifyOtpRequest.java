package com.bimoraai.brahm.api.models;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpRequest {

    @SerializedName("phone")
    private final String phone;

    @SerializedName("otp")
    private final String otp;

    public VerifyOtpRequest(String phone, String otp) {
        this.phone = phone;
        this.otp   = otp;
    }

    public String getPhone() { return phone; }
    public String getOtp()   { return otp; }
}
