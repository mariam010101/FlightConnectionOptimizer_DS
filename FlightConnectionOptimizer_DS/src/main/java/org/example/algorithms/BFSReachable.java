package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.*;

/**
 * BFSReachable
 * ------------
 * Breadth-First Search over the directed flight graph that returns every
 * airport reachable from a source within at most K connections (hops).
 *
 * "K connections" means K edges traversed, i.e. K flights taken.
 * The source airport itself is NOT included in the result.
 *
 * Algorithm outline
 * -----------------
 *   1. Enqueue (src, depth=0).
 *   2. While queue is not empty and current depth ≤ K:
 *        a. Dequeue (u, depth).
 *        b. For each outgoing edge u → v:
 *             if v not yet visited:
 *               mark visited with hop-count = depth + 1
 *               enqueue (v, depth + 1)  if  depth + 1 ≤ K
 *
 * Because BFS explores layer by layer, the first time a vertex is reached
 * gives the minimum number of hops — which is exactly what we want to report.
 *
 * Complexity
 * ----------
 *   Time : O(V + E)  — each vertex and edge visited at most once.
 *   Space: O(V)      — visited map + queue, both bounded by number of vertices.
 *
 * where V = airports, E = directed routes.
 */
public class BFSReachable {

    /**
     * Returns all airports reachable from {@code src} using at most
     * {@code maxHops} direct flights, mapped to the minimum number of hops
     * needed to reach them.
     *
     * Edge cases handled:
     *   - {@code src} not in graph  → returns empty map
     *   - {@code maxHops} == 0      → returns empty map (no flights taken)
     *   - isolated airport          → returns empty map
     *
     * Time complexity : O(V + E)
     * Space complexity: O(V)
     *
     * @param graph   the flight network
     * @param src     IATA code of the departure airport
     * @param maxHops maximum number of connections allowed (≥ 0)
     * @return Map from reachable IATA code → minimum hop count to reach it
     *         (source itself excluded)
     */
    public static Map<String, Integer> within(Graph graph,
                                              String src,
                                              int maxHops) {

        // Result: IATA → minimum hops required
        Map<String, Integer> reachable = new LinkedHashMap<>();

        if (!graph.containsAirport(src) || maxHops <= 0) {
            return reachable;
        }

        // BFS queue stores Object[]{ iataCode, currentDepth }
        Queue<Object[]> queue   = new ArrayDeque<>();
        Set<String>     visited = new HashSet<>();

        visited.add(src);
        queue.offer(new Object[]{src, 0});

        while (!queue.isEmpty()) {
            Object[] entry = queue.poll();
            String   u     = (String) entry[0];
            int      depth = (int)    entry[1];

            // Explore neighbours only if we haven't hit the hop limit
            if (depth >= maxHops) continue;

            for (Edge edge : graph.getEdges(u)) {
                String v = edge.getDestination();

                if (!visited.contains(v)) {
                    visited.add(v);
                    int hops = depth + 1;
                    reachable.put(v, hops);

                    // Only enqueue for further exploration if we can still go deeper
                    if (hops < maxHops) {
                        queue.offer(new Object[]{v, hops});
                    }
                }
            }
        }

        return reachable;
    }
}
