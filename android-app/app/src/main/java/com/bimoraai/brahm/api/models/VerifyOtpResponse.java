package com.bimoraai.brahm.api.models;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("name")
    private String name;

    @SerializedName("plan")
    private String plan;

    public String getAccessToken() { return accessToken != null ? accessToken : ""; }
    public String getName()        { return name        != null ? name        : ""; }
    public String getPlan()        { return plan        != null ? plan        : "free"; }
}
