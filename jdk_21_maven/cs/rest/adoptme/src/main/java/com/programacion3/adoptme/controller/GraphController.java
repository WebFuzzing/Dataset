package com.programacion3.adoptme.controller;

import com.programacion3.adoptme.dto.PathResponse;
import com.programacion3.adoptme.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    /**
     * Ejemplo:
     *  GET /graph/reachable?from=A&to=D
     *  GET /graph/reachable?from=A&to=D&method=dfs
     */
    @GetMapping("/reachable")
    public ResponseEntity<PathResponse> reachable(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "bfs") String method
    ) {
        var m = method.trim().toLowerCase();
        switch (m) {
            case "dfs" -> {
                var path = graphService.dfsPath(from, to);
                var steps = path.isEmpty() ? 0 : path.size() - 1;
                return ResponseEntity.ok(new PathResponse(!path.isEmpty(), "DFS", path, steps, 0));
            }
            default -> {
                var path = graphService.bfsPath(from, to);
                var steps = path.isEmpty() ? 0 : path.size() - 1;
                return ResponseEntity.ok(new PathResponse(!path.isEmpty(), "BFS", path, steps, 0));
            }
        }
    }
}
