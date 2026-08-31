package com.programacion3.adoptme.controller;

import com.programacion3.adoptme.dto.PathResponse;
import com.programacion3.adoptme.dto.TspResponse;
import com.programacion3.adoptme.service.GraphLoader;
import com.programacion3.adoptme.service.ShortestPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RoutesController {

    private final ShortestPathService shortestPathService;
    private final GraphLoader graphLoader;
    private final com.programacion3.adoptme.service.TSPService tspService;

    /**
     * Dijkstra - Encuentra el camino más corto considerando distancias
     * GET /routes/shortest?from=A&to=C
     */
    @GetMapping("/shortest")
    public ResponseEntity<PathResponse> shortestRoute(
            @RequestParam String from,
            @RequestParam String to
    ) {
        // Validaciones
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new PathResponse(false, "Dijkstra", null, 0, 0.0));
        }

        // Cargar aristas con pesos desde Neo4j
        var edges = graphLoader.loadWeightedEdges();

        // Ejecutar Dijkstra
        var result = shortestPathService.shortestPath(from, to, edges);

        // Si no hay camino
        if (result.path.isEmpty() || Double.isInfinite(result.cost)) {
            return ResponseEntity.ok(new PathResponse(
                    false,
                    "Dijkstra",
                    null,
                    0,
                    0.0
            ));
        }

        // Respuesta exitosa
        return ResponseEntity.ok(new PathResponse(
                true,
                "Dijkstra",
                result.path,
                result.path.size() - 1,
                result.cost
        ));
    }

    /**
     * TSP (Travelling Salesman Problem) usando Branch & Bound
     *
     * Encuentra la ruta más corta para visitar todos los refugios especificados
     * exactamente una vez y regresar al punto de inicio.
     *
     * GET /routes/tsp/bnb?nodes=A,B,C,H
     * Si no se especifican nodos, usa todos los refugios disponibles.
     *
     * @param nodes lista de IDs de nodos separados por comas (opcional)
     * @return tour óptimo y distancia total
     */
    @GetMapping("/tsp/bnb")
    public ResponseEntity<TspResponse> tspBranchBound(
            @RequestParam(required = false) String nodes
    ) {
        // Determinar qué nodos visitar
        List<String> nodeList;

        if (nodes != null && !nodes.isBlank()) {
            // Usar nodos especificados
            nodeList = Arrays.asList(nodes.split(","));
            nodeList = nodeList.stream().map(String::trim).toList();
        } else {
            // Usar todos los shelters
            nodeList = new ArrayList<>(graphLoader.loadAllShelterIds());
        }

        if (nodeList.isEmpty()) {
            return ResponseEntity.ok(
                    TspResponse.builder()
                            .route(List.of())
                            .totalDistanceKm(0)
                            .build()
            );
        }

        if (nodeList.size() == 1) {
            return ResponseEntity.ok(
                    TspResponse.builder()
                            .route(nodeList)
                            .totalDistanceKm(0)
                            .build()
            );
        }

        // Cargar aristas con distancias
        var rawEdges = graphLoader.loadWeightedEdges();

        // Convertir al formato del TSPService
        List<com.programacion3.adoptme.service.TSPService.Edge> edges = rawEdges.stream()
                .map(e -> new com.programacion3.adoptme.service.TSPService.Edge(
                        e.from,
                        e.to,
                        e.weight
                ))
                .toList();

        // Ejecutar algoritmo de Branch & Bound
        var result = tspService.solveTSP(nodeList, edges);

        // Verificar si se encontró una solución
        if (result.route.isEmpty() || Double.isInfinite(result.totalDistance)) {
            return ResponseEntity.ok(
                    TspResponse.builder()
                            .route(null)
                            .totalDistanceKm(null)
                            .build()
            );
        }

        return ResponseEntity.ok(
                TspResponse.builder()
                        .route(result.route)
                        .totalDistanceKm((int) Math.round(result.totalDistance))
                        .build()
        );
    }
}