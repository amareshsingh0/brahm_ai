package com.bimoraai.brahm.api.models;

import com.google.gson.annotations.SerializedName;

public class SendOtpRequest {

    @SerializedName("phone")
    private final String phone;

    public SendOtpRequest(String phone) {
        this.phone = phone;
    }

    public String getPhone() { return phone; }
}
