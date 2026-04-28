package org.example.utils;

/**
 * RouteEnricher
 *
 * Derives realistic cost (USD) and duration (minutes) for a flight route
 * purely from the origin/destination coordinates.
 *
 * Pipeline:
 *   lat/lon  →  Haversine distance (km)  →  cost (USD)  →  duration (min)
 *
 * Complexity: O(1) per call – all operations are constant-time arithmetic.
 */
public class RouteEnricher {

    // ---------------------------------------------------------------
    // Physical / aviation constants
    // ---------------------------------------------------------------

    /** Mean radius of the Earth in kilometres (WGS-84 approximation). */
    private static final double EARTH_RADIUS_KM = 6_371.0;

    /**
     * Average cruising speed of a commercial jet in km/h.
     * (Typical narrow-body cruise: 850-900 km/h; we use 880 as a midpoint.)
     */
    private static final double AVG_CRUISE_SPEED_KMH = 880.0;

    /**
     * Fixed airport overhead added to every flight (boarding, taxi, approach).
     * Real-world average door-to-door overhead ≈ 60 min each end → 90 min total
     * is a conservative but realistic figure.
     */
    private static final double AIRPORT_OVERHEAD_MIN = 90.0;

    // ---------------------------------------------------------------
    // Pricing model  (inspired by IATA fare construction principles)
    // ---------------------------------------------------------------

    /** Flat booking/airport fee applied to every ticket regardless of distance. */
    private static final double BASE_FEE_USD = 35.0;

    /*
     * Tiered per-km rates (USD/km).
     * Short-haul flights cost more per km than long-haul due to fixed overheads
     * being amortised over fewer kilometres.
     *
     *  ≤  500 km  →  short-haul   (e.g. intra-Europe, domestic hops)
     *  ≤ 3000 km  →  medium-haul
     *  >  3000 km →  long-haul / ultra-long-haul
     */
    private static final double RATE_SHORT_HAUL  = 0.18;   // USD per km
    private static final double RATE_MEDIUM_HAUL = 0.12;
    private static final double RATE_LONG_HAUL   = 0.08;

    /** Minimum ticket price – no route should cost less than this (fuel + fees). */
    private static final double MIN_COST_USD = 49.0;

    /** Minimum flight duration in minutes (shortest scheduled commercial segments). */
    private static final double MIN_DURATION_MIN = 25.0;

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Calculates the great-circle distance between two coordinates using
     * the Haversine formula.
     *
     * Formula:
     *   a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlon/2)
     *   c = 2·atan2(√a, √(1−a))
     *   d = R·c
     *
     * Time complexity : O(1)
     * Space complexity: O(1)
     *
     * @param lat1 Latitude  of origin      (decimal degrees)
     * @param lon1 Longitude of origin      (decimal degrees)
     * @param lat2 Latitude  of destination (decimal degrees)
     * @param lon2 Longitude of destination (decimal degrees)
     * @return distance in kilometres (always ≥ 0)
     */
    public static double haversineKm(double lat1, double lon1,
                                     double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Estimates a one-way economy ticket price in USD for a route of the
     * given distance, using a tiered per-km pricing model.
     *
     * Pricing tiers:
     *   ≤  500 km  : BASE_FEE + dist × RATE_SHORT_HAUL
     *   ≤ 3000 km  : BASE_FEE + dist × RATE_MEDIUM_HAUL
     *   >  3000 km : BASE_FEE + dist × RATE_LONG_HAUL
     *
     * Result is rounded to the nearest dollar and floored at MIN_COST_USD.
     *
     * Time complexity : O(1)
     * Space complexity: O(1)
     *
     * @param distanceKm great-circle distance in kilometres (must be ≥ 0)
     * @return estimated ticket cost in USD (integer, ≥ MIN_COST_USD)
     */
    public static int estimateCostUSD(double distanceKm) {
        double rate;
        if (distanceKm <= 500) {
            rate = RATE_SHORT_HAUL;
        } else if (distanceKm <= 3_000) {
            rate = RATE_MEDIUM_HAUL;
        } else {
            rate = RATE_LONG_HAUL;
        }

        double raw = BASE_FEE_USD + distanceKm * rate;
        int cost = (int) Math.round(raw);
        return Math.max(cost, (int) MIN_COST_USD);
    }

    /**
     * Estimates total door-to-door flight duration in minutes for a route of
     * the given distance.
     *
     * Formula:
     *   duration = (distanceKm / AVG_CRUISE_SPEED_KMH) × 60  +  AIRPORT_OVERHEAD_MIN
     *
     * Result is rounded to the nearest minute and floored at MIN_DURATION_MIN.
     *
     * Time complexity : O(1)
     * Space complexity: O(1)
     *
     * @param distanceKm great-circle distance in kilometres (must be ≥ 0)
     * @return estimated total duration in minutes (integer, ≥ MIN_DURATION_MIN)
     */
    public static int estimateDurationMin(double distanceKm) {
        double flightMinutes = (distanceKm / AVG_CRUISE_SPEED_KMH) * 60.0;
        double total = flightMinutes + AIRPORT_OVERHEAD_MIN;
        int duration = (int) Math.round(total);
        return Math.max(duration, (int) MIN_DURATION_MIN);
    }

    /**
     * Convenience method: given four coordinates, returns a two-element array
     * [costUSD, durationMin].  Useful when the caller needs both values from
     * a single call without storing the intermediate distance.
     *
     * Time complexity : O(1)
     * Space complexity: O(1)
     *
     * @param lat1 Latitude  of origin      (decimal degrees)
     * @param lon1 Longitude of origin      (decimal degrees)
     * @param lat2 Latitude  of destination (decimal degrees)
     * @param lon2 Longitude of destination (decimal degrees)
     * @return int[] { costUSD, durationMin }
     */
    public static int[] computeCostAndDuration(double lat1, double lon1,
                                               double lat2, double lon2) {
        double km = haversineKm(lat1, lon1, lat2, lon2);
        return new int[]{ estimateCostUSD(km), estimateDurationMin(km) };
    }
}
