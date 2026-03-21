package com.bimoraai.brahm.model;

/**
 * Planetary (graha) data for a single planet as returned by the Kundali API.
 * Used by KundaliPlanetsTabFragment and KundaliNavamshaTabFragment.
 */
public class GrahaData {

    private final String rashi;     // e.g. "Mesh", "Vrishabha"
    private final int    house;     // 1–12
    private final double degree;    // degree within the rashi (0.0–29.99)
    private final boolean retro;    // retrograde flag
    private final String nakshatra; // e.g. "Ashwini", "Rohini"
    private final String status;    // e.g. "Exalted", "Debilitated", "Own Sign", ""

    public GrahaData(String rashi, int house, double degree,
                     boolean retro, String nakshatra, String status) {
        this.rashi     = rashi;
        this.house     = house;
        this.degree    = degree;
        this.retro     = retro;
        this.nakshatra = nakshatra;
        this.status    = status != null ? status : "";
    }

    public String  getRashi()     { return rashi; }
    public int     getHouse()     { return house; }
    public double  getDegree()    { return degree; }
    public boolean isRetro()      { return retro; }
    public String  getNakshatra() { return nakshatra; }
    public String  getStatus()    { return status; }

    /**
     * Returns the degree formatted as "12°34'" (degrees + arcminutes).
     */
    public String getDegreeFormatted() {
        int d = (int) degree;
        int m = (int) ((degree - d) * 60);
        return d + "\u00b0" + String.format("%02d", m) + "'";
    }
}
