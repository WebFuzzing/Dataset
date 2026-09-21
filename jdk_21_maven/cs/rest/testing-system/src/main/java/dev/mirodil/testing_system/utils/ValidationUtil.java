package dev.mirodil.testing_system.utils;

import dev.mirodil.testing_system.exceptions.GenericValidationError;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class ValidationUtil {
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Validates the sorting parameters of a {@link Pageable} object to ensure that only allowed attributes
     * and valid sort directions (ascending or descending) are used. This method helps prevent SQL injection
     * attacks by restricting sorting attributes to a predefined set of allowed values.
     * <p>
     * Malicious input example:
     * {@code ?size=7&sort=id,desc&sort=fname;DROP TABLE users; --}
     *
     * @param pageable              the {@link Pageable} object to validate.
     * @param allowedSortAttributes a {@link Set} of attribute names that are permitted for sorting.
     * @throws GenericValidationError if an invalid sort attribute or direction is detected.
     */
    public static void forceValidPageable(Pageable pageable, Set<String> allowedSortAttributes) throws GenericValidationError {
        forceValidPage(pageable.getPageNumber(), pageable.getPageSize());
        forceValidSort(pageable.getSort(), allowedSortAttributes);
    }

    public static void forceValidPage(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize < 0) {
            throw new GenericValidationError("Page number or page size cannot be negative");
        }

        if (pageSize > MAX_PAGE_SIZE) {
            throw new GenericValidationError("Page size cannot be more than " + MAX_PAGE_SIZE);
        }
    }

    public static void forceValidSort(Sort sort, Set<String> allowedSortAttributes) {
        if (sort.isEmpty()) {
            return;
        }

        for (Sort.Order order : sort.stream().toList()) {
            if (!allowedSortAttributes.contains(order.getProperty())) {
                throw new GenericValidationError("Not allowed sort attribute: '" + order.getProperty() + "'. Allowed attributes: " + allowedSortAttributes);
            }

            if (!order.isAscending() && !order.isDescending()) {
                throw new GenericValidationError("Sort direction must be either ASC or DESC");
            }
        }
    }

    /**
     * Validates raw filter parameters with {@code Map<attribute, Map<operator, value>>} representation to ensure
     * that only allowed filter attributes are used.
     *
     * @param rawFilters              the {@code Map<attribute, Map<operator, value>>} object to validate. Example object:<p>
     *                                {@code Map.of("score", Map.of(">=", "70")) // ?score>=70}
     * @param allowedFilterAttributes a {@link Map} of permitted attributes and their value types for filtering. {@code Map.of("isPassed", Boolean.class)}.
     * @throws GenericValidationError if an invalid filter attribute, operator, or value is detected.
     */
    public static void forceValidRawFilters(Map<String, Map<String, String>> rawFilters, Map<String, Class<?>> allowedFilterAttributes) {
        if (rawFilters.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<String, String>> filter : rawFilters.entrySet()) {
            String attribute = filter.getKey();
            Map.Entry<String, String> operatorValueEntry = filter.getValue().entrySet().iterator().next();
            String operator = operatorValueEntry.getKey();
            String value = operatorValueEntry.getValue();

            if (!allowedFilterAttributes.containsKey(attribute)) {
                throw new GenericValidationError("Not allowed filter attribute: '" + attribute + "'. Allowed attributes: " + allowedFilterAttributes.keySet());
            }

            Class<?> type = allowedFilterAttributes.get(attribute);
            Set<String> validOperators = getAllowedOperatorsForType(type);
            if (!validOperators.contains(operator)) {
                throw new GenericValidationError("Invalid operator '" + operator + "' for filter '" + attribute + "'. " +
                        "Allowed operators: " + validOperators);
            }

            if (value.isBlank()) {
                throw new GenericValidationError("Value for '" + attribute + "' cannot be empty.");
            }

            if (value.contains(",") && !operator.equals("=")) {
                throw new GenericValidationError("Multivalued filter with the same attribute is only supported with equality operator (=). You should declare " + attribute + " separately (e.g. scorePercentage>=70&scorePercentage<90).");
            }
        }
    }

    /**
     * Determines the allowed operators for a given data type.
     */
    private static Set<String> getAllowedOperatorsForType(Class<?> type) {
        final Set<String> ALL_OPERATORS = Set.of("=", "<", ">", "<=", ">=");

        if (Number.class.isAssignableFrom(type) || type.equals(Instant.class)) {
            return ALL_OPERATORS;
        } else if (type.equals(String.class) || type.equals(Boolean.class)) {
            return Set.of("=");
        } else {
            throw new RuntimeException("Unsupported filter type: " + type.getSimpleName());
        }
    }
}
