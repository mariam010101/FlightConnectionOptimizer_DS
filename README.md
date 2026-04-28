# ✈️ Flight Connection Optimizer

A graph-based flight network analyzer built in Java. Given a dataset of real-world airports and routes, the application constructs a weighted directed graph and lets you query it through a **JavaFX graphical interface** — finding cheapest routes, fastest routes, reachable airports, critical hubs, minimum spanning trees, and more.

---
***Created by Mariam Ghevondyan and Maria Danielyan***
---
---

## Project Description

Every airline booking engine needs to answer questions like: what is the cheapest way to get from city A to city B, possibly with layovers? What airports are reachable from a given hub? Which routes are structurally most critical?

This project builds a **flight network analyzer** using real-world-inspired data and applies multiple graph algorithms to answer these questions. The network is modelled as a **weighted directed graph** using adjacency lists, where each airport is a vertex and each route is a directed edge carrying two independent weights — cost (USD) and duration (minutes).

Since the raw routes dataset contains no cost or duration data, both weights are **computed on-the-fly** at load time using:

- **Haversine formula** → great-circle distance between two lat/lon coordinates
- **Tiered per-km pricing model** → estimated one-way economy fare in USD
- **Cruising speed model** → estimated door-to-door duration in minutes

The result is a fully-weighted graph ready for shortest-path, reachability, structural, and spanning-tree queries — all accessible through an interactive JavaFX GUI.

---

## 📋 Table of Contents

