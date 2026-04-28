package org.example.tests;

import org.example.algorithms.BFSReachable;
import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.model.Airport;

import java.util.Map;

public class BFSReachableTest {

    public static void main(String[] args) {

        Graph graph = new Graph();

        // ---------------------------------------------------
        // Add Airports
        // ---------------------------------------------------

        graph.addAirport(new Airport("JFK", "New York", "USA", 40.6, -73.7));
        graph.addAirport(new Airport("ORD", "Chicago", "USA", 41.9, -87.9));
        graph.addAirport(new Airport("ATL", "Atlanta", "USA", 33.6, -84.4));
        graph.addAirport(new Airport("DFW", "Dallas", "USA", 32.8, -96.8));
        graph.addAirport(new Airport("LAX", "Los Angeles", "USA", 33.9, -118.4));
        graph.addAirport(new Airport("SEA", "Seattle", "USA", 47.4, -122.3));

        // ---------------------------------------------------
        // Add Routes
        // ---------------------------------------------------

        graph.addEdge("JFK", new Edge("ORD", 150, 120));
        graph.addEdge("JFK", new Edge("ATL", 180, 140));

        graph.addEdge("ORD", new Edge("DFW", 130, 150));
        graph.addEdge("ATL", new Edge("LAX", 250, 300));

        graph.addEdge("DFW", new Edge("SEA", 200, 220));

        // ---------------------------------------------------
        // Test K = 1
        // ---------------------------------------------------

        System.out.println("===== Reachable Within 1 Hop =====");

        Map<String, Integer> result1 = BFSReachable.within(graph, "JFK", 1);

        result1.forEach((airport, hops) ->
                System.out.println(airport + " -> " + hops + " hop(s)")
        );

        // ---------------------------------------------------
        // Test K = 2
        // ---------------------------------------------------

        System.out.println("\n===== Reachable Within 2 Hops =====");

        Map<String, Integer> result2 = BFSReachable.within(graph, "JFK", 2);

        result2.forEach((airport, hops) ->
                System.out.println(airport + " -> " + hops + " hop(s)")
        );

        // ---------------------------------------------------
        // Test K = 3
        // ---------------------------------------------------

        System.out.println("\n===== Reachable Within 3 Hops =====");

        Map<String, Integer> result3 = BFSReachable.within(graph, "JFK", 3);

        result3.forEach((airport, hops) ->
                System.out.println(airport + " -> " + hops + " hop(s)")
        );

        // ---------------------------------------------------
        // Invalid Airport Test
        // ---------------------------------------------------

        System.out.println("\n===== Invalid Airport =====");

        Map<String, Integer> invalid = BFSReachable.within(graph, "XXX", 2);

        System.out.println("Result size: " + invalid.size());

        // ---------------------------------------------------
        // Zero Hops Test
        // ---------------------------------------------------

        System.out.println("\n===== Zero Hops =====");

        Map<String, Integer> zero = BFSReachable.within(graph, "JFK", 0);

        System.out.println("Result size: " + zero.size());

        // ---------------------------------------------------
        // Isolated Airport Test
        // ---------------------------------------------------

        graph.addAirport(new Airport("ISO", "Isolated", "Nowhere", 0, 0));

        System.out.println("\n===== Isolated Airport =====");

        Map<String, Integer> isolated = BFSReachable.within(graph, "ISO", 3);

        System.out.println("Reachable airports: " + isolated.size());
    }
}