package org.example.model;

import java.util.Objects;

/**
 * Airport
 * -------
 * Immutable value object representing one vertex in the flight network graph.
 *
 * The IATA code is the natural primary key — it is used as the vertex
 * identifier inside {@link org.example.graph.Graph} and as the map key
 * inside {@link org.example.utils.CSVLoader}.
 *
 * Coordinates (WGS-84 decimal degrees) are stored here so that
 * {@link org.example.utils.RouteEnricher} can compute Haversine distances
 * without any extra look-ups.
 *
 * Complexity
 *   All methods: O(1) time and space.
 */
public class Airport {

    /** Three-letter IATA airport code, e.g. "KEF", "LHR". */
    private final String iata;

    /** City served by the airport. */
    private final String city;

    /** Country. */
    private final String country;

    /** WGS-84 latitude in decimal degrees (negative = south). */
    private final double latitude;

    /** WGS-84 longitude in decimal degrees (negative = west). */
    private final double longitude;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public Airport(String iata, String city, String country,
                   double latitude, double longitude) {
        this.iata      = iata;
        this.city      = city;
        this.country   = country;
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public String getIata()      { return iata; }
    public String getCity()      { return city; }
    public String getCountry()   { return country; }
    public double getLatitude()  { return latitude; }
    public double getLongitude() { return longitude; }

    // ------------------------------------------------------------------
    // Object overrides
    // ------------------------------------------------------------------

    /**
     * Two airports are equal iff they share the same IATA code.
     * Allows Airport to be used safely as a Map / Set key.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Airport)) return false;
        Airport other = (Airport) o;
        return Objects.equals(iata, other.iata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iata);
    }

    /** Example: "KEF (Keflavik, Iceland)" */
    @Override
    public String toString() {
        return String.format("%s (%s, %s)", iata, city, country);
    }
}
