package org.example.algorithms;

import org.example.graph.Edge;
import org.example.graph.Graph;

import java.util.*;

/**
 * Dijkstra
 * --------
 * Classic single-source shortest-path algorithm adapted for two independent
 * weight dimensions: cost (USD) and duration (minutes).
 *
 * Three public façades are exposed:
 *   cheapest(graph, src, dest)        – minimise total cost
 *   fastest (graph, src, dest)        – minimise total duration
 *   withinBudget(graph, src, budget)  – all airports reachable ≤ budget USD
 *
 * Algorithm outline (standard Dijkstra with a min-heap)
 * ------------------------------------------------------
 *   1. Initialise dist[src] = 0, dist[v] = ∞ for all other v.
 *   2. Push (0, src) onto a priority queue ordered by distance.
 *   3. While the queue is not empty:
 *        a. Poll the vertex u with the smallest tentative distance.
 *        b. If u has already been finalised (visited), skip.
 *        c. Mark u as visited.
 *        d. For each edge (u → v, w):
 *             if dist[u] + w < dist[v]: relax, update prev[v] = u, push (dist[v], v).
 *   4. Reconstruct path by back-tracking through prev[].
 *
 * Complexity
 * ----------
 *   Time : O((V + E) log V)  — each vertex / edge processed at most once;
 *                               priority-queue operations cost O(log V).
 *   Space: O(V + E)          — dist[], prev[], visited[] each O(V); PQ ≤ O(E).
 *
 * where V = number of airports, E = number of directed routes.
 */
public class Dijkstra {

    // ------------------------------------------------------------------
    // Result container
    // ------------------------------------------------------------------

    /**
     * Immutable result returned by cheapest() and fastest().
     *
     * Carries:
     *   path        – ordered list of IATA codes from source to destination
     *                 (includes both endpoints); empty when unreachable.
     *   totalWeight – optimal accumulated weight along the path
     *                 (USD for cheapest, minutes for fastest).
     *   reachable   – false iff no path exists.
     */
    public static class Result {

        private final List<String> path;
        private final int          totalWeight;
        private final boolean      reachable;

        private Result(List<String> path, int totalWeight, boolean reachable) {
            this.path        = Collections.unmodifiableList(path);
            this.totalWeight = totalWeight;
            this.reachable   = reachable;
        }

        public List<String> getPath()        { return path; }
        public int          getTotalWeight() { return totalWeight; }
        public boolean      isReachable()    { return reachable; }
    }

    // ------------------------------------------------------------------
    // Public façades
    // ------------------------------------------------------------------

    /**
     * Finds the route from {@code src} to {@code dest} that minimises total
     * cost (USD).
     *
     * Time complexity : O((V + E) log V)
     * Space complexity: O(V + E)
     *
     * @param graph the flight network
     * @param src   IATA code of the origin      (must be in graph)
     * @param dest  IATA code of the destination (must be in graph)
     * @return Result with the cheapest path and its total cost in USD
     */
    public static Result cheapest(Graph graph, String src, String dest) {
        return run(graph, src, dest, false);
    }

    /**
     * Finds the route from {@code src} to {@code dest} that minimises total
     * duration (minutes).
     *
     * Time complexity : O((V + E) log V)
     * Space complexity: O(V + E)
     *
     * @param graph the flight network
     * @param src   IATA code of the origin      (must be in graph)
     * @param dest  IATA code of the destination (must be in graph)
     * @return Result with the fastest path and its total duration in minutes
     */
    public static Result fastest(Graph graph, String src, String dest) {
        return run(graph, src, dest, true);
    }

