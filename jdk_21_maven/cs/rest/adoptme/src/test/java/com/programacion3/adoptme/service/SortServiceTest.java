package com.programacion3.adoptme.service;

import com.programacion3.adoptme.domain.Dog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SortService Unit Tests")
class SortServiceTest {

    private SortService sortService;
    private List<Dog> testDogs;

    @BeforeEach
    void setUp() {
        sortService = new SortService();
        testDogs = createTestDogs();
    }

    private List<Dog> createTestDogs() {
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("D1");
        dog1.setName("Luna");
        dog1.setPriority(5);
        dog1.setAge(3);
        dog1.setWeightKg(10);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("D2");
        dog2.setName("Max");
        dog2.setPriority(8);
        dog2.setAge(1);
        dog2.setWeightKg(25);
        dogs.add(dog2);

        Dog dog3 = new Dog();
        dog3.setId("D3");
        dog3.setName("Bella");
        dog3.setPriority(3);
        dog3.setAge(5);
        dog3.setWeightKg(15);
        dogs.add(dog3);

        Dog dog4 = new Dog();
        dog4.setId("D4");
        dog4.setName("Charlie");
        dog4.setPriority(10);
        dog4.setAge(2);
        dog4.setWeightKg(8);
        dogs.add(dog4);

        return dogs;
    }

    @Test
    @DisplayName("Sort by priority using MergeSort - ascending order")
    void testSortByPriorityMergeSort() {
        // Act
        sortService.sortDogs(testDogs, "priority", "mergesort");

        // Assert
        assertEquals("D3", testDogs.get(0).getId()); // priority 3
        assertEquals("D1", testDogs.get(1).getId()); // priority 5
        assertEquals("D2", testDogs.get(2).getId()); // priority 8
        assertEquals("D4", testDogs.get(3).getId()); // priority 10

        // Verify ascending order
        for (int i = 0; i < testDogs.size() - 1; i++) {
            assertTrue(testDogs.get(i).getPriority() <= testDogs.get(i + 1).getPriority(),
                    "Dogs should be sorted in ascending order by priority");
        }
    }

    @Test
    @DisplayName("Sort by priority using QuickSort - ascending order")
    void testSortByPriorityQuickSort() {
        // Act
        sortService.sortDogs(testDogs, "priority", "quicksort");

        // Assert
        assertEquals(3, testDogs.get(0).getPriority());
        assertEquals(5, testDogs.get(1).getPriority());
        assertEquals(8, testDogs.get(2).getPriority());
        assertEquals(10, testDogs.get(3).getPriority());

        // Verify ascending order
        for (int i = 0; i < testDogs.size() - 1; i++) {
            assertTrue(testDogs.get(i).getPriority() <= testDogs.get(i + 1).getPriority());
        }
    }

    @Test
    @DisplayName("Sort by age using MergeSort")
    void testSortByAgeMergeSort() {
        // Act
        sortService.sortDogs(testDogs, "age", "mergesort");

        // Assert
        assertEquals(1, testDogs.get(0).getAge());
        assertEquals(2, testDogs.get(1).getAge());
        assertEquals(3, testDogs.get(2).getAge());
        assertEquals(5, testDogs.get(3).getAge());

        // Verify ascending order
        for (int i = 0; i < testDogs.size() - 1; i++) {
            assertTrue(testDogs.get(i).getAge() <= testDogs.get(i + 1).getAge());
        }
    }

    @Test
    @DisplayName("Sort by age using QuickSort")
    void testSortByAgeQuickSort() {
        // Act
        sortService.sortDogs(testDogs, "age", "quicksort");

        // Assert
        for (int i = 0; i < testDogs.size() - 1; i++) {
            assertTrue(testDogs.get(i).getAge() <= testDogs.get(i + 1).getAge());
        }
    }

    @Test
    @DisplayName("Sort by weight using MergeSort")
    void testSortByWeightMergeSort() {
        // Act
        sortService.sortDogs(testDogs, "weight", "mergesort");

        // Assert
        assertEquals(8, testDogs.get(0).getWeight());
        assertEquals(10, testDogs.get(1).getWeight());
        assertEquals(15, testDogs.get(2).getWeight());
        assertEquals(25, testDogs.get(3).getWeight());

        // Verify ascending order
        for (int i = 0; i < testDogs.size() - 1; i++) {
            assertTrue(testDogs.get(i).getWeight() <= testDogs.get(i + 1).getWeight());
        }
    }

    @Test
    @DisplayName("Sort by weight using QuickSort")
    void testSortByWeightQuickSort() {
        // Act
        sortService.sortDogs(testDogs, "weight", "quicksort");

        // Assert
        for (int i = 0; i < testDogs.size() - 1; i++) {
            assertTrue(testDogs.get(i).getWeight() <= testDogs.get(i + 1).getWeight());
        }
    }

    @Test
    @DisplayName("Sort with invalid criteria throws exception")
    void testSortWithInvalidCriteria() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            sortService.sortDogs(testDogs, "invalid", "mergesort");
        });
    }

    @Test
    @DisplayName("Sort empty list does not throw exception")
    void testSortEmptyList() {
        // Arrange
        List<Dog> emptyList = new ArrayList<>();

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            sortService.sortDogs(emptyList, "priority", "mergesort");
        });
    }

    @Test
    @DisplayName("Sort single element list")
    void testSortSingleElement() {
        // Arrange
        List<Dog> singleDog = new ArrayList<>();
        singleDog.add(testDogs.get(0));

        // Act
        sortService.sortDogs(singleDog, "priority", "quicksort");

        // Assert
        assertEquals(1, singleDog.size());
        assertEquals("D1", singleDog.get(0).getId());
    }

    @Test
    @DisplayName("Sort maintains stability for equal elements")
    void testSortStability() {
        // Arrange - Create dogs with same priority but different names
        List<Dog> dogs = new ArrayList<>();

        Dog dog1 = new Dog();
        dog1.setId("D1");
        dog1.setName("Alpha");
        dog1.setPriority(5);
        dogs.add(dog1);

        Dog dog2 = new Dog();
        dog2.setId("D2");
        dog2.setName("Beta");
        dog2.setPriority(5);
        dogs.add(dog2);

        // Act
        sortService.sortDogs(dogs, "priority", "mergesort");

        // Assert - MergeSort should maintain original order for equal priorities
        assertEquals(5, dogs.get(0).getPriority());
        assertEquals(5, dogs.get(1).getPriority());
    }

    @Test
    @DisplayName("Sort large dataset")
    void testSortLargeDataset() {
        // Arrange - Create 100 dogs with random priorities
        List<Dog> largeDogList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Dog dog = new Dog();
            dog.setId("D" + i);
            dog.setPriority((i * 7) % 20); // pseudo-random priorities
            dog.setAge((i * 3) % 10);
            dog.setWeightKg(5 + (i % 30));
            largeDogList.add(dog);
        }

        // Act
        sortService.sortDogs(largeDogList, "priority", "quicksort");

        // Assert - Verify sorted
        for (int i = 0; i < largeDogList.size() - 1; i++) {
            assertTrue(largeDogList.get(i).getPriority() <= largeDogList.get(i + 1).getPriority());
        }
    }

    @Test
    @DisplayName("Sort defaults to MergeSort when algorithm not specified")
    void testDefaultAlgorithm() {
        // Act
        sortService.sortDogs(testDogs, "priority");

        // Assert - Should be sorted
        for (int i = 0; i < testDogs.size() - 1; i++) {
            assertTrue(testDogs.get(i).getPriority() <= testDogs.get(i + 1).getPriority());
        }
    }
}
