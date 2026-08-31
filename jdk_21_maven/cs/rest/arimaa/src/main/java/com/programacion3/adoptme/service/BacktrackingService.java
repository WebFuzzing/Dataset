package com.programacion3.adoptme.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio que implementa Backtracking para asignar perros a adoptantes
 * con restricciones de compatibilidad, presupuesto y capacidad.
 *
 * Problema: Asignar N perros a M adoptantes de forma que:
 * - Cada perro se asigna a máximo un adoptante
 * - Se respetan restricciones de presupuesto, capacidad y compatibilidad
 * - Se maximiza la satisfacción total
 */
@Service
public class BacktrackingService {

    /**
     * Información de un perro para asignación
     */
    public static class Dog {
        public final String id;
        public final boolean goodWithKids;
        public final boolean needsGarden;
        public final int energy;
        public final double cost;

        public Dog(String id, boolean goodWithKids, boolean needsGarden, int energy, double cost) {
            this.id = id;
            this.goodWithKids = goodWithKids;
            this.needsGarden = needsGarden;
            this.energy = energy;
            this.cost = cost;
        }
    }

    /**
     * Información de un adoptante
     */
    public static class Adopter {
        public final String id;
        public final String name;
        public final boolean hasKids;
        public final boolean hasGarden;
        public final int maxDogs;
        public final double budget;
        public final int preferredEnergy; // 1-10

        public Adopter(String id, String name, boolean hasKids, boolean hasGarden,
                       int maxDogs, double budget, int preferredEnergy) {
            this.id = id;
            this.name = name;
            this.hasKids = hasKids;
            this.hasGarden = hasGarden;
            this.maxDogs = maxDogs;
            this.budget = budget;
            this.preferredEnergy = preferredEnergy;
        }
    }

    /**
     * Asignación: adopterId -> lista de dogIds
     */
    public static class Assignment {
        public final Map<String, List<String>> assignments; // adopterId -> [dogIds]
        public final double totalScore;

        public Assignment(Map<String, List<String>> assignments, double totalScore) {
            this.assignments = assignments;
            this.totalScore = totalScore;
        }
    }

    /**
     * Encuentra la mejor asignación usando backtracking.
     *
     * @param dogs lista de perros disponibles
     * @param adopters lista de adoptantes
     * @return mejor asignación encontrada
     */
    public Assignment findBestAssignment(List<Dog> dogs, List<Adopter> adopters) {
        if (dogs.isEmpty() || adopters.isEmpty()) {
            return new Assignment(new HashMap<>(), 0.0);
        }

        // OPTIMIZATION: Limit to first 20 dogs to keep execution time reasonable
        // With 30+ dogs and 15 adopters, complexity becomes too high
        List<Dog> limitedDogs = dogs.size() > 20 ? dogs.subList(0, 20) : dogs;

        // Estado inicial: ningún perro asignado
        Map<String, List<String>> currentAssignment = new HashMap<>();
        for (Adopter a : adopters) {
            currentAssignment.put(a.id, new ArrayList<>());
        }

        Map<String, Double> currentCost = new HashMap<>();
        for (Adopter a : adopters) {
            currentCost.put(a.id, 0.0);
        }

        // Variables para la mejor solución encontrada
        BestSolution best = new BestSolution();
        best.startTime = System.currentTimeMillis();
        best.timeoutMs = 5000; // 5 second timeout

        // Iniciar backtracking
        backtrack(0, limitedDogs, adopters, currentAssignment, currentCost, 0.0, best);

        System.out.println("[BACKTRACKING] Explored " + best.nodesExplored + " nodes");
        System.out.println("[BACKTRACKING] Best score: " + best.score);

        return new Assignment(best.assignments, best.score);
    }

    /**
     * Clase auxiliar para mantener la mejor solución
     */
    private static class BestSolution {
        Map<String, List<String>> assignments = new HashMap<>();
        double score = 0.0;
        long startTime = 0;
        long timeoutMs = 5000;
        int nodesExplored = 0;

        boolean isTimeout() {
            return System.currentTimeMillis() - startTime > timeoutMs;
        }
    }

