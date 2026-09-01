package com.programacion3.adoptme.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphService {

    private final GraphLoader loader;

    public GraphService(GraphLoader loader) {
        this.loader = loader;
    }

    /** Camino más corto en cantidad de pasos (BFS). Devuelve vacío si no hay. */
    public List<String> bfsPath(String from, String to) {
        Map<String, List<String>> g = loader.loadAdjacency();
        if (from == null || to == null || from.isBlank() || to.isBlank()) return List.of();
        if (from.equals(to)) return List.of(from);

        Queue<String> q = new ArrayDeque<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> vis = new HashSet<>();

        q.add(from);
        vis.add(from);

        while (!q.isEmpty()) {
            String u = q.poll();
            if (u.equals(to)) break;
            for (String v : g.getOrDefault(u, List.of())) {
                if (!vis.contains(v)) {
                    vis.add(v);
                    prev.put(v, u);
                    q.add(v);
                }
            }
        }

        if (!vis.contains(to)) return List.of(); // no hay camino

        LinkedList<String> path = new LinkedList<>();
        for (String cur = to; cur != null; cur = prev.get(cur)) path.addFirst(cur);
        return path;
    }

    /** DFS: intenta encontrar algún camino; no garantiza mínimo en pasos. */
    public List<String> dfsPath(String from, String to) {
        Map<String, List<String>> g = loader.loadAdjacency();
        List<String> path = new ArrayList<>();
        Set<String> vis = new HashSet<>();
        if (dfs(g, from, to, vis, path)) return path;
        return List.of();
    }

    private boolean dfs(Map<String, List<String>> g, String u, String to, Set<String> vis, List<String> path) {
        if (u == null) return false;
        path.add(u);
        if (u.equals(to)) return true;
        vis.add(u);
        for (String v : g.getOrDefault(u, List.of())) {
            if (!vis.contains(v)) {
                if (dfs(g, v, to, vis, path)) return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }
}
