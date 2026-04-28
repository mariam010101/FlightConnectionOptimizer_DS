package org.example;

import org.example.algorithms.ArticulationPoints;
import org.example.algorithms.BFSReachable;
import org.example.algorithms.Dijkstra;
import org.example.algorithms.MSTKruskal;
import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.model.Airport;
import org.example.utils.CSVLoader;

import java.io.IOException;
import java.util.*;

/**
 * Main
 * ----
 * Command-line entry point for the Flight Connection Optimizer.
 *
 * Usage
 * -----
 *   java -jar FlightConnectionOptimizer.jar [airports.csv] [routes.csv]
 *
 * If no arguments are provided the application looks for the data files at
 * the default relative paths  data/airports.csv  and  data/routes.csv.
 *
 * Interactive menu
 * ----------------
 *   [1] Cheapest route (Dijkstra on cost)
 *   [2] Fastest route  (Dijkstra on duration)
 *   [3] Reachable airports within K connections (BFS)
 *   [4] Critical airports (Articulation Points)
 *   [5] Minimum Spanning Tree (Kruskal)
 *   [6] Travel-budget mode (all destinations within $X)
 *   [7] Airport info lookup
 *   [8] Graph statistics
 *   [0] Exit
 *
 * All algorithm classes are called through their static façade methods;
 * Main itself contains only I/O and menu logic.
 */
public class MainCLI {

    // ------------------------------------------------------------------
    // Default file locations (overridden by CLI args)
    // ------------------------------------------------------------------