    /**
     * Returns every airport reachable from {@code src} whose cheapest path
     * cost does not exceed {@code budgetUSD}, mapped to that minimum cost.
     *
     * Internally this is a full Dijkstra from {@code src} stopped early for
     * each vertex once its cost exceeds the budget; the source itself is
     * excluded from the result.
     *
     * Time complexity : O((V + E) log V)  — same as standard Dijkstra
     * Space complexity: O(V)
     *
     * @param graph     the flight network
     * @param src       IATA code of the departure airport
     * @param budgetUSD maximum total fare in USD (inclusive)
     * @return Map from reachable IATA code → minimum cost in USD
     */
    public static Map<String, Integer> withinBudget(Graph graph,
                                                    String src,
                                                    int budgetUSD) {
        Map<String, Integer> dist    = new HashMap<>();
        Set<String>          visited = new HashSet<>();

        // PQ entries: [distance, iata]
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        // We encode the IATA string index separately to avoid storing objects in the PQ
        // Use a small wrapper via Object[] instead
        PriorityQueue<Object[]> queue = new PriorityQueue<>(
                Comparator.comparingInt(a -> (int) a[0]));

        dist.put(src, 0);
        queue.offer(new Object[]{0, src});

        while (!queue.isEmpty()) {
            Object[] top  = queue.poll();
            int    d    = (int)    top[0];
            String u    = (String) top[1];

            if (visited.contains(u)) continue;
            visited.add(u);

            if (d > budgetUSD) continue;   // prune: no need to explore further

            for (Edge edge : graph.getEdges(u)) {
                String v      = edge.getDestination();
                int    newDist = d + edge.getCostUSD();

                if (newDist <= budgetUSD) {
                    int prevDist = dist.getOrDefault(v, Integer.MAX_VALUE);
                    if (newDist < prevDist) {
                        dist.put(v, newDist);
                        queue.offer(new Object[]{newDist, v});
                    }
                }
            }
        }

        // Remove the source itself from results
        dist.remove(src);
        return dist;
    }

    // ------------------------------------------------------------------
    // Core Dijkstra implementation
    // ------------------------------------------------------------------

    /**
     * Runs Dijkstra from {@code src} toward {@code dest} using either the
     * cost or the duration edge weight.
     *
     * @param useDuration true  → weight = durationMin
     *                    false → weight = costUSD
     */
    private static Result run(Graph graph, String src, String dest,
                              boolean useDuration) {

        // Edge-case: same airport
        if (src.equals(dest)) {
            return new Result(List.of(src), 0, true);
        }

        // dist[v] = best known total weight to reach v
        Map<String, Integer> dist    = new HashMap<>();
        // prev[v] = predecessor of v on the best path found so far
        Map<String, String>  prev    = new HashMap<>();
        Set<String>          visited = new HashSet<>();

        // Priority queue: Object[]{ weight, iataCode }
        PriorityQueue<Object[]> pq = new PriorityQueue<>(
                Comparator.comparingInt(a -> (int) a[0]));

        // Initialise
        for (String iata : graph.getAllIataCodes()) {
            dist.put(iata, Integer.MAX_VALUE);
        }
        dist.put(src, 0);
        pq.offer(new Object[]{0, src});

        while (!pq.isEmpty()) {
            Object[] top = pq.poll();
            int    d = (int)    top[0];
            String u = (String) top[1];

            if (visited.contains(u)) continue;
            visited.add(u);

            // Early exit: destination finalised
            if (u.equals(dest)) break;

            for (Edge edge : graph.getEdges(u)) {
                String v      = edge.getDestination();
                int    weight = useDuration ? edge.getDurationMin() : edge.getCostUSD();
                int    distV  = dist.getOrDefault(v, Integer.MAX_VALUE);

                if (d != Integer.MAX_VALUE && d + weight < distV) {
                    dist.put(v, d + weight);
                    prev.put(v, u);
                    pq.offer(new Object[]{d + weight, v});
                }
            }
        }

        // No path found
        int bestWeight = dist.getOrDefault(dest, Integer.MAX_VALUE);
        if (bestWeight == Integer.MAX_VALUE) {
            return new Result(new ArrayList<>(), 0, false);
        }

        // Reconstruct path by walking prev[] backwards
        LinkedList<String> path = new LinkedList<>();
        String cur = dest;
        while (cur != null) {
            path.addFirst(cur);
            cur = prev.get(cur);
        }

        return new Result(new ArrayList<>(path), bestWeight, true);
    }
}
