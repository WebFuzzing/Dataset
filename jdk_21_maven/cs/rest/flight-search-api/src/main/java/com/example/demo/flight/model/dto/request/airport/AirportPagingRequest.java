package com.example.demo.flight.model.dto.request.airport;

import com.example.demo.common.model.dto.request.CustomPagingRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Request class used for paginated airport retrieval.
 * Inherits from the base class {@link CustomPagingRequest}, which provides pagination details such as page number and size.
 * This class is used to request a paginated list of airports.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AirportPagingRequest extends CustomPagingRequest {

}
