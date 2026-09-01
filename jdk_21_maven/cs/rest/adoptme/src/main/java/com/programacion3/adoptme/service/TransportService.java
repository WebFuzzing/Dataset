package com.programacion3.adoptme.service;

import com.programacion3.adoptme.domain.Dog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para optimizar el transporte de perros usando Programación Dinámica (Knapsack).
 *
 * Problema: Dado un vehículo con capacidad limitada en kg, seleccionar el mejor conjunto
 * de perros para transportar maximizando la prioridad total.
 */
@Service
public class TransportService {

    /**
     * Resuelve el problema de la mochila 0/1 usando Programación Dinámica.
     *
     * @param dogs lista de perros disponibles para transporte
     * @param capacityKg capacidad del vehículo en kilogramos
     * @return resultado con perros seleccionados y valor total
     */
    public KnapsackResult optimizeTransport(List<Dog> dogs, int capacityKg) {
        if (dogs == null || dogs.isEmpty() || capacityKg <= 0) {
            return new KnapsackResult(new ArrayList<>(), 0, 0);
        }

        int n = dogs.size();

        // Tabla DP: dp[i][w] = máxima prioridad con primeros i perros y capacidad w
        int[][] dp = new int[n + 1][capacityKg + 1];

        // Llenar tabla usando programación dinámica
        for (int i = 1; i <= n; i++) {
            Dog dog = dogs.get(i - 1);
            int weight = dog.getWeight();
            int priority = dog.getPriority();

            for (int w = 0; w <= capacityKg; w++) {
                // Opción 1: No incluir este perro
                dp[i][w] = dp[i - 1][w];

                // Opción 2: Incluir este perro (si cabe)
                if (weight <= w) {
                    int valueWithDog = dp[i - 1][w - weight] + priority;
                    dp[i][w] = Math.max(dp[i][w], valueWithDog);
                }
            }
        }

        // Reconstruir solución: qué perros fueron seleccionados
        List<Dog> selectedDogs = new ArrayList<>();
        int w = capacityKg;
        int totalWeight = 0;

        for (int i = n; i > 0 && w > 0; i--) {
            // Si el valor cambió, significa que incluimos este perro
            if (dp[i][w] != dp[i - 1][w]) {
                Dog dog = dogs.get(i - 1);
                selectedDogs.add(dog);
                w -= dog.getWeight();
                totalWeight += dog.getWeight();
            }
        }

        int totalPriority = dp[n][capacityKg];

        return new KnapsackResult(selectedDogs, totalPriority, totalWeight);
    }

    /**
     * Resultado del problema de la mochila
     */
    public static class KnapsackResult {
        public final List<Dog> selectedDogs;
        public final int totalPriority;
        public final int totalWeight;

        public KnapsackResult(List<Dog> selectedDogs, int totalPriority, int totalWeight) {
            this.selectedDogs = selectedDogs;
            this.totalPriority = totalPriority;
            this.totalWeight = totalWeight;
        }
    }
}
