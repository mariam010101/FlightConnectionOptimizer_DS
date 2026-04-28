package org.example.utils;

import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.model.Airport;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSVLoader
 * ---------
 * Reads {@code airports.csv} and {@code routes.csv} from disk and constructs
 * a fully-populated {@link Graph}, enriching every route with cost and
 * duration values derived from the endpoint coordinates.
 *
 * Expected CSV formats
 * --------------------
 * airports.csv  (header row required)
 *   IATA,City,Country,Latitude,Longitude
 *   KEF,Keflavik,Iceland,63.985,-22.605
 *
 * routes.csv  (header row required)
 *   Source Airport,Destination Airport
 *   KEF,LHR
 *
 * Null sentinels
 * --------------
 * OpenFlights uses "\N" to denote missing values.  Any row whose source or
 * destination IATA starts with "\N" is silently ignored.
 * Any route referencing an airport absent from airports.csv is also ignored.
 *
 * Complexity
 * ----------
 *   loadGraph (airports pass) : O(A)   A = airport rows
 *   loadGraph (routes  pass)  : O(R)   R = route rows, each O(1) enrichment
 *   Overall                   : O(A + R)
 */
public class CSVLoader {

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Reads both CSV files and returns a ready-to-use {@link Graph}.
     *
     * Steps:
     *  1. Parse airports.csv → add each airport as a vertex.
     *  2. Parse routes.csv   → for each route look up both endpoints,
     *     compute distance via Haversine, derive cost + duration, add an Edge.
     *
     * Time complexity : O(A + R)
     * Space complexity: O(A + R)
     *
     * @param airportsFile path to airports.csv
     * @param routesFile   path to routes.csv
     * @return populated Graph (never null; may have 0 vertices if files are empty)
     * @throws IOException if either file cannot be opened or read
     */
    public static Graph loadGraph(String airportsFile, String routesFile)
            throws IOException {

        Graph graph = new Graph();

        // ---- Pass 1: airports ----------------------------------------
        try (BufferedReader br = new BufferedReader(new FileReader(airportsFile))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }   // skip header row
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = splitCSV(line);
                if (parts.length < 5) continue;

                String iata    = parts[0].trim();
                String city    = parts[1].trim();
                String country = parts[2].trim();
                String latStr  = parts[3].trim();
                String lonStr  = parts[4].trim();

                if (iata.equals("\\N") || iata.isEmpty()) continue;

                double lat, lon;
                try {
                    lat = Double.parseDouble(latStr);
                    lon = Double.parseDouble(lonStr);
                } catch (NumberFormatException e) {
                    continue;   // malformed coordinate — skip silently
                }

                graph.addAirport(new Airport(iata, city, country, lat, lon));
            }
        }

        // ---- Pass 2: routes ------------------------------------------
        try (BufferedReader br = new BufferedReader(new FileReader(routesFile))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = splitCSV(line);
                if (parts.length < 2) continue;

                String srcIata  = parts[0].trim();
                String destIata = parts[1].trim();

                if (srcIata.equals("\\N")  || srcIata.isEmpty())  continue;
                if (destIata.equals("\\N") || destIata.isEmpty()) continue;

                Airport src  = graph.getAirport(srcIata);
                Airport dest = graph.getAirport(destIata);
                if (src == null || dest == null) continue;   // airport not in dataset

                // Derive weights from coordinates
                double distKm   = RouteEnricher.haversineKm(
                        src.getLatitude(),  src.getLongitude(),
                        dest.getLatitude(), dest.getLongitude());
                int costUSD     = RouteEnricher.estimateCostUSD(distKm);
                int durationMin = RouteEnricher.estimateDurationMin(distKm);

                graph.addEdge(srcIata, new Edge(destIata, costUSD, durationMin));
            }
        }

        return graph;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Minimal CSV tokeniser that correctly handles quoted fields containing
     * commas — sufficient for well-formed OpenFlights data.
     *
     * Time complexity: O(n)  n = length of input line
     */
    private static String[] splitCSV(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes    = false;
        StringBuilder sb    = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());   // last token after final comma

        return tokens.toArray(new String[0]);
    }
}
