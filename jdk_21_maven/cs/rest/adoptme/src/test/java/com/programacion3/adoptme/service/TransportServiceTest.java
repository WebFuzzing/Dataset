package com.programacion3.adoptme.service;

import com.programacion3.adoptme.domain.Dog;
import com.programacion3.adoptme.service.TransportService.KnapsackResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransportService (Knapsack DP) Unit Tests")
class TransportServiceTest {

    private TransportService transportService;

    @BeforeEach
    void setUp() {
        transportService = new TransportService();
    }

    private List<Dog> createTestDogs() {
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("D1");
        dog1.setName("Luna");
        dog1.setPriority(10);
        dog1.setWeightKg(8);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("D2");
        dog2.setName("Max");
        dog2.setPriority(7);
        dog2.setWeightKg(15);
        dogs.add(dog2);

        Dog dog3 = new Dog();
        dog3.setId("D3");
        dog3.setName("Bella");
        dog3.setPriority(5);
        dog3.setWeightKg(10);
        dogs.add(dog3);

        Dog dog4 = new Dog();
        dog4.setId("D4");
        dog4.setName("Charlie");
        dog4.setPriority(3);
        dog4.setWeightKg(6);
        dogs.add(dog4);

        return dogs;
    }

    // ==================== Basic Knapsack Tests ====================

