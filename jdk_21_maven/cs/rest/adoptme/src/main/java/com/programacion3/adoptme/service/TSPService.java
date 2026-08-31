package com.programacion3.adoptme.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio que implementa Branch & Bound para el Problema del Viajante (TSP).
 *
 * Encuentra la ruta más corta que visita todos los nodos exactamente una vez
 * y regresa al nodo de inicio.
 */
@Service
public class TSPService {

    /**
     * Representa una arista con peso
     */
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

    /**
     * Resultado del TSP
     */
    public static class TSPResult {
        public final List<String> route; // Secuencia de nodos en el tour óptimo
        public final double totalDistance; // Distancia total del tour

        public TSPResult(List<String> route, double totalDistance) {
            this.route = route;
            this.totalDistance = totalDistance;
        }
    }

    /**
     * Resuelve TSP usando Branch & Bound
     *
     * @param nodes lista de nodos a visitar
     * @param edges lista de aristas con distancias
     * @return tour óptimo y su distancia total
     */
    public TSPResult solveTSP(List<String> nodes, List<Edge> edges) {
        if (nodes == null || nodes.isEmpty()) {
            return new TSPResult(new ArrayList<>(), 0.0);
        }

        if (nodes.size() == 1) {
            return new TSPResult(new ArrayList<>(nodes), 0.0);
        }

        // Construir matriz de distancias
        Map<String, Map<String, Double>> distMatrix = buildDistanceMatrix(nodes, edges);

        // Verificar si el grafo es conexo
        if (!isConnected(nodes, distMatrix)) {
            return new TSPResult(new ArrayList<>(), Double.POSITIVE_INFINITY);
        }

        // Variables para la mejor solución
        BestSolution best = new BestSolution();
        best.cost = Double.POSITIVE_INFINITY;

        // Empezar desde el primer nodo
        String startNode = nodes.get(0);
        List<String> currentPath = new ArrayList<>();
        currentPath.add(startNode);

        Set<String> visited = new HashSet<>();
        visited.add(startNode);

        // Branch & Bound
        branchAndBound(startNode, startNode, currentPath, visited, 0.0, nodes, distMatrix, best);

        return new TSPResult(best.route, best.cost);
    }

    /**
     * Clase para mantener la mejor solución encontrada
     */
    private static class BestSolution {
        List<String> route = new ArrayList<>();
        double cost = Double.POSITIVE_INFINITY;
    }

    /**
     * Algoritmo de Branch & Bound recursivo
     *
     * @param startNode nodo de inicio del tour
     * @param currentNode nodo actual
     * @param currentPath camino actual
     * @param visited conjunto de nodos visitados
     * @param currentCost costo acumulado
     * @param allNodes todos los nodos a visitar
     * @param distMatrix matriz de distancias
     * @param best mejor solución encontrada
     */
    private void branchAndBound(
            String startNode,
            String currentNode,
            List<String> currentPath,
            Set<String> visited,
            double currentCost,
            List<String> allNodes,
            Map<String, Map<String, Double>> distMatrix,
            BestSolution best
    ) {
        // Caso base: todos los nodos visitados
        if (visited.size() == allNodes.size()) {
            // Agregar costo de regresar al inicio
            double returnCost = getDistance(currentNode, startNode, distMatrix);

            if (returnCost != Double.POSITIVE_INFINITY) {
                double totalCost = currentCost + returnCost;

                // Si es mejor que la solución actual, actualizarla
                if (totalCost < best.cost) {
                    best.cost = totalCost;
                    best.route = new ArrayList<>(currentPath);
                    best.route.add(startNode); // Completar el ciclo
                }
            }
            return;
        }

        // Calcular bound (cota inferior)
        double bound = currentCost + calculateBound(currentNode, visited, allNodes, distMatrix);

        // Poda: si el bound supera la mejor solución conocida, no explorar esta rama
        if (bound >= best.cost) {
            return;
        }

        // Explorar todos los nodos no visitados
        for (String nextNode : allNodes) {
            if (!visited.contains(nextNode)) {
                double edgeCost = getDistance(currentNode, nextNode, distMatrix);

                if (edgeCost != Double.POSITIVE_INFINITY) {
                    // Forward
                    currentPath.add(nextNode);
                    visited.add(nextNode);

                    // Recursión
                    branchAndBound(
                            startNode,
                            nextNode,
                            currentPath,
                            visited,
                            currentCost + edgeCost,
                            allNodes,
                            distMatrix,
                            best
                    );

                    // Backtrack
                    currentPath.remove(currentPath.size() - 1);
                    visited.remove(nextNode);
                }
            }
        }
    }

