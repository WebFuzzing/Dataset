package com.example.demo.common.model;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A class representing a paginated response for a list of items.
 * This class is used to wrap a list of items along with pagination details such as the page number,
 * page size, total number of elements, and total number of pages.
 * This is typically used in APIs to return large sets of data in chunks, while providing metadata
 * about the pagination (e.g., for implementing pagination in front-end applications).
 *
 * @param <T> The type of items contained in the page (e.g., `User`, `Product`).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomPage<T> {
    private List<T> content;

    private Integer pageNumber;

    private Integer pageSize;

    private Long totalElementCount;

    private Integer totalPageCount;

    /**
     * A utility method to create a `CustomPage` instance from a list of domain models and a Spring Data `Page` object.
     * <p>
     * This method extracts relevant pagination information from the provided `Page` object and combines it
     * with the list of domain models to construct a `CustomPage`.
     *
     * @param <C> The type of the content in the new `CustomPage`.
     * @param <X> The type of the content in the provided `Page` (usually the same as `C`, but could differ).
     * @param domainModels A list of domain models to include in the page's content.
     * @param page The Spring Data `Page` object containing pagination metadata.
     * @return A new `CustomPage` instance containing the domain models and pagination details.
     */
    public static <C, X> CustomPage<C> of(final List<C> domainModels, final Page<X> page) {
        return CustomPage.<C>builder()
                .content(domainModels)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPageCount(page.getTotalPages())
                .totalElementCount(page.getTotalElements())
                .build();
    }

}
