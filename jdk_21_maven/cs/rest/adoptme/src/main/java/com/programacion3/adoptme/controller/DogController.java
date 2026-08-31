package com.programacion3.adoptme.controller;

import com.programacion3.adoptme.domain.Dog;
import com.programacion3.adoptme.repo.DogRepository;
import com.programacion3.adoptme.service.SortService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dogs")
@RequiredArgsConstructor
public class DogController {
    private final DogRepository dogRepository;
    private final SortService sortService;

    /**
     * Lista todos los perros
     * GET /dogs
     */
    @GetMapping
    public List<Dog> all() {
        return dogRepository.findAll();
    }

    /**
     * Ordena perros usando MergeSort o QuickSort (Divide y Vencerás)
     * GET /dogs/sort?criteria=priority&algorithm=quicksort
     * GET /dogs/sort?criteria=age&algorithm=mergesort
     * GET /dogs/sort?criteria=weight
     */
    @GetMapping("/sort")
    public ResponseEntity<?> sortDogs(
            @RequestParam(defaultValue = "priority") String criteria,
            @RequestParam(defaultValue = "mergesort") String algorithm
    ) {
        try {
            // Obtener todos los perros
            List<Dog> dogs = dogRepository.findAll();

            if (dogs.isEmpty()) {
                return ResponseEntity.ok(new SortResponse(
                        "No dogs found",
                        criteria,
                        algorithm,
                        List.of()
                ));
            }

            // Ordenar usando el servicio y algoritmo seleccionado
            sortService.sortDogs(dogs, criteria, algorithm);

            String algorithmName = "quicksort".equalsIgnoreCase(algorithm) ? "QuickSort" : "MergeSort";

            return ResponseEntity.ok(new SortResponse(
                    "Dogs sorted by " + criteria + " using " + algorithmName,
                    criteria,
                    algorithm,
                    dogs
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", e.getMessage(),
                            "validCriteria", List.of("priority", "age", "weight"),
                            "validAlgorithms", List.of("mergesort", "quicksort")
                    ));
        }
    }

    // DTO para respuesta
    record SortResponse(
            String message,
            String criteria,
            String algorithm,
            List<Dog> dogs
    ) {}
}