    @Test
    @DisplayName("Knapsack: Empty dogs returns zero")
    void testEmptyDogs() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 50);

        // Assert
        assertNotNull(result);
        assertTrue(result.selectedDogs.isEmpty());
        assertEquals(0, result.totalPriority);
        assertEquals(0, result.totalWeight);
    }

    @Test
    @DisplayName("Knapsack: Null dogs returns empty result")
    void testNullDogs() {
        // Act
        KnapsackResult result = transportService.optimizeTransport(null, 50);

        // Assert
        assertNotNull(result);
        assertTrue(result.selectedDogs.isEmpty());
        assertEquals(0, result.totalPriority);
        assertEquals(0, result.totalWeight);
    }

    @Test
    @DisplayName("Knapsack: Zero capacity returns empty")
    void testZeroCapacity() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 0);

        // Assert
        assertNotNull(result);
        assertTrue(result.selectedDogs.isEmpty());
        assertEquals(0, result.totalPriority);
        assertEquals(0, result.totalWeight);
    }

    @Test
    @DisplayName("Knapsack: Negative capacity returns empty")
    void testNegativeCapacity() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, -10);

        // Assert
        assertNotNull(result);
        assertTrue(result.selectedDogs.isEmpty());
        assertEquals(0, result.totalPriority);
        assertEquals(0, result.totalWeight);
    }

    @Test
    @DisplayName("Knapsack: Single dog within capacity")
    void testSingleDogWithinCapacity() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        Dog dog = new Dog();
        dog.setId("D1");
        dog.setPriority(10);
        dog.setWeightKg(8);
        dogs.add(dog);

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 10);

        // Assert
        assertEquals(1, result.selectedDogs.size());
        assertEquals("D1", result.selectedDogs.get(0).getId());
        assertEquals(10, result.totalPriority);
        assertEquals(8, result.totalWeight);
    }

    @Test
    @DisplayName("Knapsack: Single dog exceeds capacity")
    void testSingleDogExceedsCapacity() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        Dog dog = new Dog();
        dog.setId("D1");
        dog.setPriority(10);
        dog.setWeightKg(15);
        dogs.add(dog);

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 10);

        // Assert
        assertTrue(result.selectedDogs.isEmpty());
        assertEquals(0, result.totalPriority);
        assertEquals(0, result.totalWeight);
    }

    // ==================== Optimization Tests ====================

    @Test
    @DisplayName("Knapsack: Selects optimal subset")
    void testSelectsOptimalSubset() {
        // Arrange
        List<Dog> dogs = createTestDogs();
        // D1: 8kg, priority 10
        // D2: 15kg, priority 7
        // D3: 10kg, priority 5
        // D4: 6kg, priority 3

        // Act - Capacity 20kg
        KnapsackResult result = transportService.optimizeTransport(dogs, 20);

        // Assert
        // Optimal: D1 (8kg, 10) + D3 (10kg, 5) = 18kg, priority 15
        // OR: D1 (8kg, 10) + D4 (6kg, 3) = 14kg, priority 13
        // OR: D2 (15kg, 7) + D4 (6kg, 3) = 21kg > 20kg (invalid)
        // Best valid: D1 + D3 = priority 15

        assertTrue(result.totalWeight <= 20, "Total weight should not exceed capacity");
        assertEquals(15, result.totalPriority, "Should maximize priority");
        assertEquals(18, result.totalWeight);
    }

    @Test
    @DisplayName("Knapsack: Prefers higher priority when weights equal")
    void testPrefersHigherPriority() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("HighPriority");
        dog1.setPriority(10);
        dog1.setWeightKg(10);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("LowPriority");
        dog2.setPriority(3);
        dog2.setWeightKg(10);
        dogs.add(dog2);

        // Act - Can fit only one
        KnapsackResult result = transportService.optimizeTransport(dogs, 10);

        // Assert
        assertEquals(1, result.selectedDogs.size());
        assertEquals("HighPriority", result.selectedDogs.get(0).getId());
        assertEquals(10, result.totalPriority);
    }

    @Test
    @DisplayName("Knapsack: Chooses multiple small over one large when beneficial")
    void testChoosesMultipleSmall() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();

        Dog large = new Dog();
        large.setId("Large");
        large.setPriority(10);
        large.setWeightKg(20);
        dogs.add(large);

        Dog small1 = new Dog();
        small1.setId("Small1");
        small1.setPriority(7);
        small1.setWeightKg(10);
        dogs.add(small1);

        Dog small2 = new Dog();
        small2.setId("Small2");
        small2.setPriority(7);
        small2.setWeightKg(10);
        dogs.add(small2);

        // Act - Capacity 20kg
        KnapsackResult result = transportService.optimizeTransport(dogs, 20);

        // Assert
        // Two small dogs (7+7=14) > one large (10)
        assertEquals(2, result.selectedDogs.size());
        assertEquals(14, result.totalPriority);
        assertEquals(20, result.totalWeight);
    }

    // ==================== Capacity Tests ====================

    @Test
    @DisplayName("Knapsack: Does not exceed capacity")
    void testDoesNotExceedCapacity() {
        // Arrange
        List<Dog> dogs = createTestDogs();
        int capacity = 25;

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, capacity);

        // Assert
        assertTrue(result.totalWeight <= capacity,
                "Total weight should not exceed capacity: " + result.totalWeight + " <= " + capacity);
    }

    @Test
    @DisplayName("Knapsack: Exact capacity fit")
    void testExactCapacityFit() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("D1");
        dog1.setPriority(5);
        dog1.setWeightKg(10);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("D2");
        dog2.setPriority(5);
        dog2.setWeightKg(10);
        dogs.add(dog2);

        // Act - Exact fit
        KnapsackResult result = transportService.optimizeTransport(dogs, 20);

        // Assert
        assertEquals(2, result.selectedDogs.size());
        assertEquals(20, result.totalWeight, "Should use exactly capacity");
        assertEquals(10, result.totalPriority);
    }

    @Test
    @DisplayName("Knapsack: Large capacity includes all dogs")
    void testLargeCapacity() {
        // Arrange
        List<Dog> dogs = createTestDogs();
        int totalWeight = dogs.stream().mapToInt(Dog::getWeight).sum();

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 1000);

        // Assert
        assertEquals(4, result.selectedDogs.size(), "Should include all dogs");
        assertEquals(totalWeight, result.totalWeight);
        assertEquals(25, result.totalPriority); // 10+7+5+3
    }

    // ==================== Priority Calculation Tests ====================

    @Test
    @DisplayName("Knapsack: Total priority equals sum of selected dogs")
    void testTotalPriorityCalculation() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 30);

        // Assert
        int calculatedPriority = result.selectedDogs.stream()
                .mapToInt(Dog::getPriority)
                .sum();

        assertEquals(calculatedPriority, result.totalPriority,
                "Total priority should equal sum of selected dogs' priorities");
    }

    @Test
    @DisplayName("Knapsack: Total weight equals sum of selected dogs")
    void testTotalWeightCalculation() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 30);

        // Assert
        int calculatedWeight = result.selectedDogs.stream()
                .mapToInt(Dog::getWeight)
                .sum();

        assertEquals(calculatedWeight, result.totalWeight,
                "Total weight should equal sum of selected dogs' weights");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Knapsack: All dogs exceed capacity")
    void testAllDogsExceedCapacity() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Dog dog = new Dog();
            dog.setId("D" + i);
            dog.setPriority(10);
            dog.setWeightKg(20);
            dogs.add(dog);
        }

        // Act - Capacity too small for any dog
        KnapsackResult result = transportService.optimizeTransport(dogs, 10);

        // Assert
        assertTrue(result.selectedDogs.isEmpty());
        assertEquals(0, result.totalPriority);
        assertEquals(0, result.totalWeight);
    }

    @Test
    @DisplayName("Knapsack: Dogs with zero priority")
    void testZeroPriorityDogs() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("D1");
        dog1.setPriority(0);
        dog1.setWeightKg(5);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("D2");
        dog2.setPriority(10);
        dog2.setWeightKg(5);
        dogs.add(dog2);

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 5);

        // Assert
        assertEquals(1, result.selectedDogs.size());
        assertEquals("D2", result.selectedDogs.get(0).getId(), "Should prefer non-zero priority");
    }

    @Test
    @DisplayName("Knapsack: Dogs with zero weight")
    void testZeroWeightDogs() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("D1");
        dog1.setPriority(10);
        dog1.setWeightKg(0);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("D2");
        dog2.setPriority(5);
        dog2.setWeightKg(10);
        dogs.add(dog2);

        // Act
        KnapsackResult result = transportService.optimizeTransport(dogs, 10);

        // Assert
        // Should include both (zero weight dog is free)
        assertEquals(2, result.selectedDogs.size());
        assertEquals(15, result.totalPriority);
    }

    // ==================== Larger Dataset Tests ====================

    @Test
    @DisplayName("Knapsack: Handles 20 dogs efficiently")
    void testLargerDataset() {
        // Arrange - 20 dogs with varied weights and priorities
        List<Dog> dogs = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Dog dog = new Dog();
            dog.setId("D" + i);
            dog.setPriority(i);
            dog.setWeightKg(i * 2);
            dogs.add(dog);
        }

        // Act
        long start = System.currentTimeMillis();
        KnapsackResult result = transportService.optimizeTransport(dogs, 50);
        long duration = System.currentTimeMillis() - start;

        // Assert
        assertNotNull(result);
        assertTrue(result.totalWeight <= 50);
        assertTrue(result.totalPriority > 0);
        assertTrue(duration < 5000, "Should complete within 5 seconds");
    }

    @Test
    @DisplayName("Knapsack: Handles 50 dogs efficiently")
    void testVeryLargeDataset() {
        // Arrange - 50 dogs
        List<Dog> dogs = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Dog dog = new Dog();
            dog.setId("D" + i);
            dog.setPriority((i % 10) + 1);
            dog.setWeightKg((i % 15) + 5);
            dogs.add(dog);
        }

        // Act
        long start = System.currentTimeMillis();
        KnapsackResult result = transportService.optimizeTransport(dogs, 100);
        long duration = System.currentTimeMillis() - start;

        // Assert
        assertNotNull(result);
        assertTrue(result.totalWeight <= 100);
        assertTrue(result.selectedDogs.size() > 0);
        assertTrue(duration < 10000, "Should complete within 10 seconds");
    }

    // ==================== Dynamic Programming Correctness ====================

    @Test
    @DisplayName("Knapsack: Classic DP test case")
    void testClassicKnapsackCase() {
        // Arrange - Classic knapsack problem
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("D1");
        dog1.setWeightKg(10);
        dog1.setPriority(60);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("D2");
        dog2.setWeightKg(20);
        dog2.setPriority(100);
        dogs.add(dog2);

        Dog dog3 = new Dog();
        dog3.setId("D3");
        dog3.setWeightKg(30);
        dog3.setPriority(120);
        dogs.add(dog3);

        // Act - Capacity 50
        KnapsackResult result = transportService.optimizeTransport(dogs, 50);

        // Assert
        // Optimal: D2 + D3 = 50kg, priority 220
        // Alternative: All three = 60kg > 50 (invalid)
        // Alternative: D1 + D3 = 40kg, priority 180
        assertEquals(220, result.totalPriority, "Should find optimal combination");
        assertEquals(50, result.totalWeight);
        assertEquals(2, result.selectedDogs.size());
    }

    @Test
    @DisplayName("Knapsack: Fractional capacity not allowed (0/1 knapsack)")
    void testZeroOneKnapsack() {
        // Arrange
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("Perfect");
        dog1.setWeightKg(15);
        dog1.setPriority(100);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("AlmostPerfect");
        dog2.setWeightKg(14);
        dog2.setPriority(99);
        dogs.add(dog2);

        // Act - Capacity 15 (can only fit Perfect, not both)
        KnapsackResult result = transportService.optimizeTransport(dogs, 15);

        // Assert
        assertEquals(1, result.selectedDogs.size(), "Should select exactly one dog (0/1 property)");
        assertEquals("Perfect", result.selectedDogs.get(0).getId());
        assertEquals(100, result.totalPriority);
    }

    // ==================== Result Consistency Tests ====================

    @Test
    @DisplayName("Knapsack: Result is deterministic")
    void testResultIsDeterministic() {
        // Arrange
        List<Dog> dogs = createTestDogs();

        // Act - Run twice
        KnapsackResult result1 = transportService.optimizeTransport(dogs, 25);
        KnapsackResult result2 = transportService.optimizeTransport(dogs, 25);

        // Assert
        assertEquals(result1.totalPriority, result2.totalPriority,
                "Same input should produce same priority");
        assertEquals(result1.totalWeight, result2.totalWeight,
                "Same input should produce same weight");
        assertEquals(result1.selectedDogs.size(), result2.selectedDogs.size(),
                "Same input should select same number of dogs");
    }
}
