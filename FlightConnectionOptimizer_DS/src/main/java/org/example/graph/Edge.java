package org.example.graph;

/**
 * Edge
 * ----
 * A directed, dual-weighted edge in the flight network graph.
 *
 * Each edge represents a non-stop flight from one airport (identified by its
 * IATA code) to another.  Two independent weights are stored:
 *
 *   costUSD     – estimated economy fare in US dollars
 *   durationMin – estimated door-to-door journey time in minutes
 *
 * Both weights are computed at load-time by
 * {@link org.example.utils.RouteEnricher} using the Haversine formula applied
 * to the endpoints' coordinates.  Neither weight is stored in the CSV files.
 *
 * Why two weights?
 *   Dijkstra is run twice — once minimising cost, once minimising duration —
 *   so the graph needs both values on every edge.  Storing them together avoids
 *   a second edge list or parallel arrays.
 *
 * Complexity
 *   Constructor / all getters: O(1) time, O(1) space.
 */
public class Edge {

    /** IATA code of the destination airport (tail of the directed edge). */
    private final String destination;

    /**
     * Estimated one-way economy fare in USD.
     * Computed as: BASE_FEE + distanceKm × tiered_rate  (see RouteEnricher).
     * Minimum value: 49 USD.
     */
    private final int costUSD;

    /**
     * Estimated total door-to-door duration in minutes.
     * Computed as: (distanceKm / 880 km·h⁻¹) × 60 + 90 min overhead.
     * Minimum value: 25 min.
     */
    private final int durationMin;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    /**
     * Creates a directed edge from the implicit source vertex to
     * {@code destination}.
     *
     * @param destination IATA code of the destination airport (non-null)
     * @param costUSD     estimated fare in USD (must be &gt; 0)
     * @param durationMin estimated duration in minutes (must be &gt; 0)
     */
    public Edge(String destination, int costUSD, int durationMin) {
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Edge destination must not be blank.");
        }
        if (costUSD <= 0 || durationMin <= 0) {
            throw new IllegalArgumentException(
                    "Edge weights must be positive: costUSD=" + costUSD
                            + ", durationMin=" + durationMin);
        }
        this.destination = destination;
        this.costUSD     = costUSD;
        this.durationMin = durationMin;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    /** @return IATA code of the destination airport */
    public String getDestination() { return destination; }

    /** @return estimated fare in USD */
    public int getCostUSD()        { return costUSD; }

    /** @return estimated flight + overhead duration in minutes */
    public int getDurationMin()    { return durationMin; }

    // ------------------------------------------------------------------
    // Object overrides
    // ------------------------------------------------------------------

    /**
     * Returns a compact representation useful for debugging and CLI output.
     * Example: "→ LHR | $350 | 135 min"
     */
    @Override
    public String toString() {
        return String.format("→ %-4s | $%-5d | %d min",
                destination, costUSD, durationMin);
    }
}
