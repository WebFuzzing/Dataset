package com.programacion3.adoptme.controller;

import com.programacion3.adoptme.service.GraphLoader;
import com.programacion3.adoptme.service.MSTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/network")
@RequiredArgsConstructor
public class NetworkController {

    private final MSTService mstService;
    private final GraphLoader graphLoader;

    /**
     * Obtiene todas las conexiones entre shelters para visualización del grafo
     * GET /network/graph
     */
    @GetMapping("/graph")
    public ResponseEntity<GraphResponse> getGraph() {
        // Cargar todos los shelters
        var shelterIds = graphLoader.loadAllShelterIds();

        if (shelterIds.isEmpty()) {
            return ResponseEntity.ok(new GraphResponse(
                    "No shelters found",
                    List.of(),
                    List.of()
            ));
        }

        // Cargar todas las aristas
        var edges = graphLoader.loadMSTEdges();

        // Formatear aristas (evitar duplicados para grafo no dirigido)
        List<EdgeDTO> formattedEdges = edges.stream()
                .map(e -> new EdgeDTO(e.a, e.b, e.weight))
                .distinct()
                .collect(Collectors.toList());

        // Convertir shelterIds a lista
        List<String> shelterList = List.copyOf(shelterIds);

        return ResponseEntity.ok(new GraphResponse(
                "Graph data loaded successfully",
                shelterList,
                formattedEdges
        ));
    }

    /**
     * Minimum Spanning Tree usando Kruskal o Prim
     * Conecta todos los refugios con la menor distancia total
     * GET /network/mst?algorithm=kruskal|prim (default: kruskal)
     */
    @GetMapping("/mst")
    public ResponseEntity<MSTResponse> mst(
            @RequestParam(defaultValue = "kruskal") String algorithm
    ) {
        // Cargar todos los shelters
        var shelterIds = graphLoader.loadAllShelterIds();

        if (shelterIds.isEmpty()) {
            return ResponseEntity.ok(new MSTResponse(
                    "No shelters found",
                    List.of(),
                    0.0,
                    algorithm
            ));
        }

        // Cargar aristas
        var edges = graphLoader.loadMSTEdges();

        // Ejecutar algoritmo seleccionado
        MSTService.MSTResult result;
        String algorithmUsed;

        if ("prim".equalsIgnoreCase(algorithm)) {
            result = mstService.computeWithPrim(shelterIds, edges);
            algorithmUsed = "Prim";
        } else {
            result = mstService.compute(shelterIds, edges);
            algorithmUsed = "Kruskal";
        }

        // Formatear respuesta
        List<EdgeDTO> formattedEdges = result.edges.stream()
                .map(e -> new EdgeDTO(e.a, e.b, e.weight))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new MSTResponse(
                "MST computed using " + algorithmUsed + "'s algorithm",
                formattedEdges,
                result.totalWeight,
                algorithmUsed
        ));
    }

    // DTOs para respuesta
    record GraphResponse(
            String message,
            List<String> nodes,
            List<EdgeDTO> edges
    ) {}

    record MSTResponse(
            String message,
            List<EdgeDTO> edges,
            double totalWeight,
            String algorithm
    ) {}

    record EdgeDTO(
            String from,
            String to,
            double weight
    ) {}
}