    private static final String DEFAULT_AIRPORTS = "data/airports.csv";
    private static final String DEFAULT_ROUTES   = "data/routes.csv";

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {

        String airportsFile = (args.length >= 1) ? args[0] : DEFAULT_AIRPORTS;
        String routesFile   = (args.length >= 2) ? args[1] : DEFAULT_ROUTES;

        // ---- Load data -----------------------------------------------
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║    Flight Connection Optimizer  v1.0     ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.printf("%nLoading airports from : %s%n", airportsFile);
        System.out.printf("Loading routes   from : %s%n%n", routesFile);

        Graph graph;
        try {
            graph = CSVLoader.loadGraph(airportsFile, routesFile);
        } catch (IOException e) {
            System.err.println("ERROR: Could not load data files.");
            System.err.println(e.getMessage());
            System.err.println("Make sure airports.csv and routes.csv exist at the paths above.");
            return;
        }

        System.out.printf("Graph loaded: %,d airports,  %,d routes%n%n",
                graph.vertexCount(), graph.edgeCount());

        // ---- REPL menu -----------------------------------------------
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();

            switch (choice) {

                // --------------------------------------------------------
                // 1 – Cheapest route (Dijkstra / cost)
                // --------------------------------------------------------
                case "1": {
                    String src  = promptIata(sc, graph, "Origin airport IATA");
                    if (src == null) break;
                    String dest = promptIata(sc, graph, "Destination airport IATA");
                    if (dest == null) break;

                    if (src.equals(dest)) {
                        System.out.println("  Origin and destination are the same airport.");
                        break;
                    }

                    Dijkstra.Result result = Dijkstra.cheapest(graph, src, dest);
                    printDijkstraResult(graph, result, src, dest, "CHEAPEST", "cost", "$");
                    break;
                }

                // --------------------------------------------------------
                // 2 – Fastest route (Dijkstra / duration)
                // --------------------------------------------------------
                case "2": {
                    String src  = promptIata(sc, graph, "Origin airport IATA");
                    if (src == null) break;
                    String dest = promptIata(sc, graph, "Destination airport IATA");
                    if (dest == null) break;

                    if (src.equals(dest)) {
                        System.out.println("  Origin and destination are the same airport.");
                        break;
                    }

                    Dijkstra.Result result = Dijkstra.fastest(graph, src, dest);
                    printDijkstraResult(graph, result, src, dest, "FASTEST", "duration", "min ");
                    break;
                }

                // --------------------------------------------------------
                // 3 – BFS reachability within K connections
                // --------------------------------------------------------
                case "3": {
                    String src = promptIata(sc, graph, "Departure airport IATA");
                    if (src == null) break;

                    int k = promptInt(sc, "Maximum connections (hops)", 1, 20);
                    if (k < 0) break;

                    Map<String, Integer> reachable = BFSReachable.within(graph, src, k);

                    System.out.printf("%n  Airports reachable from %s within %d connection(s):%n%n",
                            src, k);

                    if (reachable.isEmpty()) {
                        System.out.println("  No airports reachable with that constraint.");
                    } else {
                        // Group by hop count for readability
                        Map<Integer, List<String>> byHop = new TreeMap<>();
                        for (Map.Entry<String, Integer> e : reachable.entrySet()) {
                            byHop.computeIfAbsent(e.getValue(), h -> new ArrayList<>())
                                    .add(e.getKey());
                        }
                        for (Map.Entry<Integer, List<String>> entry : byHop.entrySet()) {
                            System.out.printf("  ── %d hop(s) ──%n", entry.getKey());
                            List<String> codes = entry.getValue();
                            Collections.sort(codes);
                            for (String code : codes) {
                                Airport ap = graph.getAirport(code);
                                String label = (ap != null) ? ap.toString() : code;
                                System.out.printf("     %-4s  %s%n", code, label);
                            }
                        }
                        System.out.printf("%n  Total: %d airports%n", reachable.size());
                    }
                    break;
                }

                // --------------------------------------------------------
                // 4 – Articulation points (critical airports)
                // --------------------------------------------------------
                case "4": {
                    System.out.println("\n  Computing critical airports (articulation points)...");
                    Set<String> critical = ArticulationPoints.find(graph);

                    if (critical.isEmpty()) {
                        System.out.println("  No articulation points found in the loaded graph.");
                    } else {
                        System.out.printf("  %d critical airport(s) found:%n%n", critical.size());
                        List<String> sorted = new ArrayList<>(critical);
                        Collections.sort(sorted);
                        for (String code : sorted) {
                            Airport ap = graph.getAirport(code);
                            String label = (ap != null) ? ap.toString() : code;
                            System.out.printf("  ★  %s%n", label);
                        }
                    }
                    break;
                }

                // --------------------------------------------------------
                // 5 – Minimum Spanning Tree (Kruskal)
                // --------------------------------------------------------
                case "5": {
                    System.out.println("\n  Computing Minimum Spanning Tree (Kruskal / cost)...");
                    List<MSTKruskal.MSTEdge> mst = MSTKruskal.compute(graph);

                    if (mst.isEmpty()) {
                        System.out.println("  MST could not be computed (graph may be disconnected).");
                    } else {
                        int totalCost = mst.stream().mapToInt(MSTKruskal.MSTEdge::getCostUSD).sum();
                        System.out.printf("  MST contains %d essential routes, total cost $%,d%n%n",
                                mst.size(), totalCost);
                        int idx = 1;
                        for (MSTKruskal.MSTEdge e : mst) {
                            System.out.printf("  %3d. %-4s → %-4s   $%,d%n",
                                    idx++, e.getSrc(), e.getDest(), e.getCostUSD());
                        }
                    }
                    break;
                }

                // --------------------------------------------------------
                // 6 – Travel-budget mode
                // --------------------------------------------------------
                case "6": {
                    String src = promptIata(sc, graph, "Departure airport IATA");
                    if (src == null) break;

                    int budget = promptInt(sc, "Maximum total budget (USD)", 1, 99_999);
                    if (budget < 0) break;

                    Map<String, Integer> reachable = Dijkstra.withinBudget(graph, src, budget);

                    System.out.printf("%n  Destinations reachable from %s with budget $%,d:%n%n",
                            src, budget);

                    if (reachable.isEmpty()) {
                        System.out.println("  No destinations reachable within that budget.");
                    } else {
                        // Sort by cost ascending
                        List<Map.Entry<String, Integer>> entries = new ArrayList<>(reachable.entrySet());
                        entries.sort(Map.Entry.comparingByValue());

                        for (Map.Entry<String, Integer> entry : entries) {
                            Airport ap = graph.getAirport(entry.getKey());
                            String label = (ap != null)
                                    ? String.format("%-30s", ap.getCity() + ", " + ap.getCountry())
                                    : String.format("%-30s", entry.getKey());
                            System.out.printf("  %-4s  %s  $%,d%n",
                                    entry.getKey(), label, entry.getValue());
                        }
                        System.out.printf("%n  Total: %d destinations%n", reachable.size());
                    }
                    break;
                }

                // --------------------------------------------------------
                // 7 – Airport info lookup
                // --------------------------------------------------------
                case "7": {
                    System.out.print("  Enter IATA code: ");
                    String code = sc.nextLine().trim().toUpperCase();
                    Airport ap = graph.getAirport(code);
                    if (ap == null) {
                        System.out.printf("  Airport '%s' not found in dataset.%n", code);
                    } else {
                        System.out.println();
                        System.out.printf("  IATA    : %s%n",  ap.getIata());
                        System.out.printf("  City    : %s%n",  ap.getCity());
                        System.out.printf("  Country : %s%n",  ap.getCountry());
                        System.out.printf("  Lat/Lon : %.6f, %.6f%n",
                                ap.getLatitude(), ap.getLongitude());
                        System.out.printf("  Routes  : %d outgoing%n",
                                graph.getEdges(code).size());
                    }
                    break;
                }

                // --------------------------------------------------------
                // 8 – Graph statistics
                // --------------------------------------------------------
                case "8": {
                    System.out.println();
                    System.out.printf("  Vertices (airports) : %,d%n", graph.vertexCount());
                    System.out.printf("  Edges    (routes)   : %,d%n", graph.edgeCount());

                    // Top-5 most connected airports (highest out-degree)
                    List<Map.Entry<String, Integer>> degrees = new ArrayList<>();
                    for (String iata : graph.getAllIataCodes()) {
                        degrees.add(new AbstractMap.SimpleEntry<>(iata, graph.getEdges(iata).size()));
                    }
                    degrees.sort((a, b) -> b.getValue() - a.getValue());

                    System.out.println("\n  Top 10 busiest hubs (by outgoing routes):");
                    int limit = Math.min(10, degrees.size());
                    for (int i = 0; i < limit; i++) {
                        Map.Entry<String, Integer> entry = degrees.get(i);
                        Airport ap = graph.getAirport(entry.getKey());
                        String name = (ap != null) ? ap.getCity() : entry.getKey();
                        System.out.printf("   %2d. %-4s  %-25s  %d routes%n",
                                i + 1, entry.getKey(), name, entry.getValue());
                    }
                    break;
                }

                // --------------------------------------------------------
                // 0 – Exit
                // --------------------------------------------------------
                case "0":
                    running = false;
                    System.out.println("\n  Goodbye. Safe travels!");
                    break;

                default:
                    System.out.println("  Invalid option. Please enter 0–8.");
            }

            if (running) System.out.println();
        }

