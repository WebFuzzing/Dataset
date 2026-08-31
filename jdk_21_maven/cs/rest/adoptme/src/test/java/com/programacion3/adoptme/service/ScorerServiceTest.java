package com.programacion3.adoptme.service;

import com.programacion3.adoptme.service.ScorerService.Dog;
import com.programacion3.adoptme.service.ScorerService.AssignmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScorerService (Greedy) Unit Tests")
class ScorerServiceTest {

    private ScorerService scorerService;

    @BeforeEach
    void setUp() {
        scorerService = new ScorerService();
    }

    private List<Dog> createTestDogs() {
        List<Dog> dogs = new ArrayList<>();

        // Perfect for adopter with kids and garden
        dogs.add(new Dog("D1", true, true, 5, 2, 9000.0));  // goodWithKids, needsGarden, moderate energy

        // Not good with kids
        dogs.add(new Dog("D2", false, false, 7, 1, 7000.0)); // No kids, no garden

        // Needs garden
        dogs.add(new Dog("D3", true, true, 3, 3, 11000.0)); // goodWithKids, needsGarden, low energy, large

        // High energy
        dogs.add(new Dog("D4", true, false, 10, 1, 7000.0)); // goodWithKids, high energy

        // Low cost, good all-around
        dogs.add(new Dog("D5", true, false, 5, 1, 5000.0)); // Cheap, moderate

        return dogs;
    }

    // ==================== Basic Assignment Tests ====================