    /**
     * Calcula una cota inferior (bound) para el costo restante.
     * Usa la suma de las dos aristas más pequeñas de cada nodo no visitado.
     */
    private double calculateBound(
            String currentNode,
            Set<String> visited,
            List<String> allNodes,
            Map<String, Map<String, Double>> distMatrix
    ) {
        double bound = 0.0;

        // 1. Arista mínima desde nodo actual a cualquier no visitado
        double minFromCurrent = Double.POSITIVE_INFINITY;
        for (String node : allNodes) {
            if (!visited.contains(node)) {
                double dist = getDistance(currentNode, node, distMatrix);
                minFromCurrent = Math.min(minFromCurrent, dist);
            }
        }
        if (minFromCurrent != Double.POSITIVE_INFINITY) {
            bound += minFromCurrent;
        }

        // 2. Costo MST de los nodos no visitados
        List<String> unvisited = allNodes.stream()
                .filter(n -> !visited.contains(n))
                .toList();

        if (unvisited.size() > 1) {
            bound += calculateMSTCost(unvisited, distMatrix);
        }

        // 3. Arista mínima desde cualquier no visitado de vuelta al inicio
        String startNode = allNodes.get(0);
        if (!visited.contains(startNode) && !unvisited.isEmpty()) {
            double minToStart = Double.POSITIVE_INFINITY;
            for (String node : unvisited) {
                double dist = getDistance(node, startNode, distMatrix);
                minToStart = Math.min(minToStart, dist);
            }
            if (minToStart != Double.POSITIVE_INFINITY) {
                bound += minToStart;
            }
        }

        return bound;
    }

    // Método auxiliar: Calcular costo MST con algoritmo de Prim
    private double calculateMSTCost(
            List<String> nodes,
            Map<String, Map<String, Double>> distMatrix
    ) {
        if (nodes.isEmpty()) return 0.0;
        if (nodes.size() == 1) return 0.0;

        Set<String> inMST = new HashSet<>();
        double mstCost = 0.0;

        inMST.add(nodes.get(0));

        while (inMST.size() < nodes.size()) {
            double minEdge = Double.POSITIVE_INFINITY;
            String nextNode = null;

            for (String inNode : inMST) {
                for (String outNode : nodes) {
                    if (!inMST.contains(outNode)) {
                        double dist = getDistance(inNode, outNode, distMatrix);
                        if (dist < minEdge) {
                            minEdge = dist;
                            nextNode = outNode;
                        }
                    }
                }
            }

            if (nextNode != null && minEdge != Double.POSITIVE_INFINITY) {
                inMST.add(nextNode);
                mstCost += minEdge;
            } else {
                break; // Grafo no conexo
            }
        }

        return mstCost;
    }

    /**
     * Construye una matriz de distancias a partir de las aristas
     */
    private Map<String, Map<String, Double>> buildDistanceMatrix(List<String> nodes, List<Edge> edges) {
        Map<String, Map<String, Double>> matrix = new HashMap<>();

        // Inicializar con infinito
        for (String from : nodes) {
            matrix.put(from, new HashMap<>());
            for (String to : nodes) {
                if (from.equals(to)) {
                    matrix.get(from).put(to, 0.0);
                } else {
                    matrix.get(from).put(to, Double.POSITIVE_INFINITY);
                }
            }
        }

        // Llenar con las distancias de las aristas directas (grafo no dirigido)
        for (Edge edge : edges) {
            if (matrix.containsKey(edge.from) && matrix.containsKey(edge.to)) {
                matrix.get(edge.from).put(edge.to, edge.weight);
                matrix.get(edge.to).put(edge.from, edge.weight);
            }
        }

        // ✅ FLOYD-WARSHALL: Calcular caminos más cortos indirectos
        for (String k : nodes) {
            for (String i : nodes) {
                for (String j : nodes) {
                    double distIK = matrix.get(i).get(k);
                    double distKJ = matrix.get(k).get(j);
                    double distIJ = matrix.get(i).get(j);

                    // Si hay un camino más corto vía k, actualizarlo
                    if (distIK + distKJ < distIJ) {
                        matrix.get(i).put(j, distIK + distKJ);
                    }
                }
            }
        }

        return matrix;
    }

    /**
     * Obtiene la distancia entre dos nodos
     */
    private double getDistance(String from, String to, Map<String, Map<String, Double>> distMatrix) {
        if (distMatrix.containsKey(from) && distMatrix.get(from).containsKey(to)) {
            return distMatrix.get(from).get(to);
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * Verifica si el grafo es conexo (todos los nodos son alcanzables)
     */
    private boolean isConnected(List<String> nodes, Map<String, Map<String, Double>> distMatrix) {
        if (nodes.isEmpty()) return true;
        if (nodes.size() == 1) return true;

        // Construir grafo de adyacencia BASADO en la distMatrix
        Map<String, Set<String>> adj = new HashMap<>();
        for (String node : nodes) {
            adj.put(node, new HashSet<>());
        }

        // Si existe un camino (distancia finita), son vecinos
        for (String from : nodes) {
            for (String to : nodes) {
                if (!from.equals(to)) {
                    double dist = getDistance(from, to, distMatrix);
                    if (dist != Double.POSITIVE_INFINITY) {
                        adj.get(from).add(to);  // Hay camino → son vecinos
                    }
                }
            }
        }

        // BFS para verificar conectividad
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        String start = nodes.get(0);
        queue.offer(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            // Explorar vecinos REALES del grafo de adyacencia
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
