package org.example.tests;

import org.example.graph.Edge;
import org.example.graph.Graph;
import org.example.model.Airport;

public class GraphTest {

    public static void main(String[] args) {

        Graph graph = new Graph();

        // ---------------------------------------------------
        // Add Airports
        // ---------------------------------------------------

        Airport jfk = new Airport("JFK", "New York", "USA", 40.6413, -73.7781);
        Airport lax = new Airport("LAX", "Los Angeles", "USA", 33.9416, -118.4085);
        Airport ord = new Airport("ORD", "Chicago", "USA", 41.9742, -87.9073);

        graph.addAirport(jfk);
        graph.addAirport(lax);
        graph.addAirport(ord);

        // ---------------------------------------------------
        // Add Routes
        // ---------------------------------------------------

        graph.addEdge("JFK", new Edge("LAX", 300, 360));
        graph.addEdge("JFK", new Edge("ORD", 150, 120));
        graph.addEdge("ORD", new Edge("LAX", 120, 240));

        // ---------------------------------------------------
        // Test Counts
        // ---------------------------------------------------

        System.out.println("Vertices: " + graph.vertexCount());
        System.out.println("Edges: " + graph.edgeCount());

        // ---------------------------------------------------
        // Test Contains
        // ---------------------------------------------------

        System.out.println("Contains JFK: " + graph.containsAirport("JFK"));
        System.out.println("Contains CDG: " + graph.containsAirport("CDG"));

        // ---------------------------------------------------
        // Test Airport Lookup
        // ---------------------------------------------------

        System.out.println("Airport JFK:");
        System.out.println(graph.getAirport("JFK"));

        // ---------------------------------------------------
        // Test Edge Retrieval
        // ---------------------------------------------------

        System.out.println("Edges from JFK:");

        graph.getEdges("JFK").forEach(System.out::println);

        // ---------------------------------------------------
        // Test Graph Output
        // ---------------------------------------------------

        System.out.println("\nFull Graph:");
        System.out.println(graph);
    }
}