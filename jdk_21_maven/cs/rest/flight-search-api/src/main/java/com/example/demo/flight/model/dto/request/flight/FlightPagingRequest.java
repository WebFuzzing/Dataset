package com.example.demo.flight.model.dto.request.flight;

import com.example.demo.common.model.dto.request.CustomPagingRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Request class used for paginated flight retrieval.
 * Inherits from the base class {@link CustomPagingRequest}, which provides pagination details such as page number and size.
 * This class is used to request a paginated list of flights.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FlightPagingRequest extends CustomPagingRequest {

}
