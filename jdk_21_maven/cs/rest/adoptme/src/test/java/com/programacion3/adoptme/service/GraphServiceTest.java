package com.programacion3.adoptme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GraphService Unit Tests")
class GraphServiceTest {

    private GraphService graphService;

    @Mock
    private GraphLoader graphLoader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        graphService = new GraphService(graphLoader);
    }

    private Map<String, List<String>> createTestGraph() {
        /*
         * Test graph structure:
         *   A -> B -> C
         *   |         |
         *   v         v
         *   D -> E -> F
         *   |
         *   v
         *   G (isolated: H)
         */
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("B", "D"));
        graph.put("B", Arrays.asList("C"));
        graph.put("C", Arrays.asList("F"));
        graph.put("D", Arrays.asList("E", "G"));
        graph.put("E", Arrays.asList("F"));
        graph.put("F", new ArrayList<>());
        graph.put("G", new ArrayList<>());
        graph.put("H", new ArrayList<>()); // Isolated node
        return graph;
    }

    private Map<String, List<String>> createComplexGraph() {
        /*
         * Complex graph with multiple paths:
         *   A -> B -> D
         *   |    |    |
         *   v    v    v
         *   C -> E -> F
         *        |
         *        v
         *        G
         */
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("B", "C"));
        graph.put("B", Arrays.asList("D", "E"));
        graph.put("C", Arrays.asList("E"));
        graph.put("D", Arrays.asList("F"));
        graph.put("E", Arrays.asList("F", "G"));
        graph.put("F", new ArrayList<>());
        graph.put("G", new ArrayList<>());
        return graph;
    }

    // ==================== BFS Tests ====================

    @Test
    @DisplayName("BFS: Simple path A->B")
    void testBfsSimplePath() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.bfsPath("A", "B");

        // Assert
        assertNotNull(path);
        assertEquals(2, path.size());
        assertEquals("A", path.get(0));
        assertEquals("B", path.get(1));
    }

    @Test
    @DisplayName("BFS: Path A->F (multiple hops)")
    void testBfsMultipleHops() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.bfsPath("A", "F");

        // Assert
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals("A", path.get(0));
        assertEquals("F", path.get(path.size() - 1));

        // BFS should find shortest path (3 hops: A->B->C->F or A->D->E->F)
        assertEquals(4, path.size());
    }

    @Test
    @DisplayName("BFS: No path to isolated node")
    void testBfsNoPath() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.bfsPath("A", "H");

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty(), "Should return empty list when no path exists");
    }

    @Test
    @DisplayName("BFS: Same source and destination")
    void testBfsSameNode() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.bfsPath("A", "A");

        // Assert
        assertNotNull(path);
        assertEquals(1, path.size());
        assertEquals("A", path.get(0));
    }

    @Test
    @DisplayName("BFS: Null source returns empty")
    void testBfsNullSource() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.bfsPath(null, "B");

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    @DisplayName("BFS: Null destination returns empty")
    void testBfsNullDestination() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.bfsPath("A", null);

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    @DisplayName("BFS: Blank source returns empty")
    void testBfsBlankSource() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.bfsPath("  ", "B");

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    @DisplayName("BFS: Finds shortest path in graph with multiple routes")
    void testBfsShortestPath() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createComplexGraph());

        // Act
        List<String> path = graphService.bfsPath("A", "F");

        // Assert
        assertNotNull(path);
        assertEquals("A", path.get(0));
        assertEquals("F", path.get(path.size() - 1));

        // Should find shortest: A->B->D->F (4 nodes) or A->B->E->F (4 nodes)
        assertEquals(4, path.size());
    }

    @Test
    @DisplayName("BFS: Path from intermediate node")
    void testBfsFromIntermediateNode() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.bfsPath("D", "F");

        // Assert
        assertNotNull(path);
        assertEquals("D", path.get(0));
        assertEquals("F", path.get(path.size() - 1));
        assertEquals(3, path.size()); // D->E->F
    }

    // ==================== DFS Tests ====================

    @Test
    @DisplayName("DFS: Simple path A->B")
    void testDfsSimplePath() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.dfsPath("A", "B");

        // Assert
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals("A", path.get(0));
        assertEquals("B", path.get(path.size() - 1));
    }

    @Test
    @DisplayName("DFS: Path A->F (deep search)")
    void testDfsDeepPath() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.dfsPath("A", "F");

        // Assert
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals("A", path.get(0));
        assertEquals("F", path.get(path.size() - 1));

        // DFS finds A path, not necessarily shortest
        assertTrue(path.size() >= 4); // At least 4 nodes
    }

    @Test
    @DisplayName("DFS: No path to isolated node")
    void testDfsNoPath() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.dfsPath("A", "H");

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty(), "Should return empty list when no path exists");
    }

    @Test
    @DisplayName("DFS: Same source and destination")
    void testDfsSameNode() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.dfsPath("A", "A");

        // Assert
        assertNotNull(path);
        assertEquals(1, path.size());
        assertEquals("A", path.get(0));
    }

    @Test
    @DisplayName("DFS: Null source returns empty")
    void testDfsNullSource() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.dfsPath(null, "B");

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    @DisplayName("DFS: Path from intermediate node")
    void testDfsFromIntermediateNode() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createTestGraph());

        // Act
        List<String> path = graphService.dfsPath("D", "G");

        // Assert
        assertNotNull(path);
        assertEquals("D", path.get(0));
        assertEquals("G", path.get(path.size() - 1));
    }

    @Test
    @DisplayName("DFS: Finds path in complex graph")
    void testDfsComplexGraph() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createComplexGraph());

        // Act
        List<String> path = graphService.dfsPath("A", "G");

        // Assert
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals("A", path.get(0));
        assertEquals("G", path.get(path.size() - 1));

        // Verify path is valid (each consecutive pair has edge)
        Map<String, List<String>> graph = createComplexGraph();
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            assertTrue(graph.get(from).contains(to),
                    "Invalid path: no edge from " + from + " to " + to);
        }
    }

    @Test
    @DisplayName("DFS: Empty graph returns empty path")
    void testDfsEmptyGraph() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(new HashMap<>());

        // Act
        List<String> path = graphService.dfsPath("A", "B");

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    @DisplayName("BFS: Empty graph returns empty path")
    void testBfsEmptyGraph() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(new HashMap<>());

        // Act
        List<String> path = graphService.bfsPath("A", "B");

        // Assert
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    // ==================== Comparison Tests ====================

    @Test
    @DisplayName("BFS vs DFS: Both find path but may differ")
    void testBfsVsDfs() {
        // Arrange
        when(graphLoader.loadAdjacency()).thenReturn(createComplexGraph());

        // Act
        List<String> bfsPath = graphService.bfsPath("A", "F");
        List<String> dfsPath = graphService.dfsPath("A", "F");

        // Assert
        assertNotNull(bfsPath);
        assertNotNull(dfsPath);
        assertFalse(bfsPath.isEmpty());
        assertFalse(dfsPath.isEmpty());

        // Both should start at A and end at F
        assertEquals("A", bfsPath.get(0));
        assertEquals("F", bfsPath.get(bfsPath.size() - 1));
        assertEquals("A", dfsPath.get(0));
        assertEquals("F", dfsPath.get(dfsPath.size() - 1));

        // BFS should find shortest or equal path
        assertTrue(bfsPath.size() <= dfsPath.size(),
                "BFS should find shortest path: BFS=" + bfsPath.size() + " vs DFS=" + dfsPath.size());
    }
}
