package com.bimoraai.brahm.api.models;

import com.google.gson.annotations.SerializedName;

public class KundaliRequest {

    @SerializedName("date")
    private final String date;

    @SerializedName("time")
    private final String time;

    @SerializedName("lat")
    private final double lat;

    @SerializedName("lon")
    private final double lon;

    @SerializedName("tz")
    private final double tz;

    @SerializedName("name")
    private final String name;

    public KundaliRequest(String date, String time, double lat, double lon, double tz, String name) {
        this.date = date;
        this.time = time;
        this.lat  = lat;
        this.lon  = lon;
        this.tz   = tz;
        this.name = name;
    }

    public String getDate() { return date; }
    public String getTime() { return time; }
    public double getLat()  { return lat; }
    public double getLon()  { return lon; }
    public double getTz()   { return tz; }
    public String getName() { return name; }
}
