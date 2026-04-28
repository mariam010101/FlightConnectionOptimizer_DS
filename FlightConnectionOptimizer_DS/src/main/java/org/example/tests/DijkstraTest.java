package org.example.tests;

import org.example.algorithms.Dijkstra;
import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.model.Airport;

import java.util.Map;

public class DijkstraTest {

    public static void main(String[] args) {

        Graph graph = new Graph();

        // ---------------------------------------------------
        // Create Airports
        // ---------------------------------------------------

        graph.addAirport(new Airport("JFK", "New York", "USA", 40.6, -73.7));
        graph.addAirport(new Airport("ORD", "Chicago", "USA", 41.9, -87.9));
        graph.addAirport(new Airport("DFW", "Dallas", "USA", 32.8, -96.8));
        graph.addAirport(new Airport("LAX", "Los Angeles", "USA", 33.9, -118.4));
        graph.addAirport(new Airport("MIA", "Miami", "USA", 25.7, -80.2));

        // ---------------------------------------------------
        // Add Flight Routes
        // ---------------------------------------------------

        graph.addEdge("JFK", new Edge("ORD", 150, 120));
        graph.addEdge("ORD", new Edge("LAX", 120, 240));

        graph.addEdge("JFK", new Edge("DFW", 200, 150));
        graph.addEdge("DFW", new Edge("LAX", 80, 120));

        graph.addEdge("JFK", new Edge("MIA", 90, 180));
        graph.addEdge("MIA", new Edge("LAX", 300, 320));

        // ---------------------------------------------------
        // Cheapest Route Test
        // ---------------------------------------------------

        System.out.println("===== Cheapest Route =====");

        Dijkstra.Result cheapest = Dijkstra.cheapest(graph, "JFK", "LAX");

        if (cheapest.isReachable()) {
            System.out.println("Path: " + cheapest.getPath());
            System.out.println("Total Cost: $" + cheapest.getTotalWeight());
        } else {
            System.out.println("No route found.");
        }

        // ---------------------------------------------------
        // Fastest Route Test
        // ---------------------------------------------------

        System.out.println("\n===== Fastest Route =====");

        Dijkstra.Result fastest = Dijkstra.fastest(graph, "JFK", "LAX");

        if (fastest.isReachable()) {
            System.out.println("Path: " + fastest.getPath());
            System.out.println("Total Duration: " + fastest.getTotalWeight() + " minutes");
        } else {
            System.out.println("No route found.");
        }

        // ---------------------------------------------------
        // Budget Mode Test
        // ---------------------------------------------------

        System.out.println("\n===== Budget Mode =====");

        Map<String, Integer> reachable = Dijkstra.withinBudget(graph, "JFK", 300);

        for (Map.Entry<String, Integer> entry : reachable.entrySet()) {
            System.out.println(entry.getKey() + " reachable for $" + entry.getValue());
        }

        // ---------------------------------------------------
        // Same Airport Test
        // ---------------------------------------------------

        System.out.println("\n===== Same Source/Destination =====");

        Dijkstra.Result same = Dijkstra.cheapest(graph, "JFK", "JFK");

        System.out.println("Path: " + same.getPath());
        System.out.println("Cost: " + same.getTotalWeight());

        // ---------------------------------------------------
        // No Route Test
        // ---------------------------------------------------

        System.out.println("\n===== No Route Test =====");

        Dijkstra.Result noRoute = Dijkstra.cheapest(graph, "LAX", "JFK");

        if (!noRoute.isReachable()) {
            System.out.println("Correct: No route exists.");
        } else {
            System.out.println("Unexpected route found.");
        }
    }
}