    /**
     * Algoritmo de backtracking recursivo.
     *
     * @param dogIndex índice del perro actual a asignar
     * @param dogs lista de perros
     * @param adopters lista de adoptantes
     * @param currentAssignment asignación actual
     * @param currentCost costo acumulado por adoptante
     * @param currentScore score total actual
     * @param best mejor solución encontrada hasta ahora
     */
    private void backtrack(
            int dogIndex,
            List<Dog> dogs,
            List<Adopter> adopters,
            Map<String, List<String>> currentAssignment,
            Map<String, Double> currentCost,
            double currentScore,
            BestSolution best
    ) {
        // Check timeout
        if (best.isTimeout()) {
            return;
        }

        best.nodesExplored++;

        // Caso base: todos los perros fueron considerados
        if (dogIndex == dogs.size()) {
            // Si esta solución es mejor, guardarla
            if (currentScore > best.score) {
                best.score = currentScore;
                best.assignments = deepCopy(currentAssignment);
            }
            return;
        }

        Dog dog = dogs.get(dogIndex);

        // Opción 1: No asignar este perro a nadie (puede quedar sin adoptar)
        backtrack(dogIndex + 1, dogs, adopters, currentAssignment, currentCost, currentScore, best);

        // Opción 2: Intentar asignar este perro a cada adoptante
        for (Adopter adopter : adopters) {
            // Check timeout periodically
            if (best.nodesExplored % 1000 == 0 && best.isTimeout()) {
                return;
            }

            // Verificar si es viable asignar este perro a este adoptante
            if (canAssign(dog, adopter, currentAssignment, currentCost)) {
                // Calcular score de esta asignación
                double score = calculateScore(dog, adopter);

                // Hacer asignación (forward)
                currentAssignment.get(adopter.id).add(dog.id);
                currentCost.put(adopter.id, currentCost.get(adopter.id) + dog.cost);

                // Recursión
                backtrack(dogIndex + 1, dogs, adopters, currentAssignment, currentCost,
                        currentScore + score, best);

                // Deshacer asignación (backtrack)
                currentAssignment.get(adopter.id).remove(currentAssignment.get(adopter.id).size() - 1);
                currentCost.put(adopter.id, currentCost.get(adopter.id) - dog.cost);
            }
        }
    }

    /**
     * Verifica si se puede asignar un perro a un adoptante respetando restricciones.
     */
    private boolean canAssign(
            Dog dog,
            Adopter adopter,
            Map<String, List<String>> currentAssignment,
            Map<String, Double> currentCost
    ) {
        // Restricción 1: No exceder capacidad máxima de perros
        if (currentAssignment.get(adopter.id).size() >= adopter.maxDogs) {
            return false;
        }

        // Restricción 2: No exceder presupuesto
        if (currentCost.get(adopter.id) + dog.cost > adopter.budget) {
            return false;
        }

        // Restricción 3: Si el perro no es bueno con niños y el adoptante tiene niños, no asignar
        if (adopter.hasKids && !dog.goodWithKids) {
            return false;
        }

        // Restricción 4: Si el perro necesita jardín y el adoptante no tiene, no asignar
        if (dog.needsGarden && !adopter.hasGarden) {
            return false;
        }

        return true;
    }

    /**
     * Calcula el score de asignar un perro específico a un adoptante específico.
     */
    private double calculateScore(Dog dog, Adopter adopter) {
        double score = 0.0;

        // +5 puntos si es compatible con niños y el adoptante tiene niños
        if (adopter.hasKids && dog.goodWithKids) {
            score += 5.0;
        }

        // +3 puntos si el perro necesita jardín y el adoptante tiene jardín
        if (dog.needsGarden && adopter.hasGarden) {
            score += 3.0;
        }

        // +0 a +5 puntos por compatibilidad de energía (menor diferencia = mejor)
        int energyDiff = Math.abs(dog.energy - adopter.preferredEnergy);
        score += Math.max(0, 5.0 - energyDiff);

        return score;
    }

    /**
     * Crea una copia profunda del mapa de asignaciones.
     */
    private Map<String, List<String>> deepCopy(Map<String, List<String>> original) {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : original.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }
}
