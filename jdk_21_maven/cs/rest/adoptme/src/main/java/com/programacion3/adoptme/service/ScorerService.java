// java
package com.programacion3.adoptme.service;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;



/*
 Scorer simple: calcula score por perro según criterios (children/garden/energy/size),
 ordena por score descendente y asigna hasta maxDogs o presupuesto.
*/
@Service
public class ScorerService {
    public static class Dog {
        public final String id;
        public final boolean goodWithKids;
        public final boolean hasGardenNeeded;
        public final int energy; // 1..10
        public final int size;   // 1 small .. 3 large
        public final double cost; // asumido costo por adopción

        public Dog(String id, boolean goodWithKids, boolean hasGardenNeeded, int energy, int size, double cost) {
            this.id = id; this.goodWithKids = goodWithKids; this.hasGardenNeeded = hasGardenNeeded;
            this.energy = energy; this.size = size; this.cost = cost;
        }
    }

    public static class AssignmentResult {
        public final List<Dog> assigned;
        public final double totalScore;
        public final double totalCost;
        public AssignmentResult(List<Dog> assigned, double totalScore, double totalCost) {
            this.assigned = assigned; this.totalScore = totalScore; this.totalCost = totalCost;
        }
    }

    // Scoring configurable simple ejemplo:
    private double scoreFor(Dog d, boolean adopterHasKids, boolean adopterHasGarden, double weights[]) {
        double s = 0;
        if (adopterHasKids && d.goodWithKids) s += weights[0]; // peso niños
        if (adopterHasGarden && d.hasGardenNeeded) s += weights[1]; // jardín
        // prefiero energía moderada: penaliza extremos
        s += weights[2] * (1.0 - Math.abs(d.energy - 5) / 5.0);
        // tamaño preferencia neutra -> pequeño un poco mejor
        s += weights[3] * (3.0 - d.size) / 2.0;
        return s;
    }

    public AssignmentResult scoreAndAssign(List<Dog> candidates,
                                           boolean adopterHasKids,
                                           boolean adopterHasGarden,
                                           int maxDogs,
                                           double budget) {
        double[] weights = {3.0, 2.0, 2.0, 1.0}; // niños, jardín, energía, tamaño
        List<ScoredDog> scored = candidates.stream()
                .map(d -> new ScoredDog(d, scoreFor(d, adopterHasKids, adopterHasGarden, weights)))
                .sorted(Comparator.comparingDouble((ScoredDog sd) -> sd.score).reversed())
                .collect(Collectors.toList());

        List<Dog> assigned = new ArrayList<>();
        double totalCost = 0, totalScore = 0;
        for (ScoredDog sd : scored) {
            if (assigned.size() >= maxDogs) break;
            if (totalCost + sd.dog.cost > budget) continue;
            assigned.add(sd.dog);
            totalCost += sd.dog.cost;
            totalScore += sd.score;
        }
        return new AssignmentResult(assigned, totalScore, totalCost);
    }

    private static class ScoredDog {
        final Dog dog;
        final double score;
        ScoredDog(Dog dog, double score) { this.dog = dog; this.score = score; }
    }
}

