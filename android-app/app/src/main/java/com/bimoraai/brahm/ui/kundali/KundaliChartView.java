package com.bimoraai.brahm.ui.kundali;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom View — draws the North Indian Kundali chart (diamond-grid style).
 * Houses are fixed positions; planets are placed by house number.
 */
public class KundaliChartView extends View {

    private final Paint linePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint housePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint planetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Planet abbreviations per house
    private final Map<Integer, String> housePlanets = new HashMap<>();
    private int lagnaRashi = 1; // 1-12

    // North Indian layout: fixed house positions (house number at grid cell)
    // Grid is 4x4, houses 1-12 clockwise from top-center
    private static final int[][] HOUSE_GRID = {
        // row, col for each house 1-12
        {0,1},{0,2},{1,3},{2,3},{3,2},{3,1},{3,0},{2,0},{1,0},{0,0},{0,1},{0,2}
        // simplified — actual North Indian layout uses triangles
    };

    public KundaliChartView(Context context) { super(context); init(); }
    public KundaliChartView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public KundaliChartView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(); }

    private void init() {
        bgPaint.setColor(Color.parseColor("#18181B"));
        bgPaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(Color.parseColor("#3F3F46"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);

        housePaint.setColor(Color.parseColor("#27272A"));
        housePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.parseColor("#A1A1AA"));
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        planetPaint.setColor(Color.parseColor("#FAFAFA"));
        planetPaint.setAntiAlias(true);
        planetPaint.setTextAlign(Paint.Align.CENTER);
        planetPaint.setFakeBoldText(true);
    }

    /** Set planets: map of house number (1-12) → planet abbreviation string */
    public void setPlanets(Map<Integer, String> planets, int lagnaRashi) {
        this.housePlanets.clear();
        this.housePlanets.putAll(planets);
        this.lagnaRashi = lagnaRashi;
        invalidate();
    }

    /**
     * Convenience method: populates the chart from the "grahas" JsonArray returned
     * by POST /api/kundali. Each element must have "name" and "house" fields.
     * Call {@link #setLagnaHouse(int)} afterwards to set the Lagna house number.
     */
    public void setPlanetsFromJson(JsonArray grahas) {
        housePlanets.clear();
        // Planet abbreviation map
        java.util.Map<String, String> abbr = new java.util.HashMap<>();
        abbr.put("Sun", "Su"); abbr.put("Moon", "Mo"); abbr.put("Mars", "Ma");
        abbr.put("Mercury", "Me"); abbr.put("Jupiter", "Ju"); abbr.put("Venus", "Ve");
        abbr.put("Saturn", "Sa"); abbr.put("Rahu", "Ra"); abbr.put("Ketu", "Ke");

        for (JsonElement el : grahas) {
            JsonObject g = el.getAsJsonObject();
            if (!g.has("house") || !g.has("name")) continue;
            int    house = g.get("house").getAsInt();
            String name  = g.get("name").getAsString();
            String a     = abbr.containsKey(name) ? abbr.get(name) : name.substring(0, 2);

            // Append to existing planets in the same house
            String existing = housePlanets.getOrDefault(house, "");
            housePlanets.put(house, existing.isEmpty() ? a : existing + " " + a);
        }
        // Do NOT call invalidate() here — caller should call it explicitly after
        // setLagnaHouse() is also called (or invalidate from the outside).
    }

    /**
     * Sets the lagna (ascendant) house number (1–12).
     * The chart marks this house with the Lagna label.
     */
    public void setLagnaHouse(int houseNumber) {
        this.lagnaRashi = houseNumber;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float size = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        float half = size / 2f;

        // Background
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Outer square
        float L = cx - half + 8;
        float T = cy - half + 8;
        float R = cx + half - 8;
        float B = cy + half - 8;

        // Draw outer border
        canvas.drawRect(L, T, R, B, linePaint);

        // Draw inner diamond (diagonals)
        Path diamond = new Path();
        diamond.moveTo(cx, T);
        diamond.lineTo(R, cy);
        diamond.lineTo(cx, B);
        diamond.lineTo(L, cy);
        diamond.close();
        canvas.drawPath(diamond, linePaint);

        // Draw inner cross lines
        canvas.drawLine(L, T, R, B, linePaint);
        canvas.drawLine(R, T, L, B, linePaint);

        // House number labels (small, muted)
        textPaint.setTextSize(size * 0.045f);
        float[][] houseCenter = getHouseCenters(L, T, R, B, cx, cy);

        for (int house = 1; house <= 12; house++) {
            float hx = houseCenter[house-1][0];
            float hy = houseCenter[house-1][1];

            // House number
            canvas.drawText(String.valueOf(house), hx, hy - size * 0.04f, textPaint);

            // Planet text
            String planets = housePlanets.getOrDefault(house, "");
            if (!planets.isEmpty()) {
                planetPaint.setTextSize(size * 0.042f);
                planetPaint.setColor(getPlanetColor(planets));
                canvas.drawText(planets, hx, hy + size * 0.04f, planetPaint);
            }
        }

        // Lagna marker (Asc house — house 1 highlighted)
        // Small "Asc" label
        float[] lagnaC = houseCenter[0];
        textPaint.setColor(Color.parseColor("#7C3AED"));
        textPaint.setTextSize(size * 0.038f);
        canvas.drawText("Asc", lagnaC[0], lagnaC[1], textPaint);
        textPaint.setColor(Color.parseColor("#A1A1AA")); // reset
    }

    /**
     * Returns [cx, cy] for each of the 12 houses in North Indian fixed layout.
     * House positions (clockwise from top-center):
     * 12,1,2  (top row left-to-right triangles)
     * 11,  ,3 (middle row)
     * 10,9,8  ... etc
     */
    private float[][] getHouseCenters(float L, float T, float R, float B, float cx, float cy) {
        float qw = (R - L) / 4f;
        float qh = (B - T) / 4f;
        return new float[][] {
            {cx,           T + qh},        // 1  — top center triangle
            {cx + qw,      T + qh * 0.6f}, // 2  — top right
            {R - qw * 0.5f, cy - qh},      // 3  — right top
            {R - qw * 0.4f, cy},           // 4  — right center
            {R - qw * 0.5f, cy + qh},      // 5  — right bottom
            {cx + qw,      B - qh * 0.6f}, // 6  — bottom right
            {cx,           B - qh},        // 7  — bottom center
            {cx - qw,      B - qh * 0.6f}, // 8  — bottom left
            {L + qw * 0.5f, cy + qh},      // 9  — left bottom
            {L + qw * 0.4f, cy},           // 10 — left center
            {L + qw * 0.5f, cy - qh},      // 11 — left top
            {cx - qw,      T + qh * 0.6f}, // 12 — top left
        };
    }

    private int getPlanetColor(String planets) {
        if (planets.contains("Su")) return Color.parseColor("#F59E0B");
        if (planets.contains("Mo")) return Color.parseColor("#94A3B8");
        if (planets.contains("Ma")) return Color.parseColor("#EF4444");
        if (planets.contains("Me")) return Color.parseColor("#22C55E");
        if (planets.contains("Ju")) return Color.parseColor("#EAB308");
        if (planets.contains("Ve")) return Color.parseColor("#A855F7");
        if (planets.contains("Sa")) return Color.parseColor("#64748B");
        if (planets.contains("Ra")) return Color.parseColor("#6366F1");
        if (planets.contains("Ke")) return Color.parseColor("#F97316");
        return Color.parseColor("#FAFAFA");
    }
}
