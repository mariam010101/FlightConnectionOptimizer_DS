package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.*;

/**
 * ArticulationPoints
 * ------------------
 * Identifies "critical" airports: vertices whose removal would disconnect
 * (or reduce connectivity in) the flight network.
 *
 * Adaptation note
 * ---------------
 * The classic articulation-point algorithm operates on *undirected* graphs.
 * The flight graph is *directed*, so we convert it to an undirected view
 * on-the-fly by treating every directed edge u→v as a bidirectional link
 * {u, v}.  This mirrors the real-world question: "if this airport closes,
 * are some cities cut off from others?"  A directed articulation point
 * (sometimes called a "cut vertex in the underlying undirected graph") is the
 * most natural and practical definition for this use case.
 *
 * Algorithm — Tarjan's DFS (1972)
 * --------------------------------
 * Two arrays are maintained:
 *   disc[u]  – discovery time of u in the DFS tree (timestamp order)
 *   low[u]   – lowest disc[] value reachable from u's subtree via any back-edge
 *
 * Articulation-point conditions (for a DFS-tree vertex u):
 *   A) u is the DFS root and has ≥ 2 children in the DFS tree.
 *   B) u is NOT the root and has a child v where low[v] ≥ disc[u]
 *      (v cannot "jump over" u using a back-edge).
 *
 * Complexity
 * ----------
 *   Time : O(V + E)  — single DFS pass over the undirected view.
 *   Space: O(V)      — disc[], low[], visited[], parent[], recursion stack.
 *
 * where V = airports, E = directed routes (each treated as undirected).
 */
public class ArticulationPoints {

    // ------------------------------------------------------------------
    // DFS state (instance fields so recursive helper stays clean)
    // ------------------------------------------------------------------

    private Map<String, Integer> disc;          // discovery time per vertex
    private Map<String, Integer> low;           // low-link value per vertex
    private Map<String, String>  parent;        // DFS-tree parent
    private Set<String>          visited;
    private Set<String>          result;        // articulation points found
    private int                  timer;         // global DFS timestamp
    private Map<String, Set<String>> undirected; // undirected adjacency view

    // ------------------------------------------------------------------
    // Public façade
    // ------------------------------------------------------------------

    /**
     * Finds all articulation points in the undirected view of {@code graph}.
     *
     * The method runs one DFS per connected component so disconnected graphs
     * are handled correctly.
     *
     * Time complexity : O(V + E)
     * Space complexity: O(V + E)  — undirected adjacency view costs O(V + E)
     *
     * @param graph the flight network
     * @return Set of IATA codes that are articulation points (may be empty)
     */
    public static Set<String> find(Graph graph) {
        ArticulationPoints ap = new ArticulationPoints();
        return ap.compute(graph);
    }

    // ------------------------------------------------------------------
    // Implementation
    // ------------------------------------------------------------------

    private Set<String> compute(Graph graph) {
        disc       = new HashMap<>();
        low        = new HashMap<>();
        parent     = new HashMap<>();
        visited    = new HashSet<>();
        result     = new HashSet<>();
        timer      = 0;
        undirected = buildUndirected(graph);

        // Run DFS from every unvisited vertex (handles disconnected graphs)
        for (String iata : graph.getAllIataCodes()) {
            if (!visited.contains(iata)) {
                dfs(iata, null);
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Recursive DFS that fills disc[], low[], and populates {@code result}.
     *
     * @param u      current vertex
     * @param parent IATA of the DFS-tree parent, or null if u is a root
     */
    private void dfs(String u, String parentNode) {
        visited.add(u);
        disc.put(u, timer);
        low.put(u, timer);
        timer++;

        int childCount = 0;   // number of DFS-tree children (root check only)

        for (String v : undirected.getOrDefault(u, Collections.emptySet())) {
            if (!visited.contains(v)) {
                // Tree edge: v is a new DFS-tree child of u
                childCount++;
                parent.put(v, u);
                dfs(v, u);

                // After returning, update low[u] with what v can reach
                low.put(u, Math.min(low.get(u), low.get(v)));

                // Condition B: u is not root, v cannot escape u's subtree
                if (parentNode != null && low.get(v) >= disc.get(u)) {
                    result.add(u);
                }

            } else if (!v.equals(parentNode)) {
                // Back edge: v is already visited and is not u's parent
                // Update low[u] — v is reachable via a back-edge
                low.put(u, Math.min(low.get(u), disc.get(v)));
            }
        }

        // Condition A: u is the DFS root and has ≥ 2 subtrees
        if (parentNode == null && childCount >= 2) {
            result.add(u);
        }
    }

    /**
     * Builds an undirected adjacency view from the directed graph.
     * For each directed edge u → v, adds both u→v and v→u to the view.
     *
     * Time complexity : O(V + E)
     * Space complexity: O(V + E)
     */
    private static Map<String, Set<String>> buildUndirected(Graph graph) {
        Map<String, Set<String>> adj = new HashMap<>();

        for (String u : graph.getAllIataCodes()) {
            adj.computeIfAbsent(u, k -> new HashSet<>());
            for (Edge edge : graph.getEdges(u)) {
                String v = edge.getDestination();
                adj.computeIfAbsent(u, k -> new HashSet<>()).add(v);
                adj.computeIfAbsent(v, k -> new HashSet<>()).add(u);
            }
        }

        return adj;
    }
}
