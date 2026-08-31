package com.programacion3.adoptme.service;

import com.programacion3.adoptme.service.ShortestPathService.Edge;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Carga la lista de adyacencias y aristas con pesos desde Neo4j
 */
@Component
public class GraphLoader {

    private final Neo4jClient neo4j;

    public GraphLoader(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    /**
     * Carga adyacencias simples (sin pesos) para BFS/DFS
     */
    public Map<String, List<String>> loadAdjacency() {
        String q = """
            MATCH (a:Shelter)-[r:NEAR]->(b:Shelter)
            RETURN a.id AS from, b.id AS to
        """;

        Map<String, List<String>> g = new HashMap<>();
        neo4j.query(q).fetch().all().forEach(row -> {
            String from = (String) row.get("from");
            String to = (String) row.get("to");
            g.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        });
        return g;
    }

    /**
     * Carga aristas con pesos (distKm) para Dijkstra y MST
     * Crea aristas bidireccionales (grafo no dirigido)
     */
    public List<Edge> loadWeightedEdges() {
        String q = """
            MATCH (a:Shelter)-[r:NEAR]->(b:Shelter)
            RETURN a.id AS from, b.id AS to, r.distKm AS weight
        """;

        List<Edge> edges = new ArrayList<>();
        neo4j.query(q).fetch().all().forEach(row -> {
            String from = (String) row.get("from");
            String to = (String) row.get("to");
            double weight = ((Number) row.get("weight")).doubleValue();

            // Agregamos la arista en ambas direcciones (grafo no dirigido)
            edges.add(new Edge(from, to, weight));
            edges.add(new Edge(to, from, weight));
        });
        return edges;
    }

    /**
     * Obtiene todos los IDs de shelters para MST
     */
    public Set<String> loadAllShelterIds() {
        String q = "MATCH (s:Shelter) RETURN s.id AS id";
        Set<String> ids = new HashSet<>();
        neo4j.query(q).fetch().all().forEach(row -> {
            ids.add((String) row.get("id"));
        });
        return ids;
    }

    /**
     * Carga aristas como MSTService.Edge para compatibilidad
     */
    public List<MSTService.Edge> loadMSTEdges() {
        String q = """
            MATCH (a:Shelter)-[r:NEAR]->(b:Shelter)
            RETURN a.id AS from, b.id AS to, r.distKm AS weight
        """;

        List<MSTService.Edge> edges = new ArrayList<>();
        neo4j.query(q).fetch().all().forEach(row -> {
            String from = (String) row.get("from");
            String to = (String) row.get("to");
            double weight = ((Number) row.get("weight")).doubleValue();

            edges.add(new MSTService.Edge(from, to, weight, "NEAR"));
        });
        return edges;
    }
}