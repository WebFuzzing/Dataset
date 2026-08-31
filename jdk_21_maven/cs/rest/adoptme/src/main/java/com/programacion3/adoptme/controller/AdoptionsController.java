package com.programacion3.adoptme.controller;

import com.programacion3.adoptme.domain.Dog;
import com.programacion3.adoptme.exception.ResourceNotFoundException;
import com.programacion3.adoptme.repo.AdopterRepository;
import com.programacion3.adoptme.repo.DogRepository;
import com.programacion3.adoptme.service.ScorerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/adoptions")
@RequiredArgsConstructor
public class AdoptionsController {

    private final AdopterRepository adopterRepository;
    private final DogRepository dogRepository;
    private final ScorerService scorerService;
    private final com.programacion3.adoptme.service.BacktrackingService backtrackingService;

    /**
     * Algoritmo Greedy: Asigna perros a un adoptante maximizando el score
     * Considera: presupuesto, jardín, niños, energía del perro
     * GET /adoptions/greedy?adopterId=P1
     */
    @GetMapping("/greedy")
    public ResponseEntity<GreedyResponse> greedyAdoption(
            @RequestParam String adopterId
    ) {
        // Buscar adoptante
        var adopter = adopterRepository.findById(adopterId)
                .orElseThrow(() -> new ResourceNotFoundException("Adopter not found: " + adopterId));

        // Obtener todos los perros disponibles
        List<Dog> allDogs = dogRepository.findAll();

        if (allDogs.isEmpty()) {
            return ResponseEntity.ok(new GreedyResponse(
                    "No dogs available for adoption",
                    adopterId,
                    adopter.getName(),
                    List.of(),
                    0.0,
                    0.0
            ));
        }

        // Convertir a formato del ScorerService
        List<ScorerService.Dog> candidates = allDogs.stream()
                .map(d -> new ScorerService.Dog(
                        d.getId(),
                        d.getGoodWithKids() != null && d.getGoodWithKids(),
                        "LARGE".equalsIgnoreCase(d.getSize()), // perros grandes necesitan jardín
                        mapEnergy(d.getEnergy()),
                        mapSize(d.getSize()),
                        estimateCost(d) // costo estimado por adopción
                ))
                .collect(Collectors.toList());

        // Ejecutar algoritmo Greedy
        var result = scorerService.scoreAndAssign(
                candidates,
                adopter.getHasKids() != null && adopter.getHasKids(),
                adopter.getHasYard() != null && adopter.getHasYard(),
                adopter.getMaxDogs() != null ? adopter.getMaxDogs() : 1,
                adopter.getBudget() != null ? adopter.getBudget() : 20000.0
        );

        // Formatear respuesta
        List<AssignedDog> assigned = result.assigned.stream()
                .map(d -> new AssignedDog(d.id, findDogName(allDogs, d.id), d.cost))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new GreedyResponse(
                "Greedy algorithm executed successfully",
                adopterId,
                adopter.getName(),
                assigned,
                result.totalScore,
                result.totalCost
        ));
    }

    /**
     * Algoritmo Backtracking: Asigna múltiples perros a múltiples adoptantes
     * respetando TODAS las restricciones y maximizando satisfacción total.
     *
     * Considera restricciones de:
     * - Presupuesto
     * - Capacidad máxima de perros
     * - Compatibilidad con niños
     * - Necesidad de jardín
     * - Preferencia de energía
     *
     * GET /adoptions/constraints/backtracking
     */
    @GetMapping("/constraints/backtracking")
    public ResponseEntity<BacktrackingResponse> backtrackingAdoption() {
        // Obtener todos los perros y adoptantes
        List<Dog> allDogs = dogRepository.findAll();
        var allAdopters = adopterRepository.findAll();

        if (allDogs.isEmpty()) {
            return ResponseEntity.ok(new BacktrackingResponse(
                    "No dogs available for adoption",
                    Map.of(),
                    0.0
            ));
        }

        if (allAdopters.isEmpty()) {
            return ResponseEntity.ok(new BacktrackingResponse(
                    "No adopters available",
                    Map.of(),
                    0.0
            ));
        }

        // Convertir perros al formato del servicio
        List<com.programacion3.adoptme.service.BacktrackingService.Dog> dogs = allDogs.stream()
                .map(d -> new com.programacion3.adoptme.service.BacktrackingService.Dog(
                        d.getId(),
                        d.getGoodWithKids() != null && d.getGoodWithKids(),
                        "LARGE".equalsIgnoreCase(d.getSize()),
                        mapEnergy(d.getEnergy()),
                        estimateCost(d)
                ))
                .toList();

        // Convertir adoptantes al formato del servicio
        List<com.programacion3.adoptme.service.BacktrackingService.Adopter> adopters = allAdopters.stream()
                .map(a -> new com.programacion3.adoptme.service.BacktrackingService.Adopter(
                        a.getId(),
                        a.getName(),
                        a.getHasKids() != null && a.getHasKids(),
                        a.getHasYard() != null && a.getHasYard(),
                        a.getMaxDogs() != null ? a.getMaxDogs() : 1,
                        a.getBudget() != null ? a.getBudget() : 20000.0,
                        5 // energía preferida default (media)
                ))
                .toList();

        // Ejecutar algoritmo de backtracking
        var result = backtrackingService.findBestAssignment(dogs, adopters);

        // Formatear respuesta
        Map<String, AdopterAssignment> assignments = new HashMap<>();

        for (var adopter : allAdopters) {
            List<String> dogIds = result.assignments.getOrDefault(adopter.getId(), List.of());

            if (!dogIds.isEmpty()) {
                List<AssignedDog> assignedDogs = dogIds.stream()
                        .map(dogId -> {
                            String dogName = findDogName(allDogs, dogId);
                            double cost = estimateCost(allDogs.stream()
                                    .filter(d -> d.getId().equals(dogId))
                                    .findFirst()
                                    .orElse(null));
                            return new AssignedDog(dogId, dogName, cost);
                        })
                        .toList();

                assignments.put(adopter.getId(), new AdopterAssignment(
                        adopter.getId(),
                        adopter.getName(),
                        assignedDogs
                ));
            }
        }

        return ResponseEntity.ok(new BacktrackingResponse(
                "Backtracking algorithm completed successfully",
                assignments,
                result.totalScore
        ));
    }

    // Métodos auxiliares
    private int mapEnergy(String energy) {
        if (energy == null) return 5;
        return switch (energy.toUpperCase()) {
            case "LOW" -> 2;
            case "MEDIUM" -> 5;
            case "HIGH" -> 8;
            default -> 5;
        };
    }

    private int mapSize(String size) {
        if (size == null) return 2;
        return switch (size.toUpperCase()) {
            case "SMALL" -> 1;
            case "MEDIUM" -> 2;
            case "LARGE" -> 3;
            default -> 2;
        };
    }

    private double estimateCost(Dog dog) {
        // Costo base + extra por tamaño y necesidades especiales
        double baseCost = 5000.0;
        double sizeCost = mapSize(dog.getSize()) * 2000.0;
        double specialNeedsCost = (dog.getSpecialNeeds() != null && dog.getSpecialNeeds()) ? 5000.0 : 0.0;
        return baseCost + sizeCost + specialNeedsCost;
    }

    private String findDogName(List<Dog> dogs, String id) {
        return dogs.stream()
                .filter(d -> d.getId().equals(id))
                .map(Dog::getName)
                .findFirst()
                .orElse("Unknown");
    }

    // DTOs
    record GreedyResponse(
            String message,
            String adopterId,
            String adopterName,
            List<AssignedDog> assignedDogs,
            double totalScore,
            double totalCost
    ) {}

    record BacktrackingResponse(
            String message,
            Map<String, AdopterAssignment> assignments,
            double totalScore
    ) {}

    record AdopterAssignment(
            String adopterId,
            String adopterName,
            List<AssignedDog> assignedDogs
    ) {}

    record AssignedDog(
            String dogId,
            String dogName,
            double cost
    ) {}
}