        sc.close();
    }

    // ------------------------------------------------------------------
    // UI helpers
    // ------------------------------------------------------------------

    /** Prints the main menu banner. */
    private static void printMenu() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│             MAIN MENU                   │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  [1]  Cheapest route (Dijkstra/cost)    │");
        System.out.println("│  [2]  Fastest route  (Dijkstra/time)    │");
        System.out.println("│  [3]  Reachable airports within K hops  │");
        System.out.println("│  [4]  Critical airports (art. points)   │");
        System.out.println("│  [5]  Minimum Spanning Tree (Kruskal)   │");
        System.out.println("│  [6]  Travel-budget mode                │");
        System.out.println("│  [7]  Airport info lookup               │");
        System.out.println("│  [8]  Graph statistics                  │");
        System.out.println("│  [0]  Exit                              │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("Choice: ");
    }

    /**
     * Prompts the user for a valid IATA code that exists in the graph.
     * Returns null if the user enters an unknown code (after warning).
     */
    private static String promptIata(Scanner sc, Graph graph, String label) {
        System.out.printf("  %s: ", label);
        String code = sc.nextLine().trim().toUpperCase();
        if (!graph.containsAirport(code)) {
            System.out.printf("  Airport '%s' is not in the dataset.%n", code);
            return null;
        }
        return code;
    }

    /**
     * Prompts the user for an integer within [min, max].
     * Returns -1 on invalid input.
     */
    private static int promptInt(Scanner sc, String label, int min, int max) {
        System.out.printf("  %s (%d–%,d): ", label, min, max);
        String input = sc.nextLine().trim();
        try {
            int value = Integer.parseInt(input);
            if (value < min || value > max) {
                System.out.printf("  Value out of range. Must be between %d and %,d.%n", min, max);
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("  Invalid number.");
            return -1;
        }
    }

    /**
     * Formats and prints a Dijkstra result in a consistent way for both
     * cheapest-route and fastest-route queries.
     *
     * @param graph      the flight graph (for airport metadata)
     * @param result     result object from Dijkstra
     * @param src        source IATA
     * @param dest       destination IATA
     * @param mode       label for the header ("CHEAPEST" / "FASTEST")
     * @param weightName weight label ("cost" / "duration")
     * @param unit       unit prefix ("$" / "min ")
     */
    private static void printDijkstraResult(Graph graph, Dijkstra.Result result,
                                            String src, String dest,
                                            String mode, String weightName,
                                            String unit) {
        System.out.printf("%n  ── %s ROUTE:  %s → %s ──%n%n", mode, src, dest);

        if (!result.isReachable()) {
            System.out.printf("  No route found from %s to %s.%n", src, dest);
            return;
        }

        List<String> path = result.getPath();

        // Print each leg
        int totalCost = 0, totalMin = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to   = path.get(i + 1);

            // Find the specific edge used (the one matching Dijkstra's choice)
            Edge bestEdge = null;
            for (Edge e : graph.getEdges(from)) {
                if (e.getDestination().equals(to)) {
                    if (bestEdge == null) {
                        bestEdge = e;
                    } else {
                        // pick same weight direction as query
                        if (weightName.equals("cost") && e.getCostUSD() < bestEdge.getCostUSD()) {
                            bestEdge = e;
                        } else if (weightName.equals("duration")
                                && e.getDurationMin() < bestEdge.getDurationMin()) {
                            bestEdge = e;
                        }
                    }
                }
            }

            Airport fromAp = graph.getAirport(from);
            Airport toAp   = graph.getAirport(to);
            String fromLabel = (fromAp != null) ? fromAp.getCity() : from;
            String toLabel   = (toAp   != null) ? toAp.getCity()   : to;

            if (bestEdge != null) {
                totalCost += bestEdge.getCostUSD();
                totalMin  += bestEdge.getDurationMin();
                System.out.printf("  %-4s %-20s → %-4s %-20s  $%,5d  %d min%n",
                        from, fromLabel, to, toLabel,
                        bestEdge.getCostUSD(), bestEdge.getDurationMin());
            } else {
                System.out.printf("  %-4s → %-4s%n", from, to);
            }
        }

        System.out.println("  " + "─".repeat(72));
        System.out.printf("  Total: %d stop(s)   $%,d   %d min  (%dh %02dm)%n",
                path.size() - 2,     // stops = hops - 1 (exclude origin)
                totalCost,
                totalMin,
                totalMin / 60,
                totalMin % 60);
        System.out.printf("  Optimal %s weight: %s%s%n",
                weightName, unit,
                weightName.equals("cost")
                        ? String.format("%,d", result.getTotalWeight())
                        : result.getTotalWeight() + " min");
    }
}
