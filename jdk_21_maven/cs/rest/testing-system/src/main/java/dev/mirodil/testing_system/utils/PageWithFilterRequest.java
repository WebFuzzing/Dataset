package dev.mirodil.testing_system.utils;

import dev.mirodil.testing_system.configs.PageableWithFilterResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@link PageRequest} to support dynamic filtering.
 * <p>
 * Filters are stored as a List of {@link FilterCriteria}
 * <p>
 * {@code List<FilterCriteria<>(key, List<operators>, List<values>)>}
 * <p>
 * Example query: {@code ?scorePercentage>=70&scorePercentage<=90&isPassed=true,false}
 * <pre>
 * [
 *   new FilterCriteria<>("scorePercentage", List.of(">=", "<="), List.of(70, 90)), // Integer values
 *   new FilterCriteria<>("isPassed", List.of("="), List.of(true, false)) // Boolean values
 * ]
 * </pre>
 * Internally, {@link PageableWithFilterResolver} converts them to the correct types.
 *
 * @see PageableWithFilterResolver
 * @see PageRequest
 */
@Schema(description = "Custom pageable request with filters")
public class PageWithFilterRequest extends PageRequest {
    /**
     * Filter criteria in the form of {@code List<FilterCriteria<>(key, List<operators>, List<values>)>}.
     */
    @Schema(description = "Map of filter criteria. Add as query parameters (e.g., ?scorePercentage>=70&scorePercentage<=90&isPassed=true,false).")
    private final List<FilterCriteria<?>> filters;

    public PageWithFilterRequest(int pageNumber, int pageSize, Sort sort, List<FilterCriteria<?>> filters) {
        super(pageNumber, pageSize, sort);
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    public List<FilterCriteria<?>> getFilters() {
        return filters;
    }
}
