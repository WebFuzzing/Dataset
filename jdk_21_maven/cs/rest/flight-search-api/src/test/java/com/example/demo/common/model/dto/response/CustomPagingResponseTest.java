package com.example.demo.common.model.dto.response;

import com.example.demo.common.model.CustomPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test class for {@link CustomPagingResponse}.
 * This class tests the functionality of the {@link CustomPagingResponse}, focusing on the behavior
 * of the static {@code of} method. The tests ensure that the method correctly creates an instance
 * of {@link CustomPagingResponse} with the expected values based on the provided input data.
 */
class CustomPagingResponseTest {

    @Test
    void testOfMethod() {

        // Given
        CustomPage<String> customPage = new CustomPage<>();
        customPage.setPageNumber(1);
        customPage.setPageSize(10);
        customPage.setTotalElementCount(100L);
        customPage.setTotalPageCount(10);

        // Act: use the builder's 'of' method to create a CustomPagingResponse
        CustomPagingResponse<String> response = CustomPagingResponse.<String>builder()
                .of(customPage)
                .build();

        // Assert: verify that the fields from CustomPage are correctly mapped to the response
        assertEquals(1, response.getPageNumber());
        assertEquals(10, response.getPageSize());
        assertEquals(100L, response.getTotalElementCount());
        assertEquals(10, response.getTotalPageCount());

    }

}