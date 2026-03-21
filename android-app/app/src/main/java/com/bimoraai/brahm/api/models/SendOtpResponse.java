package com.bimoraai.brahm.api.models;

import com.google.gson.annotations.SerializedName;

public class SendOtpResponse {

    @SerializedName("sent")
    private boolean sent;

    @SerializedName("message")
    private String message;

    public boolean isSent()      { return sent; }
    public String  getMessage()  { return message != null ? message : ""; }
}