    @Test
    @DisplayName("Greedy: Empty candidates returns empty assignment")
    void testEmptyCandidates() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 3, 50000.0);

        // Assert
        assertTrue(result.assigned.isEmpty());
        assertEquals(0.0, result.totalScore, 0.01);
        assertEquals(0.0, result.totalCost, 0.01);
    }

    @Test
    @DisplayName("Greedy: Single dog within budget")
    void testSingleDogWithinBudget() {
        // Arrange
        List<Dog> dogs = Arrays.asList(
                new Dog("D1", true, true, 5, 2, 9000.0)
        );

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 3, 10000.0);

        // Assert
        assertEquals(1, result.assigned.size());
        assertEquals("D1", result.assigned.get(0).id);
        assertEquals(9000.0, result.totalCost, 0.01);
        assertTrue(result.totalScore > 0);
    }

    @Test
    @DisplayName("Greedy: Selects highest scoring dogs first")
    void testSelectsHighestScoring() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act - Adopter with kids and garden
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 5, 50000.0);

        // Assert
        assertFalse(result.assigned.isEmpty());
        assertTrue(result.totalCost <= 50000.0, "Should not exceed budget");

        // D1 and D3 should score highest (kids + garden match)
        List<String> assignedIds = new ArrayList<>();
        for (Dog dog : result.assigned) {
            assignedIds.add(dog.id);
        }
        assertTrue(assignedIds.contains("D1"), "Should include D1 (perfect match)");
    }

    // ==================== Budget Constraint Tests ====================

    @Test
    @DisplayName("Greedy: Respects budget constraint")
    void testRespectsBudget() {
        // Arrange
        List<Dog> dogs = createTestDogs();
        double budget = 15000.0;

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 10, budget);

        // Assert
        assertTrue(result.totalCost <= budget, "Total cost should not exceed budget");
    }

    @Test
    @DisplayName("Greedy: Zero budget returns empty")
    void testZeroBudget() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 3, 0.0);

        // Assert
        assertTrue(result.assigned.isEmpty());
        assertEquals(0.0, result.totalCost, 0.01);
    }

    @Test
    @DisplayName("Greedy: Tight budget selects cheaper dogs")
    void testTightBudget() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("Cheap1", true, true, 5, 1, 1000.0));
        dogs.add(new Dog("Cheap2", true, true, 5, 1, 2000.0));
        dogs.add(new Dog("Expensive", true, true, 5, 1, 50000.0));

        // Act - Budget only allows 2 cheap dogs
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 10, 3000.0);

        // Assert
        assertEquals(2, result.assigned.size());
        assertTrue(result.totalCost <= 3000.0);
    }

    @Test
    @DisplayName("Greedy: Large budget allows all dogs within maxDogs")
    void testLargeBudget() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 3, 100000.0);

        // Assert
        assertEquals(3, result.assigned.size(), "Should assign up to maxDogs limit");
        assertTrue(result.totalCost > 0);
    }

    // ==================== MaxDogs Constraint Tests ====================

    @Test
    @DisplayName("Greedy: Respects maxDogs limit")
    void testRespectsMaxDogs() {
        // Arrange
        List<Dog> dogs = createTestDogs();
        int maxDogs = 2;

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, maxDogs, 50000.0);

        // Assert
        assertTrue(result.assigned.size() <= maxDogs, "Should not exceed maxDogs");
    }

    @Test
    @DisplayName("Greedy: MaxDogs of zero returns empty")
    void testMaxDogsZero() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 0, 50000.0);

        // Assert
        assertTrue(result.assigned.isEmpty());
    }

    @Test
    @DisplayName("Greedy: MaxDogs of one selects best dog")
    void testMaxDogsOne() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 1, 50000.0);

        // Assert
        assertEquals(1, result.assigned.size());
        // Should select highest scoring dog that fits budget
    }

    // ==================== Scoring Criteria Tests ====================

    @Test
    @DisplayName("Greedy: Kids constraint affects scoring")
    void testKidsConstraint() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("GoodWithKids", true, false, 5, 2, 9000.0));
        dogs.add(new Dog("NotGoodWithKids", false, false, 5, 2, 9000.0));

        // Act - Adopter with kids
        AssignmentResult withKids = scorerService.scoreAndAssign(dogs, true, false, 5, 50000.0);

        // Act - Adopter without kids
        AssignmentResult withoutKids = scorerService.scoreAndAssign(dogs, false, false, 5, 50000.0);

        // Assert
        // With kids: should prefer GoodWithKids (higher score)
        if (!withKids.assigned.isEmpty()) {
            assertEquals("GoodWithKids", withKids.assigned.get(0).id);
        }

        // Without kids: both have similar scores, but both should be acceptable
        assertFalse(withoutKids.assigned.isEmpty());
    }

    @Test
    @DisplayName("Greedy: Garden constraint affects scoring")
    void testGardenConstraint() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("NeedsGarden", true, true, 5, 2, 9000.0));
        dogs.add(new Dog("NoGarden", true, false, 5, 2, 9000.0));

        // Act - Adopter with garden
        AssignmentResult withGarden = scorerService.scoreAndAssign(dogs, true, true, 5, 50000.0);

        // Act - Adopter without garden
        AssignmentResult withoutGarden = scorerService.scoreAndAssign(dogs, true, false, 5, 50000.0);

        // Assert
        // With garden: should prefer NeedsGarden (higher score)
        if (!withGarden.assigned.isEmpty()) {
            assertEquals("NeedsGarden", withGarden.assigned.get(0).id);
        }

        // Without garden: NoGarden should score higher
        if (!withoutGarden.assigned.isEmpty()) {
            assertEquals("NoGarden", withoutGarden.assigned.get(0).id);
        }
    }

    @Test
    @DisplayName("Greedy: Energy preference affects scoring")
    void testEnergyPreference() {
        // Arrange - Dogs with different energy levels
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("LowEnergy", true, true, 1, 2, 9000.0));    // Energy 1
        dogs.add(new Dog("MidEnergy", true, true, 5, 2, 9000.0));    // Energy 5
        dogs.add(new Dog("HighEnergy", true, true, 10, 2, 9000.0));  // Energy 10

        // Act - All else equal, moderate energy (5) should score best
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 1, 10000.0);

        // Assert
        assertEquals(1, result.assigned.size());
        // Moderate energy (5) should be preferred (closest to ideal 5)
        assertEquals("MidEnergy", result.assigned.get(0).id);
    }

    // ==================== Size Preference Tests ====================

    @Test
    @DisplayName("Greedy: Size affects scoring slightly")
    void testSizePreference() {
        // Arrange - Dogs with different sizes, all else equal
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("Small", true, true, 5, 1, 7000.0));   // Size 1
        dogs.add(new Dog("Medium", true, true, 5, 2, 9000.0));  // Size 2
        dogs.add(new Dog("Large", true, true, 5, 3, 11000.0));  // Size 3

        // Act - With large budget, should prefer smaller (slightly higher score)
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 1, 8000.0);

        // Assert
        assertEquals(1, result.assigned.size());
        assertEquals("Small", result.assigned.get(0).id);
    }

    // ==================== Combined Constraints Tests ====================

    @Test
    @DisplayName("Greedy: Multiple constraints combined")
    void testMultipleConstraints() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act - Adopter: has kids, has garden, maxDogs=2, budget=20000
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 2, 20000.0);

        // Assert
        assertTrue(result.assigned.size() <= 2, "Should respect maxDogs");
        assertTrue(result.totalCost <= 20000.0, "Should respect budget");

        // Verify all assigned dogs are good with kids
        for (Dog dog : result.assigned) {
            assertTrue(dog.goodWithKids, "All assigned dogs should be good with kids");
        }
    }

    @Test
    @DisplayName("Greedy: No kids adopter excludes bad-with-kids dogs in prioritization")
    void testNoKidsAdopter() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("Perfect1", true, false, 5, 2, 9000.0));
        dogs.add(new Dog("Perfect2", false, false, 5, 2, 9000.0));

        // Act - Adopter without kids
        AssignmentResult result = scorerService.scoreAndAssign(dogs, false, false, 5, 50000.0);

        // Assert - Both dogs should be acceptable
        assertEquals(2, result.assigned.size());
    }

    // ==================== Score and Cost Validation ====================

    @Test
    @DisplayName("Greedy: Total cost equals sum of assigned dog costs")
    void testTotalCostCalculation() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 3, 30000.0);

        // Assert
        double calculatedCost = 0.0;
        for (Dog dog : result.assigned) {
            calculatedCost += dog.cost;
        }
        assertEquals(calculatedCost, result.totalCost, 0.01);
    }

    @Test
    @DisplayName("Greedy: Total score is positive when dogs assigned")
    void testTotalScorePositive() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 3, 30000.0);

        // Assert
        if (!result.assigned.isEmpty()) {
            assertTrue(result.totalScore > 0, "Total score should be positive when dogs are assigned");
        }
    }

    @Test
    @DisplayName("Greedy: Better match produces higher score")
    void testBetterMatchHigherScore() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("PerfectMatch", true, true, 5, 1, 5000.0));

        // Act
        AssignmentResult perfectMatch = scorerService.scoreAndAssign(dogs, true, true, 1, 10000.0);
        AssignmentResult poorMatch = scorerService.scoreAndAssign(dogs, false, false, 1, 10000.0);

        // Assert
        assertTrue(perfectMatch.totalScore >= poorMatch.totalScore,
                "Perfect match should have equal or higher score than poor match");
    }

    // ==================== Greedy Selection Order Tests ====================

    @Test
    @DisplayName("Greedy: Selects dogs in descending score order")
    void testGreedySelectionOrder() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("Best", true, true, 5, 1, 1000.0));     // Highest score
        dogs.add(new Dog("Good", true, false, 5, 1, 1000.0));    // Medium score
        dogs.add(new Dog("OK", false, false, 5, 1, 1000.0));     // Lower score

        // Act - With kids and garden, "Best" should be selected first
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 1, 10000.0);

        // Assert
        assertEquals(1, result.assigned.size());
        assertEquals("Best", result.assigned.get(0).id);
    }

    @Test
    @DisplayName("Greedy: Fills capacity with highest scoring affordable dogs")
    void testFillsCapacityOptimally() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("D1", true, true, 5, 1, 5000.0));   // High score
        dogs.add(new Dog("D2", true, true, 5, 1, 5000.0));   // High score
        dogs.add(new Dog("D3", true, true, 5, 1, 5000.0));   // High score
        dogs.add(new Dog("D4", false, false, 8, 3, 5000.0)); // Lower score

        // Act - Can afford 3 dogs
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 5, 15000.0);

        // Assert
        assertEquals(3, result.assigned.size());
        // Should select D1, D2, D3 (highest scores)
        List<String> assignedIds = new ArrayList<>();
        for (Dog dog : result.assigned) {
            assignedIds.add(dog.id);
        }
        assertTrue(assignedIds.contains("D1"));
        assertTrue(assignedIds.contains("D2"));
        assertTrue(assignedIds.contains("D3"));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Greedy: Negative max dogs returns empty")
    void testNegativeMaxDogs() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, -1, 50000.0);

        // Assert
        assertTrue(result.assigned.isEmpty());
    }

    @Test
    @DisplayName("Greedy: All dogs too expensive")
    void testAllDogsTooExpensive() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("Expensive1", true, true, 5, 2, 50000.0));
        dogs.add(new Dog("Expensive2", true, true, 5, 2, 60000.0));

        // Act
        AssignmentResult result = scorerService.scoreAndAssign(dogs, true, true, 5, 1000.0);

        // Assert
        assertTrue(result.assigned.isEmpty(), "Should not assign any dogs if all exceed budget");
        assertEquals(0.0, result.totalCost, 0.01);
    }
}
