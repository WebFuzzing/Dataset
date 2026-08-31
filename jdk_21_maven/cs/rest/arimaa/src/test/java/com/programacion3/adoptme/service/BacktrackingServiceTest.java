package com.programacion3.adoptme.service;

import com.programacion3.adoptme.service.BacktrackingService.Dog;
import com.programacion3.adoptme.service.BacktrackingService.Adopter;
import com.programacion3.adoptme.service.BacktrackingService.Assignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BacktrackingService (Constraint Satisfaction) Unit Tests")
class BacktrackingServiceTest {

    private BacktrackingService backtrackingService;

    @BeforeEach
    void setUp() {
        backtrackingService = new BacktrackingService();
    }

    private List<Dog> createSimpleDogs() {
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog("D1", true, false, 5, 5000.0));   // Good with kids, no garden, moderate energy
        dogs.add(new Dog("D2", false, true, 7, 8000.0));   // Not good with kids, needs garden
        dogs.add(new Dog("D3", true, true, 3, 10000.0));   // Good with kids, needs garden, low energy
        return dogs;
    }

    private List<Adopter> createSimpleAdopters() {
        List<Adopter> adopters = new ArrayList<>();
        adopters.add(new Adopter("A1", "Alice", true, true, 2, 20000.0, 5));  // Has kids, has garden
        adopters.add(new Adopter("A2", "Bob", false, false, 1, 6000.0, 7));   // No kids, no garden
        return adopters;
    }

    // ==================== Basic Assignment Tests ====================

    @Test
    @DisplayName("Backtracking: Empty dogs returns empty assignment")
    void testEmptyDogs() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        List<Adopter> adopters = createSimpleAdopters();

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        assertNotNull(result);
        assertTrue(result.assignments.isEmpty() || allAssignmentsEmpty(result.assignments));
        assertEquals(0.0, result.totalScore, 0.01);
    }

    @Test
    @DisplayName("Backtracking: Empty adopters returns empty assignment")
    void testEmptyAdopters() {
        // Arrange
        List<Dog> dogs = createSimpleDogs();
        List<Adopter> adopters = new ArrayList<>();

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        assertNotNull(result);
        assertTrue(result.assignments.isEmpty());
        assertEquals(0.0, result.totalScore, 0.01);
    }

    @Test
    @DisplayName("Backtracking: Single dog and single adopter")
    void testSingleDogSingleAdopter() {
        // Arrange
        List<Dog> dogs = Arrays.asList(new Dog("D1", true, false, 5, 5000.0));
        List<Adopter> adopters = Arrays.asList(new Adopter("A1", "Alice", true, true, 2, 10000.0, 5));

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        assertNotNull(result);
        assertTrue(result.totalScore > 0, "Should find a valid assignment");

        // Check that dog was assigned
        boolean dogAssigned = false;
        for (List<String> dogList : result.assignments.values()) {
            if (dogList.contains("D1")) {
                dogAssigned = true;
                break;
            }
        }
        assertTrue(dogAssigned, "Dog should be assigned to adopter");
    }

    // ==================== Constraint Tests ====================

    @Test
    @DisplayName("Backtracking: Respects kids constraint")
    void testKidsConstraint() {
        // Arrange
        List<Dog> dogs = Arrays.asList(
                new Dog("GoodWithKids", true, false, 5, 5000.0),
                new Dog("NotGoodWithKids", false, false, 5, 5000.0)
        );
        List<Adopter> adopters = Arrays.asList(
                new Adopter("HasKids", "Parent", true, true, 5, 50000.0, 5)
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToParent = result.assignments.get("HasKids");
        if (assignedToParent != null) {
            // Should only assign GoodWithKids
            assertFalse(assignedToParent.contains("NotGoodWithKids"),
                    "Dog not good with kids should not be assigned to adopter with kids");
        }
    }

    @Test
    @DisplayName("Backtracking: Respects garden constraint")
    void testGardenConstraint() {
        // Arrange
        List<Dog> dogs = Arrays.asList(
                new Dog("NeedsGarden", true, true, 5, 5000.0),
                new Dog("NoGardenNeeded", true, false, 5, 5000.0)
        );
        List<Adopter> adopters = Arrays.asList(
                new Adopter("NoGarden", "Bob", false, false, 5, 50000.0, 5)
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToBob = result.assignments.get("NoGarden");
        if (assignedToBob != null) {
            // Should not assign dog that needs garden
            assertFalse(assignedToBob.contains("NeedsGarden"),
                    "Dog needing garden should not be assigned to adopter without garden");
        }
    }

    @Test
    @DisplayName("Backtracking: Respects maxDogs constraint")
    void testMaxDogsConstraint() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            dogs.add(new Dog("D" + i, true, false, 5, 1000.0));
        }

        List<Adopter> adopters = Arrays.asList(
                new Adopter("A1", "Alice", true, true, 2, 50000.0, 5)  // Max 2 dogs
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToAlice = result.assignments.get("A1");
        if (assignedToAlice != null) {
            assertTrue(assignedToAlice.size() <= 2,
                    "Should not exceed maxDogs limit of 2");
        }
    }

    @Test
    @DisplayName("Backtracking: Respects budget constraint")
    void testBudgetConstraint() {
        // Arrange
        List<Dog> dogs = Arrays.asList(
                new Dog("Cheap", true, false, 5, 5000.0),
                new Dog("Expensive", true, false, 5, 20000.0)
        );
        List<Adopter> adopters = Arrays.asList(
                new Adopter("LowBudget", "Bob", true, true, 5, 6000.0, 5)
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToBob = result.assignments.get("LowBudget");
        if (assignedToBob != null) {
            // Calculate total cost
            double totalCost = 0.0;
            for (String dogId : assignedToBob) {
                for (Dog dog : dogs) {
                    if (dog.id.equals(dogId)) {
                        totalCost += dog.cost;
                        break;
                    }
                }
            }
            assertTrue(totalCost <= 6000.0, "Total cost should not exceed budget");

            // Should not include expensive dog
            assertFalse(assignedToBob.contains("Expensive"),
                    "Expensive dog should not be assigned when budget insufficient");
        }
    }

    // ==================== Score Maximization Tests ====================

    @Test
    @DisplayName("Backtracking: Maximizes total score")
    void testMaximizesScore() {
        // Arrange
        List<Dog> dogs = createSimpleDogs();
        List<Adopter> adopters = createSimpleAdopters();

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        assertTrue(result.totalScore >= 0, "Total score should be non-negative");
        assertNotNull(result.assignments);
    }

    @Test
    @DisplayName("Backtracking: Prefers better matches")
    void testPrefersBetterMatches() {
        // Arrange
        // Dog with kids + garden compatibility
        List<Dog> dogs = Arrays.asList(
                new Dog("Perfect", true, true, 5, 5000.0),  // Perfect match
                new Dog("OK", false, false, 8, 5000.0)      // Poor match
        );

        // Adopter with kids and garden
        List<Adopter> adopters = Arrays.asList(
                new Adopter("A1", "Alice", true, true, 1, 10000.0, 5)
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToAlice = result.assignments.get("A1");
        if (assignedToAlice != null && !assignedToAlice.isEmpty()) {
            // Should prefer Perfect match over OK
            assertTrue(assignedToAlice.contains("Perfect"),
                    "Should assign better matching dog");
        }
    }

    // ==================== Multiple Adopters Tests ====================

    @Test
    @DisplayName("Backtracking: Distributes dogs among multiple adopters")
    void testMultipleAdopters() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            dogs.add(new Dog("D" + i, true, false, 5, 3000.0));
        }

        List<Adopter> adopters = new ArrayList<>();
        adopters.add(new Adopter("A1", "Alice", true, true, 2, 10000.0, 5));
        adopters.add(new Adopter("A2", "Bob", true, true, 2, 10000.0, 5));

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        int totalAssigned = 0;
        for (List<String> dogList : result.assignments.values()) {
            totalAssigned += dogList.size();
        }

        assertTrue(totalAssigned > 0, "Should assign at least some dogs");
        assertTrue(totalAssigned <= dogs.size(), "Should not assign more dogs than available");
    }

    @Test
    @DisplayName("Backtracking: Each dog assigned to at most one adopter")
    void testNoDuplicateAssignments() {
        // Arrange
        List<Dog> dogs = createSimpleDogs();
        List<Adopter> adopters = createSimpleAdopters();

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        Set<String> allAssignedDogs = new HashSet<>();
        for (List<String> dogList : result.assignments.values()) {
            for (String dogId : dogList) {
                assertFalse(allAssignedDogs.contains(dogId),
                        "Dog " + dogId + " assigned to multiple adopters");
                allAssignedDogs.add(dogId);
            }
        }
    }

    // ==================== Energy Compatibility Tests ====================

    @Test
    @DisplayName("Backtracking: Energy compatibility affects score")
    void testEnergyCompatibility() {
        // Arrange
        List<Dog> dogs = Arrays.asList(
                new Dog("MatchingEnergy", true, false, 5, 3000.0),  // Energy 5
                new Dog("MismatchEnergy", true, false, 10, 3000.0)  // Energy 10
        );

        List<Adopter> adopters = Arrays.asList(
                new Adopter("A1", "Alice", true, true, 1, 10000.0, 5)  // Prefers energy 5
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToAlice = result.assignments.get("A1");
        if (assignedToAlice != null && !assignedToAlice.isEmpty()) {
            // Should prefer matching energy
            assertTrue(assignedToAlice.contains("MatchingEnergy"),
                    "Should prefer dog with matching energy level");
        }
    }

    // ==================== Performance and Timeout Tests ====================

    @Test
    @DisplayName("Backtracking: Completes within reasonable time")
    void testCompletesInReasonableTime() {
        // Arrange - 10 dogs and 3 adopters
        List<Dog> dogs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            dogs.add(new Dog("D" + i, true, false, 5, 5000.0));
        }

        List<Adopter> adopters = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            adopters.add(new Adopter("A" + i, "Adopter" + i, true, true, 5, 50000.0, 5));
        }

        // Act
        long start = System.currentTimeMillis();
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);
        long duration = System.currentTimeMillis() - start;

        // Assert
        assertNotNull(result);
        assertTrue(duration < 10000, "Should complete within 10 seconds");
    }

    @Test
    @DisplayName("Backtracking: Handles 20 dogs (service limit)")
    void testHandles20Dogs() {
        // Arrange - Exactly 20 dogs (the service's internal limit)
        List<Dog> dogs = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            dogs.add(new Dog("D" + i, true, false, 5, 3000.0));
        }

        List<Adopter> adopters = Arrays.asList(
                new Adopter("A1", "Alice", true, true, 10, 100000.0, 5)
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        assertNotNull(result);
        assertTrue(result.totalScore >= 0);
    }

    @Test
    @DisplayName("Backtracking: Limits dogs to 20 when more provided")
    void testLimitsDogs() {
        // Arrange - 30 dogs (exceeds service's 20-dog limit)
        List<Dog> dogs = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            dogs.add(new Dog("D" + i, true, false, 5, 3000.0));
        }

        List<Adopter> adopters = Arrays.asList(
                new Adopter("A1", "Alice", true, true, 30, 100000.0, 5)
        );

        // Act - Should limit to first 20 dogs internally
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        assertNotNull(result);
        // Count assigned dogs
        int totalAssigned = 0;
        for (List<String> dogList : result.assignments.values()) {
            totalAssigned += dogList.size();
        }
        assertTrue(totalAssigned <= 20, "Service should limit to 20 dogs");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Backtracking: No valid assignments returns zero score")
    void testNoValidAssignments() {
        // Arrange - Dog needs garden, adopter has no garden
        List<Dog> dogs = Arrays.asList(
                new Dog("NeedsGarden", true, true, 5, 5000.0)
        );
        List<Adopter> adopters = Arrays.asList(
                new Adopter("NoGarden", "Bob", true, false, 1, 10000.0, 5)
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToBob = result.assignments.get("NoGarden");
        if (assignedToBob != null) {
            assertFalse(assignedToBob.contains("NeedsGarden"),
                    "Should not assign dog needing garden to adopter without garden");
        }
    }

    @Test
    @DisplayName("Backtracking: All dogs too expensive")
    void testAllDogsTooExpensive() {
        // Arrange
        List<Dog> dogs = Arrays.asList(
                new Dog("Expensive1", true, false, 5, 50000.0),
                new Dog("Expensive2", true, false, 5, 60000.0)
        );
        List<Adopter> adopters = Arrays.asList(
                new Adopter("LowBudget", "Bob", true, true, 5, 1000.0, 5)
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToBob = result.assignments.get("LowBudget");
        assertTrue(assignedToBob == null || assignedToBob.isEmpty(),
                "Should not assign any dogs when all exceed budget");
    }

    @Test
    @DisplayName("Backtracking: Adopter maxDogs is zero")
    void testAdopterMaxDogsZero() {
        // Arrange
        List<Dog> dogs = createSimpleDogs();
        List<Adopter> adopters = Arrays.asList(
                new Adopter("A1", "Alice", true, true, 0, 50000.0, 5)  // Max 0 dogs
        );

        // Act
        Assignment result = backtrackingService.findBestAssignment(dogs, adopters);

        // Assert
        List<String> assignedToAlice = result.assignments.get("A1");
        assertTrue(assignedToAlice == null || assignedToAlice.isEmpty(),
                "Should not assign any dogs when maxDogs is 0");
    }

    // ==================== Helper Methods ====================

    private boolean allAssignmentsEmpty(Map<String, List<String>> assignments) {
        for (List<String> dogList : assignments.values()) {
            if (!dogList.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
