package com.programacion3.adoptme.service;

import org.springframework.stereotype.Service;

import java.util.*;


/*
 Kruskal simple que opera sobre un subconjunto de nodos (p. ej. shelters/hubs)
 y aristas filtradas por tipo "NEAR". Devuelve lista de aristas del MST y costo total.
*/
@Service
public class MSTService {

    public static class Edge {
        public final String a;
        public final String b;
        public final double weight;
        public final String type; // p.ej. "NEAR"

        public Edge(String a, String b, double weight, String type) {
            this.a = a;
            this.b = b;
            this.weight = weight;
            this.type = type;
        }
    }

    public static class MSTResult {
        public final List<Edge> edges;
        public final double totalWeight;
        public MSTResult(List<Edge> edges, double totalWeight) {
            this.edges = edges; this.totalWeight = totalWeight;
        }
    }

    /**
     * Algoritmo de Kruskal para MST.
     * Ordena todas las aristas por peso y las agrega greedily evitando ciclos (Union-Find).
     */
    public MSTResult compute(Collection<String> nodesOfInterest, Collection<Edge> allEdges) {
        // Filtrar aristas NEAR que conecten nodos de interés
        List<Edge> edges = new ArrayList<>();
        for (Edge e : allEdges) {
            if (!"NEAR".equalsIgnoreCase(e.type)) continue;
            if (nodesOfInterest.contains(e.a) && nodesOfInterest.contains(e.b)) edges.add(e);
        }
        edges.sort(Comparator.comparingDouble(e -> e.weight));
        UnionFind uf = new UnionFind(nodesOfInterest);
        List<Edge> mst = new ArrayList<>();
        double total = 0.0;
        for (Edge e : edges) {
            if (uf.union(e.a, e.b)) {
                mst.add(e);
                total += e.weight;
            }
        }
        return new MSTResult(mst, total);
    }

    /**
     * Algoritmo de Prim para MST.
     * Empieza desde un nodo inicial y expande el árbol agregando la arista de menor peso
     * que conecta un nodo del árbol con uno fuera del árbol.
     */
    public MSTResult computeWithPrim(Collection<String> nodesOfInterest, Collection<Edge> allEdges) {
        if (nodesOfInterest.isEmpty()) {
            return new MSTResult(new ArrayList<>(), 0.0);
        }

        // Filtrar aristas NEAR que conecten nodos de interés
        List<Edge> edges = new ArrayList<>();
        for (Edge e : allEdges) {
            if (!"NEAR".equalsIgnoreCase(e.type)) continue;
            if (nodesOfInterest.contains(e.a) && nodesOfInterest.contains(e.b)) edges.add(e);
        }

        // Construir lista de adyacencia con pesos
        Map<String, List<Edge>> adj = new HashMap<>();
        for (String node : nodesOfInterest) {
            adj.put(node, new ArrayList<>());
        }
        for (Edge e : edges) {
            adj.get(e.a).add(e);
            // Agregar arista inversa para grafo no dirigido
            adj.get(e.b).add(new Edge(e.b, e.a, e.weight, e.type));
        }

        // Prim: empezar desde un nodo arbitrario
        String start = nodesOfInterest.iterator().next();
        Set<String> inMST = new HashSet<>();
        inMST.add(start);

        // Cola de prioridad de aristas: (peso, arista)
        PriorityQueue<EdgeWithPriority> pq = new PriorityQueue<>(Comparator.comparingDouble(e -> e.weight));

        // Agregar todas las aristas del nodo inicial
        for (Edge e : adj.get(start)) {
            pq.offer(new EdgeWithPriority(e.weight, e));
        }

        List<Edge> mst = new ArrayList<>();
        double total = 0.0;

        while (!pq.isEmpty() && inMST.size() < nodesOfInterest.size()) {
            EdgeWithPriority current = pq.poll();
            Edge edge = current.edge;

            // Si ambos extremos ya están en el MST, saltar (evita ciclos)
            if (inMST.contains(edge.b)) {
                continue;
            }

            // Agregar arista al MST
            mst.add(edge);
            total += edge.weight;
            inMST.add(edge.b);

            // Agregar todas las aristas del nuevo nodo
            for (Edge e : adj.get(edge.b)) {
                if (!inMST.contains(e.b)) {
                    pq.offer(new EdgeWithPriority(e.weight, e));
                }
            }
        }

        return new MSTResult(mst, total);
    }

    // Clase auxiliar para la cola de prioridad de Prim
    private static class EdgeWithPriority {
        final double weight;
        final Edge edge;

        EdgeWithPriority(double weight, Edge edge) {
            this.weight = weight;
            this.edge = edge;
        }
    }

    private static class UnionFind {
        private final Map<String, String> parent;
        private final Map<String, Integer> rank;

        UnionFind(Collection<String> nodes) {
            parent = new HashMap<>();
            rank = new HashMap<>();
            for (String n : nodes) { parent.put(n, n); rank.put(n, 0); }
        }

        String find(String x) {
            String p = parent.get(x);
            if (p == null) return x;
            if (!p.equals(x)) parent.put(x, find(p));
            return parent.get(x);
        }

        boolean union(String a, String b) {
            String ra = find(a), rb = find(b);
            if (ra.equals(rb)) return false;
            int rka = rank.getOrDefault(ra, 0), rkb = rank.getOrDefault(rb, 0);
            if (rka < rkb) parent.put(ra, rb);
            else if (rka > rkb) parent.put(rb, ra);
            else { parent.put(rb, ra); rank.put(ra, rka + 1); }
            return true;
        }
    }
}

