package com.programacion3.adoptme.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.programacion3.adoptme.domain.Dog;


@Service
public class SortService {

    /**
     * Ordena perros usando TimSort (default) o QuickSort.
     * @param dogs lista de perros a ordenar (se modifica in-place)
     * @param criteria criterio de ordenamiento (priority, age, weight)
     * @param algorithm algoritmo a usar (mergesort, quicksort)
     */
    public void sortDogs(List<Dog> dogs, String criteria, String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            algorithm = "mergesort";
        }

        if ("quicksort".equalsIgnoreCase(algorithm)) {
            quickSortDogs(dogs, criteria, 0, dogs.size() - 1);
        } else {
            // MergeSort (TimSort)
            Comparator<Dog> comparator = getComparator(criteria);
            dogs.sort(comparator);
        }
    }

    /**
     * Versión legacy que usa MergeSort por defecto
     */
    public void sortDogs(List<Dog> dogs, String criteria) {
        sortDogs(dogs, criteria, "mergesort");
    }

    /**
     * Implementación de QuickSort para perros
     * Algoritmo divide y vencerás que particiona la lista recursivamente
     */
    private void quickSortDogs(List<Dog> dogs, String criteria, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(dogs, criteria, low, high);
            quickSortDogs(dogs, criteria, low, pivotIndex - 1);
            quickSortDogs(dogs, criteria, pivotIndex + 1, high);
        }
    }

    /**
     * Particiona la lista usando el último elemento como pivote
     */
    private int partition(List<Dog> dogs, String criteria, int low, int high) {
        Dog pivot = dogs.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (compare(dogs.get(j), pivot, criteria) <= 0) {
                i++;
                swap(dogs, i, j);
            }
        }

        swap(dogs, i + 1, high);
        return i + 1;
    }

    /**
     * Compara dos perros según el criterio especificado
     * @return negativo si a < b, 0 si a == b, positivo si a > b
     */
    private int compare(Dog a, Dog b, String criteria) {
        switch (criteria.toLowerCase()) {
            case "priority":
                // Orden ascendente (menor prioridad primero)
                return Integer.compare(a.getPriority(), b.getPriority());
            case "age":
                return Integer.compare(a.getAge(), b.getAge());
            case "weight":
                return Integer.compare(a.getWeight(), b.getWeight());
            default:
                throw new IllegalArgumentException("Criterio de orden no válido: " + criteria);
        }
    }

    /**
     * Intercambia dos elementos en la lista
     */
    private void swap(List<Dog> dogs, int i, int j) {
        Dog temp = dogs.get(i);
        dogs.set(i, dogs.get(j));
        dogs.set(j, temp);
    }

    /**
     * Obtiene el comparador para un criterio dado
     */
    private Comparator<Dog> getComparator(String criteria) {
        switch (criteria.toLowerCase()) {
            case "priority":
                // Orden ascendente (menor prioridad primero)
                return Comparator.comparingInt(Dog::getPriority);
            case "age":
                return Comparator.comparingInt(Dog::getAge);
            case "weight":
                return Comparator.comparingDouble(Dog::getWeight);
            default:
                throw new IllegalArgumentException("Criterio de orden no válido: " + criteria);
        }
    }
}



