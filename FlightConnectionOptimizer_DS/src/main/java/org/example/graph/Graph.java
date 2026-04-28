package org.example.graph;

import org.example.model.Airport;

import java.util.*;

/**
 * Graph
 * -----
 * Weighted directed graph backed by adjacency lists, modelling the global
 * flight network.
 *
 * Vertex identity: IATA airport code (String).
 * Edge weights   : cost (USD) and duration (minutes), both stored on each
 *                  {@link Edge} so Dijkstra can run with either weight.
 *
 * Internal representation
 * -----------------------
 *   adjacencyList : HashMap&lt;String, List&lt;Edge&gt;&gt;
 *       key   = source IATA code
 *       value = list of outgoing Edge objects
 *
 *   airports : HashMap&lt;String, Airport&gt;
 *       key   = IATA code
 *       value = Airport metadata (city, country, coordinates)
 *
 * Complexity summary
 * ------------------
 *   addAirport      : O(1) amortised
 *   addEdge         : O(1) amortised
 *   getEdges        : O(1)
 *   getAirport      : O(1)
 *   getAllIataCodes  : O(V)
 *   vertexCount     : O(1)
 *   edgeCount       : O(V)  – sums all adjacency lists
 */
public class Graph {

    // ------------------------------------------------------------------
    // Internal state
    // ------------------------------------------------------------------

    /**
     * Adjacency list: source IATA → list of outgoing edges.
     * A vertex with no outgoing edges still has an entry (empty list) once
     * added via {@link #addAirport(Airport)}.
     */
    private final Map<String, List<Edge>> adjacencyList;

    /**
     * Metadata store: IATA → Airport object.
     * Kept separately so algorithms that only need topology never pay the
     * cost of loading Airport objects.
     */
    private final Map<String, Airport> airports;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    /** Creates an empty graph. */
    public Graph() {
        adjacencyList = new HashMap<>();
        airports      = new HashMap<>();
    }

    // ------------------------------------------------------------------
    // Mutation
    // ------------------------------------------------------------------

    /**
     * Registers an airport as a vertex.  If the IATA code already exists the
     * metadata is updated but the adjacency list is left intact.
     *
     * Time complexity : O(1) amortised
     * Space complexity: O(1) amortised
     *
     * @param airport non-null Airport to add
     */
    public void addAirport(Airport airport) {
        Objects.requireNonNull(airport, "Airport must not be null.");
        airports.put(airport.getIata(), airport);
        adjacencyList.putIfAbsent(airport.getIata(), new ArrayList<>());
    }

    /**
     * Adds a directed edge from {@code sourceIata} to {@code edge.destination}.
     *
     * If the source IATA is not yet a registered vertex a bare entry is created
     * in the adjacency list (without Airport metadata).  Callers should always
     * call {@link #addAirport(Airport)} first to ensure metadata is present.
     *
     * Duplicate edges (same source and destination) are allowed — they model
     * codeshare or multiple daily services with different fares.
     *
     * Time complexity : O(1) amortised
     * Space complexity: O(1) amortised
     *
     * @param sourceIata IATA code of the origin airport (non-null)
     * @param edge       outgoing Edge to add (non-null)
     */
    public void addEdge(String sourceIata, Edge edge) {
        Objects.requireNonNull(sourceIata, "sourceIata must not be null.");
        Objects.requireNonNull(edge,       "edge must not be null.");
        adjacencyList.computeIfAbsent(sourceIata, k -> new ArrayList<>()).add(edge);
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    /**
     * Returns whether the given IATA code is a registered vertex.
     *
     * Time complexity: O(1)
     *
     * @param iata IATA code to check
     * @return true iff the airport is in the graph
     */
    public boolean containsAirport(String iata) {
        return adjacencyList.containsKey(iata);
    }

    /**
     * Returns the {@link Airport} metadata for the given IATA code, or
     * {@code null} if the code is not registered.
     *
     * Time complexity: O(1)
     *
     * @param iata IATA airport code
     * @return Airport or null
     */
    public Airport getAirport(String iata) {
        return airports.get(iata);
    }

    /**
     * Returns an unmodifiable view of the outgoing edges from the given
     * airport, or an empty list if the airport has no outgoing edges or is
     * not in the graph.
     *
     * Time complexity: O(1)
     *
     * @param iata source IATA code
     * @return unmodifiable list of outgoing {@link Edge} objects
     */
    public List<Edge> getEdges(String iata) {
        List<Edge> edges = adjacencyList.get(iata);
        return (edges == null)
                ? Collections.emptyList()
                : Collections.unmodifiableList(edges);
    }

    /**
     * Returns the set of all registered IATA codes (vertices).
     *
     * Time complexity : O(V)
     * Space complexity: O(V)
     *
     * @return unmodifiable set of IATA codes
     */
    public Set<String> getAllIataCodes() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    /**
     * Returns the unmodifiable airport metadata map.
     * Useful for algorithms that need to iterate over all Airport objects.
     *
     * Time complexity : O(1) — returns the backing map view directly
     *
     * @return unmodifiable Map from IATA to Airport
     */
    public Map<String, Airport> getAirports() {
        return Collections.unmodifiableMap(airports);
    }

    /**
     * Number of vertices (registered airports) in the graph.
     *
     * Time complexity: O(1)
     */
    public int vertexCount() {
        return adjacencyList.size();
    }

    /**
     * Total number of directed edges across all adjacency lists.
     *
     * Time complexity: O(V)  — must iterate over every adjacency list
     */
    public int edgeCount() {
        int total = 0;
        for (List<Edge> list : adjacencyList.values()) {
            total += list.size();
        }
        return total;
    }

    // ------------------------------------------------------------------
    // Diagnostics
    // ------------------------------------------------------------------

    /**
     * Returns a multi-line string listing every vertex and its outgoing edges.
     * Intended for debugging small graphs; may be very large for the full
     * OpenFlights dataset.
     *
     * Time complexity: O(V + E)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Graph: %d vertices, %d edges%n",
                vertexCount(), edgeCount()));

        // Sort for deterministic output
        List<String> sorted = new ArrayList<>(adjacencyList.keySet());
        Collections.sort(sorted);

        for (String iata : sorted) {
            Airport ap = airports.get(iata);
            String label = (ap != null)
                    ? ap.toString()
                    : iata + " (no metadata)";
            sb.append(label).append("\n");
            for (Edge e : adjacencyList.get(iata)) {
                sb.append("    ").append(e).append("\n");
            }
        }
        return sb.toString();
    }
}
