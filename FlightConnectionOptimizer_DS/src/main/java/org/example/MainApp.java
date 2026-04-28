package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

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
import java.util.stream.Collectors;

/**
 * MainApp
 * -------
 * JavaFX graphical interface for the Flight Connection Optimizer.
 *
 * Layout
 * ------
 *   ┌─ Header bar (title + graph stats) ─────────────────────┐
 *   ├─ TabPane ───────────────────────────────────────────────┤
 *   │   Tab 1 │ Cheapest Route      (Dijkstra / cost)         │
 *   │   Tab 2 │ Fastest Route       (Dijkstra / duration)     │
 *   │   Tab 3 │ Reachable Airports  (BFS / K hops)            │
 *   │   Tab 4 │ Critical Airports   (Articulation Points)      │
 *   │   Tab 5 │ Min Spanning Tree   (Kruskal)                 │
 *   │   Tab 6 │ Travel Budget       (Dijkstra within $X)      │
 *   │   Tab 7 │ Airport Info                                   │
 *   │   Tab 8 │ Graph Statistics                              │
 *   └─────────────────────────────────────────────────────────┘
 *
 * Every tab is self-contained: inputs at the top, results table below.
 * The graph is loaded once on startup from data/airports.csv + data/routes.csv.
 */
public class MainApp extends Application {

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    private static final String DEFAULT_AIRPORTS = "data/airports.csv";
    private static final String DEFAULT_ROUTES   = "data/routes.csv";

    // Colour palette
    private static final String CLR_NAVY    = "#1a2744";
    private static final String CLR_BLUE    = "#2563eb";
    private static final String CLR_LIGHT   = "#f0f4ff";
    private static final String CLR_WHITE   = "#ffffff";
    private static final String CLR_ACCENT  = "#16a34a";
    private static final String CLR_ERROR   = "#dc2626";
    private static final String CLR_BORDER  = "#cbd5e1";

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private Graph graph;
    private List<String> sortedIataCodes;   // alphabetically sorted, for ComboBoxes

    // ------------------------------------------------------------------
    // JavaFX start
    // ------------------------------------------------------------------

    @Override
    public void start(Stage stage) {
        // ---- Load graph data -----------------------------------------
        try {
            graph = CSVLoader.loadGraph(DEFAULT_AIRPORTS, DEFAULT_ROUTES);
        } catch (IOException e) {
            showFatalError(stage, e.getMessage());
            return;
        }

        sortedIataCodes = graph.getAllIataCodes()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        // ---- Root layout ---------------------------------------------
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildTabPane());
        root.setStyle("-fx-background-color: " + CLR_LIGHT + ";");

        Scene scene = new Scene(root, 1100, 720);
        stage.setScene(scene);
        stage.setTitle("Flight Connection Optimizer");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    // ==================================================================
    // HEADER
    // ==================================================================

    private VBox buildHeader() {
        Label title = new Label("✈  Flight Connection Optimizer");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.WHITE);

        Label stats = new Label(String.format(
                "Network: %,d airports   •   %,d routes",
                graph.vertexCount(), graph.edgeCount()));
        stats.setFont(Font.font("System", 13));
        stats.setTextFill(Color.web("#94a3b8"));

