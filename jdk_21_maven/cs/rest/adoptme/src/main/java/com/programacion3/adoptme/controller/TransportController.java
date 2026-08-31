package com.programacion3.adoptme.controller;

import com.programacion3.adoptme.domain.Dog;
import com.programacion3.adoptme.repo.DogRepository;
import com.programacion3.adoptme.service.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para optimización de transporte de perros.
 * Usa Programación Dinámica (problema de la mochila 0/1) para seleccionar
 * el mejor conjunto de perros dado una capacidad de vehículo limitada.
 */
@RestController
@RequestMapping("/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;
    private final DogRepository dogRepository;

    /**
     * Optimiza el transporte de perros usando Programación Dinámica (Knapsack).
     *
     * Dado un vehículo con capacidad limitada, selecciona el conjunto óptimo
     * de perros que maximiza la prioridad total sin exceder la capacidad.
     *
     * GET /transport/optimal-dp?capacityKg=50
     *
     * @param capacityKg capacidad del vehículo en kilogramos
     * @return conjunto óptimo de perros para transportar
     */
    @GetMapping("/optimal-dp")
    public ResponseEntity<?> optimalTransport(
            @RequestParam(defaultValue = "50") int capacityKg
    ) {
        if (capacityKg <= 0) {
            return ResponseEntity.badRequest()
                    .body(new TransportResponse(
                            "Invalid capacity: must be greater than 0",
                            capacityKg,
                            List.of(),
                            0,
                            0
                    ));
        }

        // Obtener todos los perros disponibles
        List<Dog> allDogs = dogRepository.findAll();

        if (allDogs.isEmpty()) {
            return ResponseEntity.ok(new TransportResponse(
                    "No dogs available for transport",
                    capacityKg,
                    List.of(),
                    0,
                    0
            ));
        }

        // Ejecutar algoritmo de programación dinámica
        TransportService.KnapsackResult result = transportService.optimizeTransport(allDogs, capacityKg);

        // Convertir perros a DTO simple
        List<DogDTO> selectedDogs = result.selectedDogs.stream()
                .map(dog -> new DogDTO(
                        dog.getId(),
                        dog.getName(),
                        dog.getWeight(),
                        dog.getPriority()
                ))
                .toList();

        return ResponseEntity.ok(new TransportResponse(
                "Optimal transport computed using Dynamic Programming (Knapsack)",
                capacityKg,
                selectedDogs,
                result.totalPriority,
                result.totalWeight
        ));
    }

    // DTOs para respuesta
    record TransportResponse(
            String message,
            int vehicleCapacityKg,
            List<DogDTO> selectedDogs,
            int totalPriority,
            int totalWeightKg
    ) {}

    record DogDTO(
            String id,
            String name,
            int weightKg,
            int priority
    ) {}
}