- [Project Description](#project-description)
- [Requirements Checklist](#requirements-checklist)
- [Features](#features)
- [Interface Preview](#interface-preview)
- [Project Structure](#project-structure)
- [Data Model](#data-model)
- [Algorithms](#algorithms)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Complexity Analysis](#complexity-analysis)
- [Technologies](#technologies)



## Requirements Checklist

### ✅ Core Requirements (All Met)

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| Load a flight dataset from CSV — at least 30 airports and 80 routes with cost and duration as edge weights | ✅ Done | `CSVLoader.java` + `RouteEnricher.java` — weights computed from coordinates via Haversine |
| Represent the network as a weighted directed graph using adjacency lists | ✅ Done | `Graph.java` — `HashMap<String, List<Edge>>` adjacency list |
| Dijkstra's algorithm — cheapest route (cost weight) | ✅ Done | `Dijkstra.cheapest()` in `Dijkstra.java` |
| Dijkstra's algorithm — fastest route (duration weight) | ✅ Done | `Dijkstra.fastest()` in `Dijkstra.java` |
| BFS — all airports reachable within at most K connections | ✅ Done | `BFSReachable.within()` in `BFSReachable.java` |
| Detect airports that, if removed, would disconnect the graph (articulation points) | ✅ Done | `ArticulationPoints.find()` — Tarjan's DFS algorithm |
| Handle edge cases: no route exists, source equals destination, airport not in dataset | ✅ Done | Validated in `Dijkstra.java`, `BFSReachable.java`, and GUI input layer |
| Complexity analysis for all graph operations | ✅ Done | Documented in Javadoc on every class and in this README |

### 🌟 Bonus Requirements (All Met)

| Bonus Feature | Status | Implementation |
|---------------|--------|----------------|
| Kruskal's algorithm — Minimum Spanning Tree visualised as a list of essential routes | ✅ Done | `MSTKruskal.java` — Kruskal + Union-Find with path compression and union by rank |
| Travel budget mode — all destinations reachable within a given cost budget | ✅ Done | `Dijkstra.withinBudget()` — modified Dijkstra pruned at budget threshold |

---

## Features

| Tab | Feature | Algorithm |
|-----|---------|-----------|
| 💰 Cheapest Route | Leg-by-leg breakdown of the lowest-cost path between two airports | Dijkstra (cost weight) |
| ⚡ Fastest Route | Lowest total duration path between two airports | Dijkstra (duration weight) |
| 🔍 BFS Reachable | All airports reachable within K connections from a departure hub | Breadth-First Search |
| ⚠️ Critical Airports | Airports whose removal would disconnect the network | Tarjan's Articulation Points |
| 🌐 Min Span Tree | Minimum-cost backbone of essential routes | Kruskal + Union-Find |
| 💳 Travel Budget | All destinations reachable from an airport within a dollar budget | Dijkstra (pruned by budget) |
| ℹ️ Airport Info | Metadata + all outgoing flights for any airport | Graph lookup |
| 📊 Graph Statistics | Total vertices/edges, average degree, top 50 busiest hubs | Degree sort |

---

## Interface Preview

<!-- Add your screenshot(s) here. Replace the placeholder below with your actual image file. -->
<!-- To add a screenshot: take a screenshot of the running app, save it as screenshot.png in the repo root, then the line below will display it automatically. -->

![Flight Connection Optimizer GUI](Screenshot.png)

<!-- To add more screenshots, duplicate the line above with different filenames, e.g.: -->
<!-- ![Cheapest Route Tab](screenshots/cheapest.png) -->
<!-- ![BFS Reachable Tab](screenshots/bfs.png) -->

---

## Project Structure

```
FlightConnectionOptimizer_DS/
│
├── src/
│   ├── main/java/org.example/
│      ├── Main.java                   # JavaFX bootstrap launcher
│      ├── MainApp.java                # Full GUI (all 8 tabs)
│      ├── MainCLI.java                # Original CLI (kept as backup)
│      │
│      ├── model/
│      │   ├── Airport.java            # Vertex: IATA, city, country, lat/lon
│      │   └── FlightRoute.java        # Legacy route model
│      │
│      ├── graph/
│      │   ├── Graph.java              # Adjacency-list weighted directed graph
│      │   └── Edge.java               # Directed edge: destination, cost, duration
│      │
│      ├── algorithms/
│      │   ├── Dijkstra.java           # Cheapest/fastest route + budget mode
│      │   ├── BFSReachable.java       # Reachability within K hops
│      │   ├── ArticulationPoints.java # Critical airport detection (Tarjan DFS)
│      │   └── MSTKruskal.java         # Minimum Spanning Tree (Kruskal + DSU)
│      │
│      └── utils/
│      |   ├── CSVLoader.java          # Parses airports.csv + routes.csv → Graph
│      |   └── RouteEnricher.java      # Haversine distance → cost + duration
│      │  
│      │  
│      │  
│      │   
│      └── tests/
│          ├── GraphTest.java
│          ├── DijkstraTest.java
│          └── BFSReachableTest.java
│       
│
├── data/
│   ├── airports.csv                    # 30+ airports: IATA, city, country, lat, lon
│   └── routes.csv                      # 80+ routes: source IATA, destination IATA
│
├── pom.xml
└── README.md
```

---

## Data Model

### `Real-World Dataset Source URL link` 

[https://www.kaggle.com/datasets/elmoallistair/airlines-airport-and-routes?resource=download](https://www.kaggle.com/datasets/elmoallistair/airlines-airport-and-routes?resource=download)

### `airports.csv`
```
IATA,City,Country,Latitude,Longitude
KEF,Keflavik,Iceland,63.985,-22.605
LHR,London,United Kingdom,51.477,-0.461
```

### `routes.csv`
```
Source Airport,Destination Airport
KEF,LHR
LHR,JFK
```

Cost and duration are **not stored in the CSV**. They are derived at load time:

| Property | Formula |
|----------|---------|
| Distance | Haversine great-circle distance (km) |
| Cost (USD) | `$35 base + km × rate` where rate = $0.18/km (≤500km), $0.12/km (≤3000km), $0.08/km (>3000km) |
| Duration (min) | `(km ÷ 880 km/h) × 60 + 90 min overhead` |

---

## Algorithms

### Dijkstra — Cheapest / Fastest Route
Standard single-source shortest path with a min-heap priority queue. Run twice: once with `costUSD` as the edge weight, once with `durationMin`. Early exit when the destination vertex is finalised.

```
Time:  O((V + E) log V)
Space: O(V + E)
```

### BFS — Reachable Airports within K Hops
Layer-by-layer BFS that stops expanding a vertex once its hop depth equals `K`. The first time a vertex is reached is always the minimum number of hops.

```
Time:  O(V + E)
Space: O(V)
```

### Articulation Points — Critical Airports
Tarjan's DFS algorithm on the undirected view of the graph (each directed edge treated as bidirectional). Uses discovery timestamps `disc[]` and low-link values `low[]` to identify cut vertices.

```
Time:  O(V + E)
Space: O(V + E)
```

### Kruskal's MST — Network Backbone ⭐ Bonus
Collects all undirected edges (deduplicating by keeping the cheaper direction), sorts by cost, then greedily picks edges that connect different components using **Union-Find with path compression and union by rank**.

```
Time:  O(E log E)
Space: O(V + E)
```

### Budget Mode — Modified Dijkstra ⭐ Bonus
Full Dijkstra from the source that prunes any vertex whose accumulated cost exceeds the user-specified budget. Returns all reachable airports mapped to their minimum cost.

```
Time:  O((V + E) log V)
Space: O(V)
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- IntelliJ IDEA (recommended) or any IDE with Maven support

### Clone the Repository

```bash
git clone https://github.com/mariam010101/FlightConnectionOptimizer_DB.git
cd FlightConnectionOptimizer_DB
```

### Place Data Files

Make sure your CSV files are at:
```
FlightConnectionOptimizer_DS/
└── data/
    ├── airports.csv
    └── routes.csv
```

The `data/` folder must be at the **project root** (same level as `src/` and `pom.xml`), not inside `src/`.

---

## Running the Application

### Option A — IntelliJ IDEA (Recommended)

1. Open the project in IntelliJ IDEA
2. Right-click `pom.xml` → **Maven → Sync Project**
3. Right-click `Main.java` → **Run 'Main.main()'**

### Option B — Maven CLI

```bash
mvn javafx:run
```

### Option C — IntelliJ Run Configuration

Go to **Run → Edit Configurations** and set:
- **Main class:** `org.example.Main`
- **Working directory:** `$PROJECT_DIR$`

---

## Running Tests

```bash
mvn test
```

Or in IntelliJ: right-click the `test/` folder → **Run All Tests**.

Test classes cover:
- `GraphTest` — vertex/edge insertion, containment, edge counts
- `DijkstraTest` — cheapest/fastest path correctness, unreachable airports, same-node edge case
- `BFSReachableTest` — hop-count accuracy, boundary conditions (K=0, isolated node)

---

## Complexity Analysis

| Operation | Time | Space |
|-----------|------|-------|
| Load airports (CSVLoader) | O(A) | O(A) |
| Load routes + enrich (CSVLoader) | O(R) | O(R) |
| Add airport (Graph) | O(1) amortised | O(1) |
| Add edge (Graph) | O(1) amortised | O(1) |
| Get edges (Graph) | O(1) | O(1) |
| Haversine distance (RouteEnricher) | O(1) | O(1) |
| Dijkstra (cheapest / fastest) | O((V+E) log V) | O(V+E) |
| Dijkstra within budget | O((V+E) log V) | O(V) |
| BFS within K hops | O(V+E) | O(V) |
| Articulation points | O(V+E) | O(V+E) |
| Kruskal MST | O(E log E) | O(V+E) |

where **A** = airport rows, **R** = route rows, **V** = vertices, **E** = edges.

---

## Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Core language |
| JavaFX | 21 | Graphical user interface |
| Maven | 3.8+ | Build and dependency management |
| JUnit Jupiter | 5.10.2 | Unit testing |
| OpenFlights | — | Real-world airport and route data |