        VBox header = new VBox(4, title, stats);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle("-fx-background-color: " + CLR_NAVY + ";");
        return header;
    }

    // ==================================================================
    // TAB PANE
    // ==================================================================

    private TabPane buildTabPane() {
        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tp.setStyle("-fx-background-color: " + CLR_LIGHT + ";");

        tp.getTabs().addAll(
                makeTab("💰 Cheapest Route",    buildCheapestTab()),
                makeTab("⚡ Fastest Route",     buildFastestTab()),
                makeTab("🔍 BFS Reachable",     buildBfsTab()),
                makeTab("⚠️ Critical Airports",  buildArticulationTab()),
                makeTab("🌐 Min Span Tree",      buildMstTab()),
                makeTab("💳 Travel Budget",      buildBudgetTab()),
                makeTab("ℹ️ Airport Info",        buildInfoTab()),
                makeTab("📊 Graph Stats",        buildStatsTab())
        );
        return tp;
    }

    private Tab makeTab(String label, Region content) {
        Tab t = new Tab(label, content);
        t.setStyle("-fx-font-size: 13px;");
        return t;
    }

    // ==================================================================
    // TAB 1 — CHEAPEST ROUTE  (Dijkstra / cost)
    // ==================================================================

    private VBox buildCheapestTab() {
        ComboBox<String> srcBox  = airportCombo("Origin");
        ComboBox<String> dstBox  = airportCombo("Destination");
        Button           runBtn  = primaryButton("Find Cheapest Route");
        Label            errLbl  = errorLabel();

        // Results table
        TableView<RouteRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Enter airports and click the button above."));
        addColumns(table, true);

        Label totalLbl = summaryLabel();

        runBtn.setOnAction(e -> {
            errLbl.setText("");
            String src = selectedIata(srcBox);
            String dst = selectedIata(dstBox);
            if (!validatePair(src, dst, errLbl)) return;

            Dijkstra.Result result = Dijkstra.cheapest(graph, src, dst);
            if (!result.isReachable()) {
                errLbl.setText("No route found between " + src + " and " + dst + ".");
                table.getItems().clear();
                totalLbl.setText("");
                return;
            }
            populateRouteTable(table, totalLbl, result.getPath(), true);
        });

        return tabLayout(
                inputRow(label("From:"), srcBox, label("To:"), dstBox, runBtn),
                errLbl, table, totalLbl);
    }

    // ==================================================================
    // TAB 2 — FASTEST ROUTE  (Dijkstra / duration)
    // ==================================================================

    private VBox buildFastestTab() {
        ComboBox<String> srcBox = airportCombo("Origin");
        ComboBox<String> dstBox = airportCombo("Destination");
        Button           runBtn = primaryButton("Find Fastest Route");
        Label            errLbl = errorLabel();

        TableView<RouteRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Enter airports and click the button above."));
        addColumns(table, false);

        Label totalLbl = summaryLabel();

        runBtn.setOnAction(e -> {
            errLbl.setText("");
            String src = selectedIata(srcBox);
            String dst = selectedIata(dstBox);
            if (!validatePair(src, dst, errLbl)) return;

            Dijkstra.Result result = Dijkstra.fastest(graph, src, dst);
            if (!result.isReachable()) {
                errLbl.setText("No route found between " + src + " and " + dst + ".");
                table.getItems().clear();
                totalLbl.setText("");
                return;
            }
            populateRouteTable(table, totalLbl, result.getPath(), false);
        });

        return tabLayout(
                inputRow(label("From:"), srcBox, label("To:"), dstBox, runBtn),
                errLbl, table, totalLbl);
    }

    // ==================================================================
    // TAB 3 — BFS REACHABLE
    // ==================================================================

    private VBox buildBfsTab() {
        ComboBox<String> srcBox  = airportCombo("Departure");
        Spinner<Integer> spinner = new Spinner<>(1, 15, 2);
        spinner.setPrefWidth(80);
        spinner.setEditable(true);
        Button runBtn = primaryButton("Find Reachable Airports");
        Label  errLbl = errorLabel();

        TableView<BfsRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Enter departure airport and max hops."));

        TableColumn<BfsRow, String>  iataCol = col("IATA",    "iata",    100);
        TableColumn<BfsRow, String>  cityCol = col("City",    "city",    200);
        TableColumn<BfsRow, String>  cntCol  = col("Country", "country", 180);
        TableColumn<BfsRow, Integer> hopCol  = colInt("Hops", "hops",    80);
        table.getColumns().addAll(iataCol, cityCol, cntCol, hopCol);

        Label countLbl = summaryLabel();

        runBtn.setOnAction(e -> {
            errLbl.setText("");
            String src = selectedIata(srcBox);
            if (src == null) { errLbl.setText("Please select a departure airport."); return; }

            int maxHops = spinner.getValue();
            Map<String, Integer> reachable = BFSReachable.within(graph, src, maxHops);

            ObservableList<BfsRow> rows = FXCollections.observableArrayList();
            reachable.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .forEach(entry -> {
                        Airport ap = graph.getAirport(entry.getKey());
                        String city    = (ap != null) ? ap.getCity()    : "—";
                        String country = (ap != null) ? ap.getCountry() : "—";
                        rows.add(new BfsRow(entry.getKey(), city, country, entry.getValue()));
                    });

            table.setItems(rows);
            countLbl.setText(String.format("Found %,d reachable airport(s) within %d hop(s) from %s",
                    rows.size(), maxHops, src));
        });

        HBox spinnerRow = new HBox(10,
                label("From:"), srcBox,
                label("Max hops:"), spinner,
                runBtn);
        spinnerRow.setAlignment(Pos.CENTER_LEFT);

        return tabLayout(spinnerRow, errLbl, table, countLbl);
    }

    // ==================================================================
    // TAB 4 — ARTICULATION POINTS
    // ==================================================================

    private VBox buildArticulationTab() {
        Button runBtn = primaryButton("Find Critical Airports");
        Label  errLbl = errorLabel();

        TableView<ArtRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Click the button to find critical airports."));

        TableColumn<ArtRow, String> iataCol    = col("IATA",          "iata",    90);
        TableColumn<ArtRow, String> cityCol    = col("City",          "city",    200);
        TableColumn<ArtRow, String> countryCol = col("Country",       "country", 180);
        TableColumn<ArtRow, String> routesCol  = col("Outgoing Routes","routes", 120);
        table.getColumns().addAll(iataCol, cityCol, countryCol, routesCol);

        Label countLbl = summaryLabel();

        runBtn.setOnAction(e -> {
            errLbl.setText("");
            Set<String> critical = ArticulationPoints.find(graph);
            ObservableList<ArtRow> rows = FXCollections.observableArrayList();
            critical.stream().sorted().forEach(iata -> {
                Airport ap   = graph.getAirport(iata);
                String city    = (ap != null) ? ap.getCity()    : "—";
                String country = (ap != null) ? ap.getCountry() : "—";
                int    deg     = graph.getEdges(iata).size();
                rows.add(new ArtRow(iata, city, country, String.valueOf(deg)));
            });
            table.setItems(rows);
            countLbl.setText(String.format(
                    "%,d critical airport(s) found — removing any one would disconnect the network.",
                    rows.size()));
        });

        return tabLayout(
                inputRow(runBtn),
                errLbl, table, countLbl);
    }

    // ==================================================================
    // TAB 5 — MINIMUM SPANNING TREE  (Kruskal)
    // ==================================================================

    private VBox buildMstTab() {
        Button runBtn = primaryButton("Compute MST (Kruskal)");
        Label  errLbl = errorLabel();

        TableView<MstRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Click the button to compute the Minimum Spanning Tree."));

        TableColumn<MstRow, Integer> rankCol  = colInt("#",    "rank",  50);
        TableColumn<MstRow, String>  srcCol   = col("From",   "src",   90);
        TableColumn<MstRow, String>  dstCol   = col("To",     "dest",  90);
        TableColumn<MstRow, String>  costCol  = col("Cost ($)","cost", 110);
        table.getColumns().addAll(rankCol, srcCol, dstCol, costCol);

        Label totalLbl = summaryLabel();

        runBtn.setOnAction(e -> {
            errLbl.setText("");
            List<MSTKruskal.MSTEdge> mst = MSTKruskal.compute(graph);
            ObservableList<MstRow> rows = FXCollections.observableArrayList();
            int idx = 1;
            int totalCost = 0;
            for (MSTKruskal.MSTEdge edge : mst) {
                rows.add(new MstRow(idx++, edge.getSrc(), edge.getDest(),
                        String.format("$%,d", edge.getCostUSD())));
                totalCost += edge.getCostUSD();
            }
            table.setItems(rows);
            totalLbl.setText(String.format(
                    "MST: %,d essential routes   •   Total network cost: $%,d",
                    mst.size(), totalCost));
        });

        return tabLayout(inputRow(runBtn), errLbl, table, totalLbl);
    }

    // ==================================================================
    // TAB 6 — TRAVEL BUDGET
    // ==================================================================

    private VBox buildBudgetTab() {
        ComboBox<String> srcBox    = airportCombo("Departure");
        TextField        budgetFld = new TextField("500");
        budgetFld.setPrefWidth(90);
        budgetFld.setPromptText("USD");
        Button runBtn = primaryButton("Find Destinations");
        Label  errLbl = errorLabel();

        TableView<BudgetRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Enter departure airport and budget."));

        TableColumn<BudgetRow, String>  iataCol    = col("IATA",       "iata",    90);
        TableColumn<BudgetRow, String>  cityCol    = col("City",       "city",    200);
        TableColumn<BudgetRow, String>  countryCol = col("Country",    "country", 160);
        TableColumn<BudgetRow, String>  costCol    = col("Min Cost ($)","cost",   120);
        table.getColumns().addAll(iataCol, cityCol, countryCol, costCol);

        Label countLbl = summaryLabel();

        runBtn.setOnAction(e -> {
            errLbl.setText("");
            String src = selectedIata(srcBox);
            if (src == null) { errLbl.setText("Please select a departure airport."); return; }

            int budget;
            try {
                budget = Integer.parseInt(budgetFld.getText().trim());
                if (budget <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLbl.setText("Please enter a valid positive integer for the budget.");
                return;
            }

            Map<String, Integer> reachable = Dijkstra.withinBudget(graph, src, budget);
            ObservableList<BudgetRow> rows = FXCollections.observableArrayList();
            reachable.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(entry -> {
                        Airport ap     = graph.getAirport(entry.getKey());
                        String city    = (ap != null) ? ap.getCity()    : "—";
                        String country = (ap != null) ? ap.getCountry() : "—";
                        rows.add(new BudgetRow(entry.getKey(), city, country,
                                String.format("$%,d", entry.getValue())));
                    });

            table.setItems(rows);
            countLbl.setText(String.format(
                    "%,d destination(s) reachable from %s within $%,d",
                    rows.size(), src, budget));
        });

        HBox row = new HBox(10,
                label("From:"), srcBox,
                label("Budget ($):"), budgetFld,
                runBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return tabLayout(row, errLbl, table, countLbl);
    }

    // ==================================================================
    // TAB 7 — AIRPORT INFO
    // ==================================================================

    private VBox buildInfoTab() {
        ComboBox<String> box    = airportCombo("Airport");
        Button           runBtn = primaryButton("Look Up");
        Label            errLbl = errorLabel();

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setVisible(false);

        Label[] keys = {
                boldLabel("IATA Code:"), boldLabel("City:"),
                boldLabel("Country:"),   boldLabel("Latitude:"),
                boldLabel("Longitude:"), boldLabel("Outgoing Routes:")
        };
        Label[] vals = new Label[keys.length];
        for (int i = 0; i < keys.length; i++) {
            vals[i] = new Label();
            vals[i].setFont(Font.font("System", 14));
            grid.add(keys[i], 0, i);
            grid.add(vals[i], 1, i);
        }

        // Mini-table of outgoing edges
        TableView<EdgeRow> edgeTable = new TableView<>();
        edgeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        edgeTable.setPrefHeight(260);
        TableColumn<EdgeRow, String> destCol = col("Destination", "dest",    120);
        TableColumn<EdgeRow, String> costCol = col("Cost (USD)",  "cost",    110);
        TableColumn<EdgeRow, String> durCol  = col("Duration",    "duration",120);
        edgeTable.getColumns().addAll(destCol, costCol, durCol);
        edgeTable.setVisible(false);

        runBtn.setOnAction(e -> {
            errLbl.setText("");
            String iata = selectedIata(box);
            if (iata == null) { errLbl.setText("Please select an airport."); return; }

            Airport ap = graph.getAirport(iata);
            if (ap == null) { errLbl.setText("Airport not found."); return; }

            vals[0].setText(ap.getIata());
            vals[1].setText(ap.getCity());
            vals[2].setText(ap.getCountry());
            vals[3].setText(String.format("%.6f", ap.getLatitude()));
            vals[4].setText(String.format("%.6f", ap.getLongitude()));
            vals[5].setText(String.valueOf(graph.getEdges(iata).size()));
            grid.setVisible(true);

            ObservableList<EdgeRow> edgeRows = FXCollections.observableArrayList();
            for (Edge edge : graph.getEdges(iata)) {
                Airport dest = graph.getAirport(edge.getDestination());
                String destLabel = (dest != null)
                        ? edge.getDestination() + " – " + dest.getCity()
                        : edge.getDestination();
                int m = edge.getDurationMin();
                edgeRows.add(new EdgeRow(
                        destLabel,
                        String.format("$%,d", edge.getCostUSD()),
                        String.format("%dh %02dm", m / 60, m % 60)));
            }
            edgeTable.setItems(edgeRows);
            edgeTable.setVisible(true);
        });

        VBox content = new VBox(14,
                inputRow(label("Airport:"), box, runBtn),
                errLbl, grid,
                boldLabel("Outgoing flights from this airport:"),
                edgeTable);
        content.setPadding(new Insets(20));
        return content;
    }

    // ==================================================================
    // TAB 8 — GRAPH STATISTICS
    // ==================================================================

    private VBox buildStatsTab() {
        Button runBtn = primaryButton("Load Statistics");
        Label  errLbl = errorLabel();

        // Top-hubs table
        TableView<HubRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Click 'Load Statistics' to view graph data."));

        TableColumn<HubRow, Integer> rankCol   = colInt("Rank",          "rank",    60);
        TableColumn<HubRow, String>  iataCol   = col("IATA",             "iata",    80);
        TableColumn<HubRow, String>  cityCol   = col("City",             "city",   200);
        TableColumn<HubRow, String>  countryCol= col("Country",          "country",180);
        TableColumn<HubRow, Integer> degCol    = colInt("Outgoing Routes","degree", 130);
        table.getColumns().addAll(rankCol, iataCol, cityCol, countryCol, degCol);

        Label summaryLbl = summaryLabel();

        runBtn.setOnAction(e -> {
            // Summary numbers
            int    V = graph.vertexCount();
            int    E = graph.edgeCount();
            double avg = V == 0 ? 0 : (double) E / V;

            summaryLbl.setText(String.format(
                    "Airports: %,d   •   Routes: %,d   •   Avg outgoing routes/airport: %.1f",
                    V, E, avg));

            // Build ranked hub list
            List<Map.Entry<String, Integer>> ranked = new ArrayList<>();
            for (String iata : graph.getAllIataCodes()) {
                ranked.add(new AbstractMap.SimpleEntry<>(iata, graph.getEdges(iata).size()));
            }
            ranked.sort((a, b) -> b.getValue() - a.getValue());

            ObservableList<HubRow> rows = FXCollections.observableArrayList();
            int limit = Math.min(50, ranked.size());
            for (int i = 0; i < limit; i++) {
                String iata    = ranked.get(i).getKey();
                int    degree  = ranked.get(i).getValue();
                Airport ap     = graph.getAirport(iata);
                String city    = (ap != null) ? ap.getCity()    : "—";
                String country = (ap != null) ? ap.getCountry() : "—";
                rows.add(new HubRow(i + 1, iata, city, country, degree));
            }
            table.setItems(rows);
            errLbl.setText("");
        });

        return tabLayout(inputRow(runBtn), errLbl, table, summaryLbl);
    }

    // ==================================================================
    // ROUTE TABLE HELPER  (shared by tabs 1 & 2)
    // ==================================================================

    /**
     * Fills a RouteRow table from a Dijkstra path.
     *
     * @param byCost true → sort/highlight by cost; false → by duration
     */
    @SuppressWarnings("unchecked")
    private void addColumns(TableView<RouteRow> table, boolean byCost) {
        TableColumn<RouteRow, String> fromCol = col("From (IATA)", "from",     120);
        TableColumn<RouteRow, String> fcityCol= col("City",        "fromCity", 180);
        TableColumn<RouteRow, String> toCol   = col("To (IATA)",   "to",       120);
        TableColumn<RouteRow, String> tcityCol= col("City",        "toCity",   180);
        TableColumn<RouteRow, String> costCol = col("Cost (USD)",  "cost",     110);
        TableColumn<RouteRow, String> durCol  = col("Duration",    "duration", 110);
        table.getColumns().addAll(fromCol, fcityCol, toCol, tcityCol, costCol, durCol);
    }

    private void populateRouteTable(TableView<RouteRow> table, Label totalLbl,
                                    List<String> path, boolean byCost) {
        ObservableList<RouteRow> rows = FXCollections.observableArrayList();
        int totalCost = 0, totalMin = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to   = path.get(i + 1);
            Edge   best = bestEdge(from, to, byCost);

            Airport fromAp = graph.getAirport(from);
            Airport toAp   = graph.getAirport(to);
            String fromCity = (fromAp != null) ? fromAp.getCity() : "—";
            String toCity   = (toAp   != null) ? toAp.getCity()   : "—";

            if (best != null) {
                totalCost += best.getCostUSD();
                totalMin  += best.getDurationMin();
                int m = best.getDurationMin();
                rows.add(new RouteRow(from, fromCity, to, toCity,
                        String.format("$%,d", best.getCostUSD()),
                        String.format("%dh %02dm", m / 60, m % 60)));
            } else {
                rows.add(new RouteRow(from, fromCity, to, toCity, "—", "—"));
            }
        }

        table.setItems(rows);
        int finalMin = totalMin;
        totalLbl.setText(String.format(
                "Total: %d stop(s)   •   Cost: $%,d   •   Duration: %dh %02dm",
                path.size() - 2, totalCost, finalMin / 60, finalMin % 60));
    }

    /** Returns the cheapest or fastest edge between from→to. */
    private Edge bestEdge(String from, String to, boolean byCost) {
        Edge best = null;
        for (Edge e : graph.getEdges(from)) {
            if (!e.getDestination().equals(to)) continue;
            if (best == null) { best = e; continue; }
            if (byCost  && e.getCostUSD()    < best.getCostUSD())    best = e;
            if (!byCost && e.getDurationMin() < best.getDurationMin()) best = e;
        }
        return best;
    }

    // ==================================================================
    // LAYOUT HELPERS
    // ==================================================================

    /** Standard tab layout: [inputRow] [errLbl] [table fills remaining] [summaryLbl] */
    private VBox tabLayout(HBox inputRow, Label errLbl,
                           TableView<?> table, Label summaryLbl) {
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox box = new VBox(12, inputRow, errLbl, table, summaryLbl);
        box.setPadding(new Insets(20));
        return box;
    }

    private HBox inputRow(javafx.scene.Node... nodes) {
        HBox row = new HBox(10, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Editable combo-box pre-loaded with all IATA codes. */
    private ComboBox<String> airportCombo(String prompt) {
        ComboBox<String> cb = new ComboBox<>(
                FXCollections.observableArrayList(sortedIataCodes));
        cb.setEditable(true);
        cb.setPromptText(prompt + " IATA");
        cb.setPrefWidth(140);

        // Live filter as user types
        cb.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            String upper = newVal.toUpperCase();
            ObservableList<String> filtered = FXCollections.observableArrayList(
                    sortedIataCodes.stream()
                            .filter(s -> s.startsWith(upper))
                            .collect(Collectors.toList()));
            cb.setItems(filtered);
            if (!filtered.isEmpty()) cb.show();
        });

        return cb;
    }

    /** Returns the selected/typed IATA code, upper-cased, or null if blank. */
    private String selectedIata(ComboBox<String> cb) {
        String val = cb.getValue();
        if (val == null || val.isBlank()) return null;
        String upper = val.trim().toUpperCase();
        return graph.containsAirport(upper) ? upper : null;
    }

    /** Validates a (src, dst) pair and writes error to errLbl. Returns true if OK. */
    private boolean validatePair(String src, String dst, Label errLbl) {
        if (src == null) { errLbl.setText("Origin airport not found. Check IATA code."); return false; }
        if (dst == null) { errLbl.setText("Destination airport not found. Check IATA code."); return false; }
        if (src.equals(dst)) { errLbl.setText("Origin and destination must be different."); return false; }
        return true;
    }

    // Widget factories
    private Button primaryButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + CLR_BLUE + "; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 7 18 7 18; -fx-cursor: hand;");
        return b;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        return l;
    }

    private Label boldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 14));
        return l;
    }

    private Label errorLabel() {
        Label l = new Label();
        l.setTextFill(Color.web(CLR_ERROR));
        l.setFont(Font.font("System", 12));
        return l;
    }

    private Label summaryLabel() {
        Label l = new Label();
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        l.setTextFill(Color.web(CLR_ACCENT));
        return l;
    }

    // Generic string-column factory
    private <T> TableColumn<T, String> col(String header, String property, int width) {
        TableColumn<T, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }

    // Generic int-column factory
    private <T> TableColumn<T, Integer> colInt(String header, String property, int width) {
        TableColumn<T, Integer> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }

    // Error dialog shown when data files are missing
    private void showFatalError(Stage stage, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Startup Error");
        alert.setHeaderText("Could not load flight data.");
        alert.setContentText(
                "Make sure airports.csv and routes.csv exist under the data/ folder.\n\n" + msg);
        alert.showAndWait();
        Platform.exit();
    }

    // ==================================================================
    // ROW MODEL CLASSES  (JavaFX TableView needs JavaBean properties)
    // ==================================================================

    public static class RouteRow {
        private final SimpleStringProperty from, fromCity, to, toCity, cost, duration;
        public RouteRow(String from, String fromCity, String to, String toCity,
                        String cost, String duration) {
            this.from     = new SimpleStringProperty(from);
            this.fromCity = new SimpleStringProperty(fromCity);
            this.to       = new SimpleStringProperty(to);
            this.toCity   = new SimpleStringProperty(toCity);
            this.cost     = new SimpleStringProperty(cost);
            this.duration = new SimpleStringProperty(duration);
        }
        public String getFrom()     { return from.get(); }
        public String getFromCity() { return fromCity.get(); }
        public String getTo()       { return to.get(); }
        public String getToCity()   { return toCity.get(); }
        public String getCost()     { return cost.get(); }
        public String getDuration() { return duration.get(); }
    }

    public static class BfsRow {
        private final SimpleStringProperty  iata, city, country;
        private final SimpleIntegerProperty hops;
        public BfsRow(String iata, String city, String country, int hops) {
            this.iata    = new SimpleStringProperty(iata);
            this.city    = new SimpleStringProperty(city);
            this.country = new SimpleStringProperty(country);
            this.hops    = new SimpleIntegerProperty(hops);
        }
        public String  getIata()    { return iata.get(); }
        public String  getCity()    { return city.get(); }
        public String  getCountry() { return country.get(); }
        public int     getHops()    { return hops.get(); }
    }

    public static class ArtRow {
        private final SimpleStringProperty iata, city, country, routes;
        public ArtRow(String iata, String city, String country, String routes) {
            this.iata    = new SimpleStringProperty(iata);
            this.city    = new SimpleStringProperty(city);
            this.country = new SimpleStringProperty(country);
            this.routes  = new SimpleStringProperty(routes);
        }
        public String getIata()    { return iata.get(); }
        public String getCity()    { return city.get(); }
        public String getCountry() { return country.get(); }
        public String getRoutes()  { return routes.get(); }
    }

    public static class MstRow {
        private final SimpleIntegerProperty rank;
        private final SimpleStringProperty  src, dest, cost;
        public MstRow(int rank, String src, String dest, String cost) {
            this.rank = new SimpleIntegerProperty(rank);
            this.src  = new SimpleStringProperty(src);
            this.dest = new SimpleStringProperty(dest);
            this.cost = new SimpleStringProperty(cost);
        }
        public int    getRank() { return rank.get(); }
        public String getSrc()  { return src.get(); }
        public String getDest() { return dest.get(); }
        public String getCost() { return cost.get(); }
    }

    public static class BudgetRow {
        private final SimpleStringProperty iata, city, country, cost;
        public BudgetRow(String iata, String city, String country, String cost) {
            this.iata    = new SimpleStringProperty(iata);
            this.city    = new SimpleStringProperty(city);
            this.country = new SimpleStringProperty(country);
            this.cost    = new SimpleStringProperty(cost);
        }
        public String getIata()    { return iata.get(); }
        public String getCity()    { return city.get(); }
        public String getCountry() { return country.get(); }
        public String getCost()    { return cost.get(); }
    }

    public static class EdgeRow {
        private final SimpleStringProperty dest, cost, duration;
        public EdgeRow(String dest, String cost, String duration) {
            this.dest     = new SimpleStringProperty(dest);
            this.cost     = new SimpleStringProperty(cost);
            this.duration = new SimpleStringProperty(duration);
        }
        public String getDest()     { return dest.get(); }
        public String getCost()     { return cost.get(); }
        public String getDuration() { return duration.get(); }
    }

    public static class HubRow {
        private final SimpleIntegerProperty rank, degree;
        private final SimpleStringProperty  iata, city, country;
        public HubRow(int rank, String iata, String city, String country, int degree) {
            this.rank    = new SimpleIntegerProperty(rank);
            this.iata    = new SimpleStringProperty(iata);
            this.city    = new SimpleStringProperty(city);
            this.country = new SimpleStringProperty(country);
            this.degree  = new SimpleIntegerProperty(degree);
        }
        public int    getRank()    { return rank.get(); }
        public String getIata()    { return iata.get(); }
        public String getCity()    { return city.get(); }
        public String getCountry() { return country.get(); }
        public int    getDegree()  { return degree.get(); }
    }
}
