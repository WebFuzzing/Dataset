package com.programacion3.adoptme.service;

import com.programacion3.adoptme.service.MSTService.Edge;
import com.programacion3.adoptme.service.MSTService.MSTResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MSTService (Kruskal & Prim) Unit Tests")
class MSTServiceTest {

    private MSTService mstService;

    @BeforeEach
    void setUp() {
        mstService = new MSTService();
    }

    private List<Edge> createSimpleGraph() {
        /*
         * Simple graph:
         *   A --5-- B
         *   |       |
         *   10      3
         *   |       |
         *   C --2-- D
         *
         * MST should be: A-B (5), B-D (3), C-D (2) = 10
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0, "NEAR"));
        edges.add(new Edge("A", "C", 10.0, "NEAR"));
        edges.add(new Edge("B", "D", 3.0, "NEAR"));
        edges.add(new Edge("C", "D", 2.0, "NEAR"));
        return edges;
    }

    private List<Edge> createComplexGraph() {
        /*
         * Complex graph:
         *   A --1-- B --4-- E
         *   |       |       |
         *   2       1       2
         *   |       |       |
         *   C --3-- D --5-- F
         *   |               |
         *   6-------8-------G
         *
         * MST edges: A-B(1), B-D(1), A-C(2), E-F(2), C-D(3), C-G(6) = 15
         * (or similar with same total weight)
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 1.0, "NEAR"));
        edges.add(new Edge("A", "C", 2.0, "NEAR"));
        edges.add(new Edge("B", "D", 1.0, "NEAR"));
        edges.add(new Edge("B", "E", 4.0, "NEAR"));
        edges.add(new Edge("C", "D", 3.0, "NEAR"));
        edges.add(new Edge("C", "G", 6.0, "NEAR"));
        edges.add(new Edge("D", "F", 5.0, "NEAR"));
        edges.add(new Edge("E", "F", 2.0, "NEAR"));
        edges.add(new Edge("F", "G", 8.0, "NEAR"));
        return edges;
    }

    private List<Edge> createDisconnectedGraph() {
        /*
         * Disconnected graph:
         *   A --5-- B       C --3-- D
         *
         * Can only create MST for connected components
         */
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0, "NEAR"));
        edges.add(new Edge("C", "D", 3.0, "NEAR"));
        return edges;
    }

    // ==================== Kruskal Tests ====================

    @Test
    @DisplayName("Kruskal: Simple graph MST")
    void testKruskalSimpleGraph() {
        // Arrange
        List<Edge> edges = createSimpleGraph();
        Set<String> nodes = Set.of("A", "B", "C", "D");

        // Act
        MSTResult result = mstService.compute(nodes, edges);

        // Assert
        assertEquals(10.0, result.totalWeight, 0.01, "Total MST weight should be 10");
        assertEquals(3, result.edges.size(), "MST should have n-1 edges for n nodes");

        // Verify no cycles (MST property)
        assertTrue(isValidMST(result.edges, nodes), "Result should be a valid spanning tree");
    }

    @Test
    @DisplayName("Kruskal: Complex graph MST")
    void testKruskalComplexGraph() {
        // Arrange
        List<Edge> edges = createComplexGraph();
        Set<String> nodes = Set.of("A", "B", "C", "D", "E", "F", "G");

        // Act
        MSTResult result = mstService.compute(nodes, edges);

        // Assert
        assertEquals(15.0, result.totalWeight, 0.01, "Total MST weight should be 15");
        assertEquals(6, result.edges.size(), "MST should have 6 edges for 7 nodes");
        assertTrue(isValidMST(result.edges, nodes), "Result should be a valid spanning tree");
    }

    @Test
    @DisplayName("Kruskal: Empty graph")
    void testKruskalEmptyGraph() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        Set<String> nodes = new HashSet<>();

        // Act
        MSTResult result = mstService.compute(nodes, edges);

        // Assert
        assertEquals(0.0, result.totalWeight, 0.01);
        assertTrue(result.edges.isEmpty());
    }

    @Test
    @DisplayName("Kruskal: Single node")
    void testKruskalSingleNode() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        Set<String> nodes = Set.of("A");

        // Act
        MSTResult result = mstService.compute(nodes, edges);

        // Assert
        assertEquals(0.0, result.totalWeight, 0.01);
        assertTrue(result.edges.isEmpty(), "Single node has no edges in MST");
    }

    @Test
    @DisplayName("Kruskal: Two nodes connected")
    void testKruskalTwoNodes() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0, "NEAR"));
        Set<String> nodes = Set.of("A", "B");

        // Act
        MSTResult result = mstService.compute(nodes, edges);

        // Assert
        assertEquals(5.0, result.totalWeight, 0.01);
        assertEquals(1, result.edges.size());
    }

    @Test
    @DisplayName("Kruskal: Disconnected graph (subset)")
    void testKruskalDisconnectedSubset() {
        // Arrange
        List<Edge> edges = createDisconnectedGraph();
        Set<String> nodes = Set.of("A", "B"); // Only connected component

        // Act
        MSTResult result = mstService.compute(nodes, edges);

        // Assert
        assertEquals(5.0, result.totalWeight, 0.01);
        assertEquals(1, result.edges.size());
    }

    @Test
    @DisplayName("Kruskal: Filters non-NEAR edges")
    void testKruskalFiltersEdgeTypes() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0, "NEAR"));
        edges.add(new Edge("B", "C", 3.0, "OTHER")); // Wrong type, should be ignored
        edges.add(new Edge("A", "C", 10.0, "NEAR"));
        Set<String> nodes = Set.of("A", "B", "C");

        // Act
        MSTResult result = mstService.compute(nodes, edges);

        // Assert
        // Should use A-B (5) and A-C (10) = 15, ignoring B-C (OTHER type)
        assertEquals(15.0, result.totalWeight, 0.01);
        assertEquals(2, result.edges.size());
    }

    // ==================== Prim Tests ====================

    @Test
    @DisplayName("Prim: Simple graph MST")
    void testPrimSimpleGraph() {
        // Arrange
        List<Edge> edges = createSimpleGraph();
        Set<String> nodes = Set.of("A", "B", "C", "D");

        // Act
        MSTResult result = mstService.computeWithPrim(nodes, edges);

        // Assert
        assertEquals(10.0, result.totalWeight, 0.01, "Total MST weight should be 10");
        assertEquals(3, result.edges.size(), "MST should have n-1 edges for n nodes");
        assertTrue(isValidMST(result.edges, nodes), "Result should be a valid spanning tree");
    }

    @Test
    @DisplayName("Prim: Complex graph MST")
    void testPrimComplexGraph() {
        // Arrange
        List<Edge> edges = createComplexGraph();
        Set<String> nodes = Set.of("A", "B", "C", "D", "E", "F", "G");

        // Act
        MSTResult result = mstService.computeWithPrim(nodes, edges);

        // Assert
        assertEquals(15.0, result.totalWeight, 0.01, "Total MST weight should be 15");
        assertEquals(6, result.edges.size(), "MST should have 6 edges for 7 nodes");
        assertTrue(isValidMST(result.edges, nodes), "Result should be a valid spanning tree");
    }

    @Test
    @DisplayName("Prim: Empty graph")
    void testPrimEmptyGraph() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        Set<String> nodes = new HashSet<>();

        // Act
        MSTResult result = mstService.computeWithPrim(nodes, edges);

        // Assert
        assertEquals(0.0, result.totalWeight, 0.01);
        assertTrue(result.edges.isEmpty());
    }

    @Test
    @DisplayName("Prim: Single node")
    void testPrimSingleNode() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        Set<String> nodes = Set.of("A");

        // Act
        MSTResult result = mstService.computeWithPrim(nodes, edges);

        // Assert
        assertEquals(0.0, result.totalWeight, 0.01);
        assertTrue(result.edges.isEmpty(), "Single node has no edges in MST");
    }

    @Test
    @DisplayName("Prim: Two nodes connected")
    void testPrimTwoNodes() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0, "NEAR"));
        Set<String> nodes = Set.of("A", "B");

        // Act
        MSTResult result = mstService.computeWithPrim(nodes, edges);

        // Assert
        assertEquals(5.0, result.totalWeight, 0.01);
        assertEquals(1, result.edges.size());
    }

    @Test
    @DisplayName("Prim: Filters non-NEAR edges")
    void testPrimFiltersEdgeTypes() {
        // Arrange
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0, "NEAR"));
        edges.add(new Edge("B", "C", 3.0, "OTHER")); // Wrong type
        edges.add(new Edge("A", "C", 10.0, "NEAR"));
        Set<String> nodes = Set.of("A", "B", "C");

        // Act
        MSTResult result = mstService.computeWithPrim(nodes, edges);

        // Assert
        assertEquals(15.0, result.totalWeight, 0.01);
        assertEquals(2, result.edges.size());
    }

    // ==================== Kruskal vs Prim Comparison ====================

    @Test
    @DisplayName("Kruskal vs Prim: Same MST weight")
    void testKruskalVsPrimSameWeight() {
        // Arrange
        List<Edge> edges = createComplexGraph();
        Set<String> nodes = Set.of("A", "B", "C", "D", "E", "F", "G");

        // Act
        MSTResult kruskalResult = mstService.compute(nodes, edges);
        MSTResult primResult = mstService.computeWithPrim(nodes, edges);

        // Assert
        assertEquals(kruskalResult.totalWeight, primResult.totalWeight, 0.01,
                "Both algorithms should produce MST with same total weight");
        assertEquals(kruskalResult.edges.size(), primResult.edges.size(),
                "Both algorithms should have same number of edges");
    }

    @Test
    @DisplayName("Kruskal vs Prim: Both produce valid MSTs")
    void testBothProduceValidMSTs() {
        // Arrange
        List<Edge> edges = createSimpleGraph();
        Set<String> nodes = Set.of("A", "B", "C", "D");

        // Act
        MSTResult kruskalResult = mstService.compute(nodes, edges);
        MSTResult primResult = mstService.computeWithPrim(nodes, edges);

        // Assert
        assertTrue(isValidMST(kruskalResult.edges, nodes), "Kruskal should produce valid MST");
        assertTrue(isValidMST(primResult.edges, nodes), "Prim should produce valid MST");
        assertEquals(kruskalResult.totalWeight, primResult.totalWeight, 0.01);
    }

    // ==================== Weight Tests ====================

    @Test
    @DisplayName("MST: Selects minimum weight edges")
    void testSelectsMinimumWeightEdges() {
        // Arrange - Graph where wrong choice leads to higher weight
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 1.0, "NEAR"));
        edges.add(new Edge("B", "C", 2.0, "NEAR"));
        edges.add(new Edge("C", "D", 3.0, "NEAR"));
        edges.add(new Edge("A", "D", 100.0, "NEAR")); // Expensive shortcut
        Set<String> nodes = Set.of("A", "B", "C", "D");

        // Act
        MSTResult kruskalResult = mstService.compute(nodes, edges);
        MSTResult primResult = mstService.computeWithPrim(nodes, edges);

        // Assert - Should choose 1+2+3=6, not include the 100 edge
        assertEquals(6.0, kruskalResult.totalWeight, 0.01);
        assertEquals(6.0, primResult.totalWeight, 0.01);
    }

    @Test
    @DisplayName("MST: Handles equal weight edges")
    void testEqualWeightEdges() {
        // Arrange - Multiple edges with same weight
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge("A", "B", 5.0, "NEAR"));
        edges.add(new Edge("B", "C", 5.0, "NEAR"));
        edges.add(new Edge("C", "D", 5.0, "NEAR"));
        edges.add(new Edge("A", "D", 5.0, "NEAR"));
        Set<String> nodes = Set.of("A", "B", "C", "D");

        // Act
        MSTResult result = mstService.compute(nodes, edges);

        // Assert
        assertEquals(15.0, result.totalWeight, 0.01, "Should select any 3 edges");
        assertEquals(3, result.edges.size());
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Kruskal: Handles larger graphs")
    void testKruskalLargerGraph() {
        // Arrange - Create a complete graph of 10 nodes
        List<Edge> edges = new ArrayList<>();
        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            nodes.add("N" + i);
            for (int j = i + 1; j < 10; j++) {
                edges.add(new Edge("N" + i, "N" + j, i + j + 1.0, "NEAR"));
            }
        }

        // Act
        long start = System.currentTimeMillis();
        MSTResult result = mstService.compute(nodes, edges);
        long duration = System.currentTimeMillis() - start;

        // Assert
        assertEquals(9, result.edges.size(), "Should have n-1 edges for n nodes");
        assertTrue(result.totalWeight > 0);
        assertTrue(duration < 1000, "Should complete within 1 second");
    }

    @Test
    @DisplayName("Prim: Handles larger graphs")
    void testPrimLargerGraph() {
        // Arrange - Create a complete graph of 10 nodes
        List<Edge> edges = new ArrayList<>();
        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            nodes.add("N" + i);
            for (int j = i + 1; j < 10; j++) {
                edges.add(new Edge("N" + i, "N" + j, i + j + 1.0, "NEAR"));
            }
        }

        // Act
        long start = System.currentTimeMillis();
        MSTResult result = mstService.computeWithPrim(nodes, edges);
        long duration = System.currentTimeMillis() - start;

        // Assert
        assertEquals(9, result.edges.size(), "Should have n-1 edges for n nodes");
        assertTrue(result.totalWeight > 0);
        assertTrue(duration < 1000, "Should complete within 1 second");
    }

    // ==================== Helper Methods ====================

    /**
     * Validates that the result is a valid MST:
     * - Has exactly n-1 edges for n nodes
     * - Is connected (all nodes reachable)
     * - Has no cycles
     */
    private boolean isValidMST(List<Edge> edges, Set<String> nodes) {
        if (nodes.isEmpty()) return edges.isEmpty();
        if (edges.size() != nodes.size() - 1) return false;

        // Build adjacency list
        Map<String, List<String>> adj = new HashMap<>();
        for (String node : nodes) {
            adj.put(node, new ArrayList<>());
        }
        for (Edge e : edges) {
            adj.get(e.a).add(e.b);
            adj.get(e.b).add(e.a);
        }

        // Check connectivity using BFS
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        String start = nodes.iterator().next();
        queue.offer(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : adj.get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }

        return visited.size() == nodes.size();
    }
}
