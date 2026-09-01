package com.programacion3.adoptme.service;
import org.springframework.stereotype.Service;
import java.util.*;
/*
     Minimal Dijkstra que usa PriorityQueue y devuelve costo + camino.
     Entrada: colección de aristas dirigidas/indirectas (Edge), ids de nodo como String.

*/
@Service
public class ShortestPathService {

        public static class Edge {
            public final String from;
            public final String to;
            public final double weight;

            public Edge(String from, String to, double weight) {
                this.from = from;
                this.to = to;
                this.weight = weight;
            }
        }

        public static class PathResult {
            public final double cost;
            public final List<String> path;

            public PathResult(double cost, List<String> path) {
                this.cost = cost;
                this.path = path;
            }
        }

        public PathResult shortestPath(String start, String goal, Collection<Edge> edges) {
            Map<String, List<Edge>> adj = buildAdj(edges);
            Map<String, Double> dist = new HashMap<>();
            Map<String, String> prev = new HashMap<>();
            PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.dist));
            dist.put(start, 0.0);
            pq.add(new Node(start, 0.0));

            while (!pq.isEmpty()) {
                Node cur = pq.poll();
                if (cur.dist > dist.getOrDefault(cur.id, Double.POSITIVE_INFINITY)) continue;
                if (cur.id.equals(goal)) break;
                for (Edge e : adj.getOrDefault(cur.id, Collections.emptyList())) {
                    double nd = cur.dist + e.weight;
                    if (nd < dist.getOrDefault(e.to, Double.POSITIVE_INFINITY)) {
                        dist.put(e.to, nd);
                        prev.put(e.to, cur.id);
                        pq.add(new Node(e.to, nd));
                    }
                }
            }

            if (!dist.containsKey(goal)) return new PathResult(Double.POSITIVE_INFINITY, Collections.emptyList());
            List<String> path = new ArrayList<>();
            String cur = goal;
            while (cur != null) {
                path.add(cur);
                cur = prev.get(cur);
            }
            Collections.reverse(path);
            return new PathResult(dist.get(goal), path);
        }

        private Map<String, List<Edge>> buildAdj(Collection<Edge> edges) {
            Map<String, List<Edge>> adj = new HashMap<>();
            for (Edge e : edges) {
                adj.computeIfAbsent(e.from, k -> new ArrayList<>()).add(e);
                // si el grafo es no dirigido, añadir la inversa:
                adj.computeIfAbsent(e.to, k -> new ArrayList<>()).add(new Edge(e.to, e.from, e.weight));
            }
            return adj;
        }

        private static class Node {
            final String id;
            final double dist;
            Node(String id, double dist) { this.id = id; this.dist = dist; }
        }
    }


