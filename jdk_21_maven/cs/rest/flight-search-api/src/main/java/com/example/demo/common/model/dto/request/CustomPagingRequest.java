package com.example.demo.common.model.dto.request;

import com.example.demo.common.model.CustomPaging;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Abstract class that represents a custom paging request.
 * This class provides pagination details through a `CustomPaging` object
 * and includes a method to convert the pagination information into a `Pageable` object.
 * It is intended to be extended by other classes that require paging functionality.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class CustomPagingRequest {

    private CustomPaging pagination;

    /**
     * Converts the `CustomPaging` object into a `Pageable` object.
     * The `Pageable` object is used by Spring Data to handle pagination in queries.
     *
     * @return A `Pageable` object containing the page number and page size derived from the `pagination` field.
     */
    public Pageable toPageable() {
        return PageRequest.of(
                Math.toIntExact(pagination.getPageNumber()),
                Math.toIntExact(pagination.getPageSize())
        );
    }

}
