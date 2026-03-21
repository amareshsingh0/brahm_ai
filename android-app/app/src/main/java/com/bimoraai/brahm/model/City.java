package com.bimoraai.brahm.model;

/**
 * Represents a city returned by the city-search API.
 * Used in OnboardingActivity and secondary activities that need location input.
 */
public class City {

    private final String name;
    private final String state;
    private final String country;
    private final double lat;
    private final double lon;
    private final double tz;

    public City(String name, String state, String country,
                double lat, double lon, double tz) {
        this.name    = name;
        this.state   = state;
        this.country = country;
        this.lat     = lat;
        this.lon     = lon;
        this.tz      = tz;
    }

    public String getName()    { return name; }
    public String getState()   { return state; }
    public String getCountry() { return country; }
    public double getLat()     { return lat; }
    public double getLon()     { return lon; }
    public double getTz()      { return tz; }

    /** Display string shown in search results: "Delhi, Delhi, India" */
    @Override
    public String toString() {
        return name + ", " + state + ", " + country;
    }
}
