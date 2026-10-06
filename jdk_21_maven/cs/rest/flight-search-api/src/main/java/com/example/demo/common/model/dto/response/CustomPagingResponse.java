package com.example.demo.common.model.dto.response;

import com.example.demo.common.model.CustomPage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents a custom paging response that holds paginated data along with
 * pagination details such as the current page number, page size, total element count,
 * and total page count.
 * This class is used to encapsulate the response for a paginated query.
 *
 * @param <T> The type of the elements in the paginated response.
 */
@Getter
@Builder
public class CustomPagingResponse<T> {

    private List<T> content;

    private Integer pageNumber;

    private Integer pageSize;

    private Long totalElementCount;

    private Integer totalPageCount;

    /**
     * Builder class for {@link CustomPagingResponse}. It provides a custom builder method
     * to create a {@link CustomPagingResponse} from a {@link CustomPage}.
     *
     * @param <T> The type of the elements in the paginated response.
     */
    public static class CustomPagingResponseBuilder<T> {

        public <C> CustomPagingResponseBuilder<T> of(final CustomPage<C> customPage) {
            return CustomPagingResponse.<T>builder()
                    .pageNumber(customPage.getPageNumber())
                    .pageSize(customPage.getPageSize())
                    .totalElementCount(customPage.getTotalElementCount())
                    .totalPageCount(customPage.getTotalPageCount());
        }

    }

}
