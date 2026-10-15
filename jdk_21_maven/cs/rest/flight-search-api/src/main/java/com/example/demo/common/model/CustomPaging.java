package com.example.demo.common.model;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A class representing the pagination information for a request.
 * This class holds the page number and page size, which are used to control the data returned in paginated responses.
 * It is typically used in APIs to handle pagination for large datasets, where only a subset of data is returned at a time.
 * This class ensures that the page number and page size provided by the user meet the minimum constraints.
 * The page number is expected to be 1-based, but internally it is converted to 0-based indexing.
 *
 * @see #getPageNumber() for how the page number is adjusted.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CustomPaging {

    @Min(value = 1, message = "Page number must be bigger than 0")
    private Integer pageNumber;

    @Min(value = 1, message = "Page size must be bigger than 0")
    private Integer pageSize;

    /**
     * Returns the 0-based page number for internal use in pagination.
     * This method adjusts the page number by subtracting 1 from the provided 1-based index.
     * For example, if the user provides page 1, this method will return 0 to match Spring Data's 0-based page indexing.
     *
     * @return The adjusted 0-based page number.
     */
    public Integer getPageNumber() {
        return pageNumber - 1;
    }

}
