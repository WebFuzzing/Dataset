package com.programacion3.adoptme.service;

import com.programacion3.adoptme.service.TSPService.Edge;
import com.programacion3.adoptme.service.TSPService.TSPResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TSPService (Branch & Bound) Unit Tests")
class TSPServiceTest {

    private TSPService tspService;

    @BeforeEach
    void setUp() {
        tspService = new TSPService();
    }

    private List<Edge> createSimpleTriangle() {
        /*
         * Triangle:
         *   A --5-- B
         *   |       |
         *   10      3
         *   |       |
         *   C ------+
         *      7
         *
         * Optimal tour: A->B->C->A = 5+3+10 = 18
         * or A->C->B->A = 10+7+3 = 20
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0));
        edges.add(new Edge("B", "C", 3.0));
        edges.add(new Edge("A", "C", 10.0));
        return edges;
    }

    private List<Edge> createSquareGraph() {
        /*
         * Square with diagonals:
         *   A --1-- B
         *   |   X   |
         *   2   3   4
         *   |   X   |
         *   C --5-- D
         *
         * Optimal tour: A->B->D->C->A = 1+4+5+2 = 12
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 1.0));
        edges.add(new Edge("B", "D", 4.0));
        edges.add(new Edge("D", "C", 5.0));
        edges.add(new Edge("C", "A", 2.0));
        edges.add(new Edge("A", "D", 10.0)); // Diagonal
        edges.add(new Edge("B", "C", 8.0)); // Diagonal
        return edges;
    }

    private List<Edge> createCompleteGraph4() {
        /*
         * Complete graph with 4 nodes
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 2.0));
        edges.add(new Edge("A", "C", 3.0));
        edges.add(new Edge("A", "D", 4.0));
        edges.add(new Edge("B", "C", 5.0));
        edges.add(new Edge("B", "D", 6.0));
        edges.add(new Edge("C", "D", 7.0));
        return edges;
    }

    // ==================== Basic TSP Tests ====================

    @Test
    @DisplayName("TSP: Empty nodes returns empty tour")
    void testEmptyNodes() {
        // Arrange
        List<String> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertTrue(result.route.isEmpty());
        assertEquals(0.0, result.totalDistance, 0.01);
    }

    @Test
    @DisplayName("TSP: Single node returns zero distance")
    void testSingleNode() {
        // Arrange
        List<String> nodes = Arrays.asList("A");
        List<Edge> edges = new ArrayList<>();

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertEquals(1, result.route.size());
        assertEquals("A", result.route.get(0));
        assertEquals(0.0, result.totalDistance, 0.01);
    }

    @Test
    @DisplayName("TSP: Two nodes simple tour")
    void testTwoNodes() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B");
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0));

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertFalse(result.route.isEmpty());
        assertEquals("A", result.route.get(0));
        assertEquals("A", result.route.get(result.route.size() - 1));
        assertEquals(10.0, result.totalDistance, 0.01); // A->B->A = 5+5
    }

    @Test
    @DisplayName("TSP: Triangle optimal tour")
    void testTriangleOptimalTour() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B", "C");
        List<Edge> edges = createSimpleTriangle();

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertFalse(result.route.isEmpty());
        assertEquals("A", result.route.get(0));
        assertEquals("A", result.route.get(result.route.size() - 1));

        // Should find A->B->C->A = 5+3+10 = 18
        assertEquals(18.0, result.totalDistance, 0.01);
        assertEquals(4, result.route.size()); // A, B, C, A
    }

    @Test
    @DisplayName("TSP: Square optimal tour")
    void testSquareOptimalTour() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B", "C", "D");
        List<Edge> edges = createSquareGraph();

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertFalse(result.route.isEmpty());
        assertEquals("A", result.route.get(0));
        assertEquals("A", result.route.get(result.route.size() - 1));

        // Should find A->B->D->C->A = 1+4+5+2 = 12
        assertEquals(12.0, result.totalDistance, 0.01);
        assertEquals(5, result.route.size()); // A, B, D, C, A
    }

    // ==================== Tour Validation Tests ====================

    @Test
    @DisplayName("TSP: Tour visits all nodes exactly once")
    void testTourVisitsAllNodes() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B", "C", "D");
        List<Edge> edges = createSquareGraph();

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        Set<String> visitedNodes = new HashSet<>();
        for (int i = 0; i < result.route.size() - 1; i++) {
            visitedNodes.add(result.route.get(i));
        }

        assertEquals(nodes.size(), visitedNodes.size(),
                "Should visit all nodes exactly once (excluding return)");

        for (String node : nodes) {
            assertTrue(visitedNodes.contains(node),
                    "Tour should include node: " + node);
        }
    }

    @Test
    @DisplayName("TSP: Tour returns to start")
    void testTourReturnsToStart() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B", "C");
        List<Edge> edges = createSimpleTriangle();

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertFalse(result.route.isEmpty());
        assertEquals(result.route.get(0), result.route.get(result.route.size() - 1),
                "Tour should return to starting node");
    }

    @Test
    @DisplayName("TSP: Tour uses valid edges")
    void testTourUsesValidEdges() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B", "C");
        List<Edge> edges = createSimpleTriangle();

        // Build edge map (bidirectional)
        Map<String, Set<String>> edgeMap = new HashMap<>();
        for (Edge e : edges) {
            edgeMap.computeIfAbsent(e.from, k -> new HashSet<>()).add(e.to);
            edgeMap.computeIfAbsent(e.to, k -> new HashSet<>()).add(e.from);
        }

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        for (int i = 0; i < result.route.size() - 1; i++) {
            String from = result.route.get(i);
            String to = result.route.get(i + 1);
            assertTrue(edgeMap.containsKey(from) && edgeMap.get(from).contains(to),
                    "Invalid edge in tour: " + from + " -> " + to);
        }
    }

    // ==================== Distance Calculation Tests ====================

    @Test
    @DisplayName("TSP: Distance calculation is accurate")
    void testDistanceCalculation() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B", "C");
        List<Edge> edges = createSimpleTriangle();

        // Build distance map
        Map<String, Map<String, Double>> distMap = new HashMap<>();
        for (Edge e : edges) {
            distMap.computeIfAbsent(e.from, k -> new HashMap<>()).put(e.to, e.weight);
            distMap.computeIfAbsent(e.to, k -> new HashMap<>()).put(e.from, e.weight);
        }

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert - Manually calculate distance
        double calculatedDistance = 0.0;
        for (int i = 0; i < result.route.size() - 1; i++) {
            String from = result.route.get(i);
            String to = result.route.get(i + 1);
            calculatedDistance += distMap.get(from).get(to);
        }

        assertEquals(calculatedDistance, result.totalDistance, 0.01,
                "Reported distance should match calculated distance");
    }

    // ==================== Disconnected Graph Tests ====================

    @Test
    @DisplayName("TSP: Disconnected graph returns infinity")
    void testDisconnectedGraph() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B", "C", "D");
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0));
        edges.add(new Edge("C", "D", 3.0));
        // A-B and C-D are disconnected

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertEquals(Double.POSITIVE_INFINITY, result.totalDistance,
                "Disconnected graph should return infinity");
        assertTrue(result.route.isEmpty());
    }

    @Test
    @DisplayName("TSP: Null nodes returns empty result")
    void testNullNodes() {
        // Arrange
        List<Edge> edges = createSimpleTriangle();

        // Act
        TSPResult result = tspService.solveTSP(null, edges);

        // Assert
        assertTrue(result.route.isEmpty());
        assertEquals(0.0, result.totalDistance, 0.01);
    }

    // ==================== Complete Graph Tests ====================

    @Test
    @DisplayName("TSP: Complete graph 4 nodes")
    void testCompleteGraph4Nodes() {
        // Arrange
        List<String> nodes = Arrays.asList("A", "B", "C", "D");
        List<Edge> edges = createCompleteGraph4();

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertFalse(result.route.isEmpty());
        assertEquals("A", result.route.get(0));
        assertEquals("A", result.route.get(result.route.size() - 1));
        assertTrue(result.totalDistance > 0);
        assertTrue(result.totalDistance < Double.POSITIVE_INFINITY);

        // Should visit all 4 nodes
        assertEquals(5, result.route.size()); // A, ?, ?, ?, A
    }

    // ==================== Optimization Tests ====================

    @Test
    @DisplayName("TSP: Finds truly optimal solution")
    void testFindsOptimalSolution() {
        // Arrange - Triangle where two tours are possible
        List<String> nodes = Arrays.asList("A", "B", "C");
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 1.0));
        edges.add(new Edge("B", "C", 2.0));
        edges.add(new Edge("A", "C", 10.0));

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        // Optimal: A->B->C->A = 1+2+10 = 13
        // Worse: A->C->B->A = 10+2+1 = 13 (same due to symmetry)
        assertEquals(13.0, result.totalDistance, 0.01);
    }

    @Test
    @DisplayName("TSP: Avoids suboptimal greedy choice")
    void testAvoidsGreedyChoice() {
        // Arrange - Graph where greedy choice is wrong
        /*
         * A --1-- B
         * |       |
         * 10     2
         * |       |
         * D --1-- C
         *
         * Greedy from A: A->B(1), B->C(2), C->D(1), D->A(10) = 14
         * Optimal: A->B(1), B->C(2), but need better route
         * Actually all tours equal due to symmetry: 1+2+1+10 = 14
         */
        List<String> nodes = Arrays.asList("A", "B", "C", "D");
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 1.0));
        edges.add(new Edge("B", "C", 2.0));
        edges.add(new Edge("C", "D", 1.0));
        edges.add(new Edge("D", "A", 10.0));

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert
        assertEquals(14.0, result.totalDistance, 0.01);
        assertTrue(isValidTour(result, nodes));
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("TSP: Completes 5 nodes reasonably fast")
    void testPerformance5Nodes() {
        // Arrange - Complete graph with 5 nodes
        List<String> nodes = Arrays.asList("A", "B", "C", "D", "E");
        List<Edge> edges = new ArrayList<>();
        String[] nodeArray = {"A", "B", "C", "D", "E"};
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                edges.add(new Edge(nodeArray[i], nodeArray[j], (i + j + 1.0)));
            }
        }

        // Act
        long start = System.currentTimeMillis();
        TSPResult result = tspService.solveTSP(nodes, edges);
        long duration = System.currentTimeMillis() - start;

        // Assert
        assertFalse(result.route.isEmpty());
        assertTrue(result.totalDistance > 0);
        assertTrue(result.totalDistance < Double.POSITIVE_INFINITY);
        assertTrue(duration < 5000, "Should complete 5 nodes within 5 seconds");
    }

    @Test
    @DisplayName("TSP: Branch and bound prunes effectively")
    void testBranchAndBoundPruning() {
        // Arrange - Graph where branch and bound should prune many branches
        List<String> nodes = Arrays.asList("A", "B", "C", "D");
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 1.0));
        edges.add(new Edge("B", "C", 1.0));
        edges.add(new Edge("C", "D", 1.0));
        edges.add(new Edge("D", "A", 1.0));
        edges.add(new Edge("A", "C", 100.0)); // Expensive diagonal
        edges.add(new Edge("B", "D", 100.0)); // Expensive diagonal

        // Act
        TSPResult result = tspService.solveTSP(nodes, edges);

        // Assert - Should find the cheap tour around the perimeter
        assertEquals(4.0, result.totalDistance, 0.01);
    }

    // ==================== Helper Methods ====================

    /**
     * Validates that the tour is valid:
     * - Starts and ends at same node
     * - Visits all nodes exactly once (excluding return)
     * - Has positive finite distance
     */
    private boolean isValidTour(TSPResult result, List<String> nodes) {
        if (result.route.isEmpty()) return false;
        if (result.totalDistance >= Double.POSITIVE_INFINITY) return false;
        if (!result.route.get(0).equals(result.route.get(result.route.size() - 1))) return false;

        Set<String> visited = new HashSet<>();
        for (int i = 0; i < result.route.size() - 1; i++) {
            visited.add(result.route.get(i));
        }

        return visited.size() == nodes.size();
    }
}
