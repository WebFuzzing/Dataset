package com.programacion3.adoptme.service;

import com.programacion3.adoptme.service.ShortestPathService.Edge;
import com.programacion3.adoptme.service.ShortestPathService.PathResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShortestPathService (Dijkstra) Unit Tests")
class ShortestPathServiceTest {

    private ShortestPathService shortestPathService;

    @BeforeEach
    void setUp() {
        shortestPathService = new ShortestPathService();
    }

    private List<Edge> createSimpleGraph() {
        /*
         * Simple graph:
         *   A --5-- B
         *   |       |
         *   10      3
         *   |       |
         *   C --2-- D
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0));
        edges.add(new Edge("A", "C", 10.0));
        edges.add(new Edge("B", "D", 3.0));
        edges.add(new Edge("C", "D", 2.0));
        return edges;
    }

    private List<Edge> createComplexGraph() {
        /*
         * Complex graph with multiple paths:
         *   A --1-- B --4-- E
         *   |       |       |
         *   2       1       2
         *   |       |       |
         *   C --3-- D --5-- F
         *   |               |
         *   6-------8-------G
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 1.0));
        edges.add(new Edge("A", "C", 2.0));
        edges.add(new Edge("B", "D", 1.0));
        edges.add(new Edge("B", "E", 4.0));
        edges.add(new Edge("C", "D", 3.0));
        edges.add(new Edge("C", "G", 6.0));
        edges.add(new Edge("D", "F", 5.0));
        edges.add(new Edge("E", "F", 2.0));
        edges.add(new Edge("G", "F", 8.0));
        return edges;
    }

    private List<Edge> createDisconnectedGraph() {
        /*
         * Disconnected graph:
         *   A --5-- B
         *
         *   C --3-- D
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0));
        edges.add(new Edge("C", "D", 3.0));
        return edges;
    }

    // ==================== Basic Path Tests ====================

    @Test
    @DisplayName("Dijkstra: Direct edge A->B")
    void testDirectPath() {
        // Arrange
        List<Edge> edges = createSimpleGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "B", edges);

        // Assert
        assertEquals(5.0, result.cost, 0.01);
        assertEquals(2, result.path.size());
        assertEquals("A", result.path.get(0));
        assertEquals("B", result.path.get(1));
    }

    @Test
    @DisplayName("Dijkstra: Shortest path A->D (through B, not C)")
    void testShortestPathChoice() {
        // Arrange
        List<Edge> edges = createSimpleGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "D", edges);

        // Assert
        // Should choose A->B->D (5+3=8) instead of A->C->D (10+2=12)
        assertEquals(8.0, result.cost, 0.01);
        assertEquals(3, result.path.size());
        assertEquals("A", result.path.get(0));
        assertEquals("B", result.path.get(1));
        assertEquals("D", result.path.get(2));
    }

    @Test
    @DisplayName("Dijkstra: Same start and goal")
    void testSameNode() {
        // Arrange
        List<Edge> edges = createSimpleGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "A", edges);

        // Assert
        assertEquals(0.0, result.cost, 0.01);
        assertEquals(1, result.path.size());
        assertEquals("A", result.path.get(0));
    }

    @Test
    @DisplayName("Dijkstra: No path to disconnected node")
    void testNoPath() {
        // Arrange
        List<Edge> edges = createDisconnectedGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "C", edges);

        // Assert
        assertEquals(Double.POSITIVE_INFINITY, result.cost);
        assertTrue(result.path.isEmpty(), "Path should be empty when unreachable");
    }

    // ==================== Complex Graph Tests ====================

    @Test
    @DisplayName("Dijkstra: Complex graph A->F")
    void testComplexGraphShortestPath() {
        // Arrange
        List<Edge> edges = createComplexGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "F", edges);

        // Assert
        // Shortest: A->B->E->F = 1+4+2 = 7
        // Alternative: A->B->D->F = 1+1+5 = 7
        // Alternative: A->C->D->F = 2+3+5 = 10
        assertEquals(7.0, result.cost, 0.01);
        assertFalse(result.path.isEmpty());
        assertEquals("A", result.path.get(0));
        assertEquals("F", result.path.get(result.path.size() - 1));
    }

    @Test
    @DisplayName("Dijkstra: Complex graph A->G")
    void testComplexGraphLongerPath() {
        // Arrange
        List<Edge> edges = createComplexGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "G", edges);

        // Assert
        // Shortest: A->C->G = 2+6 = 8
        // Alternative: A->B->E->F->G = 1+4+2+8 = 15
        assertEquals(8.0, result.cost, 0.01);
        assertEquals("A", result.path.get(0));
        assertEquals("G", result.path.get(result.path.size() - 1));
    }

    @Test
    @DisplayName("Dijkstra: Path from intermediate node")
    void testFromIntermediateNode() {
        // Arrange
        List<Edge> edges = createComplexGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("C", "E", edges);

        // Assert
        // Shortest: C->D->B->E or similar
        assertFalse(result.path.isEmpty());
        assertEquals("C", result.path.get(0));
        assertEquals("E", result.path.get(result.path.size() - 1));
        assertTrue(result.cost > 0);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Dijkstra: Empty edge collection")
    void testEmptyEdges() {
        // Arrange
        List<Edge> edges = new ArrayList<>();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "B", edges);

        // Assert
        assertEquals(Double.POSITIVE_INFINITY, result.cost);
        assertTrue(result.path.isEmpty());
    }

    @Test
    @DisplayName("Dijkstra: Single node (no edges)")
    void testSingleNodeNoEdges() {
        // Arrange
        List<Edge> edges = new ArrayList<>();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "A", edges);

        // Assert
        assertEquals(0.0, result.cost, 0.01);
        assertEquals(1, result.path.size());
        assertEquals("A", result.path.get(0));
    }

    @Test
    @DisplayName("Dijkstra: Nonexistent start node")
    void testNonexistentStartNode() {
        // Arrange
        List<Edge> edges = createSimpleGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("Z", "A", edges);

        // Assert
        assertEquals(Double.POSITIVE_INFINITY, result.cost);
        assertTrue(result.path.isEmpty());
    }

    @Test
    @DisplayName("Dijkstra: Nonexistent goal node")
    void testNonexistentGoalNode() {
        // Arrange
        List<Edge> edges = createSimpleGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "Z", edges);

        // Assert
        assertEquals(Double.POSITIVE_INFINITY, result.cost);
        assertTrue(result.path.isEmpty());
    }

    // ==================== Weight Tests ====================

    @Test
    @DisplayName("Dijkstra: Handles different weights correctly")
    void testDifferentWeights() {
        // Arrange - Graph with varying weights
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 100.0));
        edges.add(new Edge("A", "C", 50.0));
        edges.add(new Edge("C", "D", 20.0));
        edges.add(new Edge("D", "B", 10.0));

        // Act
        PathResult result = shortestPathService.shortestPath("A", "B", edges);

        // Assert
        // Should choose A->C->D->B (50+20+10=80) instead of A->B (100)
        assertEquals(80.0, result.cost, 0.01);
        assertEquals(4, result.path.size());
    }

    @Test
    @DisplayName("Dijkstra: Handles small decimal weights")
    void testDecimalWeights() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 0.5));
        edges.add(new Edge("B", "C", 0.7));
        edges.add(new Edge("A", "C", 1.5));

        // Act
        PathResult result = shortestPathService.shortestPath("A", "C", edges);

        // Assert
        // Should choose A->B->C (0.5+0.7=1.2) instead of A->C (1.5)
        assertEquals(1.2, result.cost, 0.01);
        assertEquals(3, result.path.size());
    }

    // ==================== Bidirectional Graph Tests ====================

    @Test
    @DisplayName("Dijkstra: Works bidirectionally (service adds reverse edges)")
    void testBidirectionalPath() {
        // Arrange - Only add edges in one direction
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0));
        edges.add(new Edge("B", "C", 3.0));

        // Act - Try to go backwards
        PathResult resultForward = shortestPathService.shortestPath("A", "C", edges);
        PathResult resultBackward = shortestPathService.shortestPath("C", "A", edges);

        // Assert - Should work both ways (service creates bidirectional graph)
        assertEquals(8.0, resultForward.cost, 0.01);
        assertEquals(8.0, resultBackward.cost, 0.01);
        assertFalse(resultForward.path.isEmpty());
        assertFalse(resultBackward.path.isEmpty());
    }

    // ==================== Path Validation Tests ====================

    @Test
    @DisplayName("Dijkstra: Path is valid and continuous")
    void testPathContinuity() {
        // Arrange
        List<Edge> edges = createComplexGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "F", edges);

        // Assert
        assertFalse(result.path.isEmpty());

        // Build adjacency for validation (bidirectional)
        Map<String, Set<String>> adj = new HashMap<>();
        for (Edge e : edges) {
            adj.computeIfAbsent(e.from, k -> new HashSet<>()).add(e.to);
            adj.computeIfAbsent(e.to, k -> new HashSet<>()).add(e.from);
        }

        // Verify each consecutive pair in path has an edge
        for (int i = 0; i < result.path.size() - 1; i++) {
            String from = result.path.get(i);
            String to = result.path.get(i + 1);
            assertTrue(adj.containsKey(from) && adj.get(from).contains(to),
                    "Invalid path: no edge between " + from + " and " + to);
        }
    }

    @Test
    @DisplayName("Dijkstra: Cost matches path edges")
    void testCostMatchesPath() {
        // Arrange
        List<Edge> edges = createSimpleGraph();

        // Act
        PathResult result = shortestPathService.shortestPath("A", "D", edges);

        // Assert
        // Build edge weight map (bidirectional)
        Map<String, Map<String, Double>> weights = new HashMap<>();
        for (Edge e : edges) {
            weights.computeIfAbsent(e.from, k -> new HashMap<>()).put(e.to, e.weight);
            weights.computeIfAbsent(e.to, k -> new HashMap<>()).put(e.from, e.weight);
        }

        // Calculate cost from path
        double calculatedCost = 0.0;
        for (int i = 0; i < result.path.size() - 1; i++) {
            String from = result.path.get(i);
            String to = result.path.get(i + 1);
            calculatedCost += weights.get(from).get(to);
        }

        assertEquals(result.cost, calculatedCost, 0.01,
                "Reported cost should match sum of edge weights in path");
    }

    // ==================== Large Graph Performance ====================

    @Test
    @DisplayName("Dijkstra: Handles larger graphs efficiently")
    void testLargerGraph() {
        // Arrange - Create a chain of 20 nodes
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            String from = "N" + i;
            String to = "N" + (i + 1);
            edges.add(new Edge(from, to, 1.0));
        }

        // Act
        long startTime = System.currentTimeMillis();
        PathResult result = shortestPathService.shortestPath("N0", "N19", edges);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertEquals(19.0, result.cost, 0.01);
        assertEquals(20, result.path.size());
        assertEquals("N0", result.path.get(0));
        assertEquals("N19", result.path.get(19));
        assertTrue(duration < 1000, "Should complete within 1 second");
    }
}
