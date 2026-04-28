package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.*;

/**
 * MSTKruskal
 * ----------
 * Computes the Minimum Spanning Tree (MST) of the flight network using
 * Kruskal's algorithm with a Union-Find (Disjoint Set Union) data structure.
 *
 * Weight used: costUSD (cheapest network backbone).
 *
 * Directed → undirected conversion
 * ---------------------------------
 * MST is defined for undirected graphs.  We treat each directed edge u→v as
 * an undirected edge {u, v} with the same cost weight, then deduplicate by
 * keeping only the cheaper of the two directions if both exist.
 *
 * Algorithm outline (Kruskal 1956)
 * ---------------------------------
 *   1. Collect all undirected edges and sort by cost ascending.   O(E log E)
 *   2. Initialise Union-Find: each vertex is its own component.   O(V)
 *   3. For each edge (u, v, w) in sorted order:                   O(E · α(V))
 *        if find(u) ≠ find(v):   — u and v are in different components
 *          add edge to MST
 *          union(u, v)
 *        stop when MST has V-1 edges.
 *
 * Union-Find details
 * ------------------
 *   - Path compression in find()  → amortised O(α(V)) ≈ O(1)
 *   - Union by rank               → keeps tree flat
 *
 * Note on disconnected graphs
 * ---------------------------
 * If the graph is not fully connected, Kruskal produces a Minimum Spanning
 * Forest (one tree per connected component).  The result list will contain
 * fewer than V-1 edges.  Main.java handles this gracefully.
 *
 * Complexity
 * ----------
 *   Time : O(E log E)  dominated by the sort step.
 *   Space: O(V + E)    Union-Find arrays + edge list.
 */
public class MSTKruskal {

    // ------------------------------------------------------------------
    // Result type
    // ------------------------------------------------------------------

    /**
     * One edge in the MST result.
     * Both endpoints are stored for display purposes.
     */
    public static class MSTEdge {
        private final String src;
        private final String dest;
        private final int    costUSD;

        public MSTEdge(String src, String dest, int costUSD) {
            this.src     = src;
            this.dest    = dest;
            this.costUSD = costUSD;
        }

        public String getSrc()     { return src; }
        public String getDest()    { return dest; }
        public int    getCostUSD() { return costUSD; }

        @Override
        public String toString() {
            return String.format("%s — %s  $%,d", src, dest, costUSD);
        }
    }

    // ------------------------------------------------------------------
    // Public façade
    // ------------------------------------------------------------------

    /**
     * Computes the MST (or MSF if the graph is disconnected) of the flight
     * network, using edge cost as the weight.
     *
     * Time complexity : O(E log E)
     * Space complexity: O(V + E)
     *
     * @param graph the flight network
     * @return list of MST edges sorted by cost ascending; empty if ≤ 1 vertex
     */
    public static List<MSTEdge> compute(Graph graph) {

        // ---- Step 1: collect undirected edges (deduplicated) ----------
        // Key = "A:B" where A < B lexicographically — ensures uniqueness
        Map<String, MSTEdge> edgeMap = new HashMap<>();

        for (String u : graph.getAllIataCodes()) {
            for (Edge edge : graph.getEdges(u)) {
                String v    = edge.getDestination();
                int    cost = edge.getCostUSD();

                // Canonical key: smaller IATA first
                String key = (u.compareTo(v) <= 0)
                        ? u + ":" + v
                        : v + ":" + u;

                // Keep the cheaper direction if both exist
                MSTEdge existing = edgeMap.get(key);
                if (existing == null || cost < existing.getCostUSD()) {
                    String lo = (u.compareTo(v) <= 0) ? u : v;
                    String hi = (u.compareTo(v) <= 0) ? v : u;
                    edgeMap.put(key, new MSTEdge(lo, hi, cost));
                }
            }
        }

        // Sort all edges by cost ascending
        List<MSTEdge> edges = new ArrayList<>(edgeMap.values());
        edges.sort(Comparator.comparingInt(MSTEdge::getCostUSD));

        // ---- Step 2: Union-Find initialisation -----------------------
        Set<String>         vertices = graph.getAllIataCodes();
        Map<String, String> parent   = new HashMap<>();   // Union-Find parent
        Map<String, Integer> rank    = new HashMap<>();   // Union-Find rank

        for (String v : vertices) {
            parent.put(v, v);
            rank.put(v, 0);
        }

        // ---- Step 3: Kruskal main loop --------------------------------
        List<MSTEdge> mst        = new ArrayList<>();
        int           targetSize = vertices.size() - 1;   // V-1 edges for a spanning tree

        for (MSTEdge e : edges) {
            if (mst.size() == targetSize) break;   // MST complete

            String rootU = find(parent, e.getSrc());
            String rootV = find(parent, e.getDest());

            if (!rootU.equals(rootV)) {
                mst.add(e);
                union(parent, rank, rootU, rootV);
            }
        }

        return mst;
    }

    // ------------------------------------------------------------------
    // Union-Find helpers
    // ------------------------------------------------------------------

    /**
     * Find with path compression.
     * Amortised time: O(α(V)) ≈ O(1).
     */
    private static String find(Map<String, String> parent, String x) {
        if (!parent.get(x).equals(x)) {
            parent.put(x, find(parent, parent.get(x)));   // path compression
        }
        return parent.get(x);
    }

    /**
     * Union by rank.
     * Keeps the Union-Find tree shallow to maintain near-constant find().
     */
    private static void union(Map<String, String> parent,
                              Map<String, Integer> rank,
                              String x, String y) {
        int rankX = rank.get(x);
        int rankY = rank.get(y);

        if (rankX < rankY) {
            parent.put(x, y);
        } else if (rankX > rankY) {
            parent.put(y, x);
        } else {
            parent.put(y, x);
            rank.put(x, rankX + 1);
        }
    }
}
