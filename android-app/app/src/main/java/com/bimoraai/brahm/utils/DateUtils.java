package com.bimoraai.brahm.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Static date / time formatting utilities used across the app.
 * All ISO strings are expected in "yyyy-MM-dd" format.
 */
public final class DateUtils {

    // Prevent instantiation
    private DateUtils() {}

    private static final SimpleDateFormat ISO_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Converts an ISO date string to a compact display date.
     *
     * @param isoDate e.g. "1990-08-15"
     * @return        e.g. "15 August 1990"
     */
    public static String formatDisplayDate(String isoDate) {
        Date d = parseIso(isoDate);
        if (d == null) return isoDate;
        return new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(d);
    }

    /**
     * Converts an ISO date string to a full human-readable date including day name.
     *
     * @param isoDate e.g. "2026-03-21"
     * @return        e.g. "Saturday, 21 March 2026"
     */
    public static String formatFullDate(String isoDate) {
        Date d = parseIso(isoDate);
        if (d == null) return isoDate;
        return new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(d);
    }

    /**
     * Returns today's date as an ISO string.
     *
     * @return e.g. "2026-03-22"
     */
    public static String getCurrentIsoDate() {
        return ISO_FORMAT.format(new Date());
    }

    /**
     * Returns the current local time as "HH:mm" (24-hour clock).
     *
     * @return e.g. "14:35"
     */
    public static String getCurrentTime() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
    }

    /**
     * Convenience overload: formats today's date in full form without a parameter.
     *
     * @return e.g. "Sunday, 22 March 2026"
     */
    public static String formatTodayFull() {
        return formatFullDate(getCurrentIsoDate());
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private static Date parseIso(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            return ISO_FORMAT.parse(isoDate);
        } catch (ParseException e) {
            return null;
        }
    }
}
