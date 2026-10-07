package com.example.demo.flight.utils.validator;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;
import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test class for {@link ArrivalTimeValidator}.
 * This test class is responsible for testing the validation logic of the {@link ArrivalTimeValidator}.
 * It extends {@link AbstractBaseServiceTest} and uses {@link Mock} annotations to mock the necessary dependencies.
 */
class ArrivalTimeValidatorTest extends AbstractBaseServiceTest {

    private ArrivalTimeValidator arrivalTimeValidator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        arrivalTimeValidator = new ArrivalTimeValidator();
    }

    @Test
    void testValidArrivalTimeAfterDepartureTime() {
        // Given
        CreateFlightRequest validRequest = new CreateFlightRequest();
        validRequest.setDepartureTime(LocalDateTime.of(2025, 1, 13, 10, 0));
        validRequest.setArrivalTime(LocalDateTime.of(2025, 1, 13, 12, 0));

        // When
        boolean isValid = arrivalTimeValidator.isValid(validRequest, context);

        // Then
        assertTrue(isValid, "Arrival time should be after departure time.");
    }

    @Test
    void testInvalidArrivalTimeBeforeDepartureTime() {
        // Given
        CreateFlightRequest invalidRequest = new CreateFlightRequest();
        invalidRequest.setDepartureTime(LocalDateTime.of(2025, 1, 13, 10, 0));
        invalidRequest.setArrivalTime(LocalDateTime.of(2025, 1, 13, 8, 0));

        // When
        boolean isValid = arrivalTimeValidator.isValid(invalidRequest, context);

        // Then
        assertFalse(isValid, "Arrival time cannot be earlier than departure time.");
    }

    @Test
    void testNullArrivalTime() {
        // Given
        CreateFlightRequest requestWithNullArrival = new CreateFlightRequest();
        requestWithNullArrival.setDepartureTime(LocalDateTime.of(2025, 1, 13, 10, 0));
        requestWithNullArrival.setArrivalTime(null);

        // When
        boolean isValid = arrivalTimeValidator.isValid(requestWithNullArrival, context);

        // Then
        assertTrue(isValid, "Arrival time should be valid when it is null.");
    }

    @Test
    void testNullDepartureTime() {
        // Given
        CreateFlightRequest requestWithNullDeparture = new CreateFlightRequest();
        requestWithNullDeparture.setDepartureTime(null);
        requestWithNullDeparture.setArrivalTime(LocalDateTime.of(2025, 1, 13, 12, 0));

        // When
        boolean isValid = arrivalTimeValidator.isValid(requestWithNullDeparture, context);

        // Then
        assertTrue(isValid, "Arrival time should be valid when departure time is null.");
    }

    @Test
    void testBothTimesNull() {
        // Given
        CreateFlightRequest requestWithBothNull = new CreateFlightRequest();
        requestWithBothNull.setDepartureTime(null);
        requestWithBothNull.setArrivalTime(null);

        // When
        boolean isValid = arrivalTimeValidator.isValid(requestWithBothNull, context);

        // Then
        assertTrue(isValid, "Both arrival and departure times should be valid when null.");
    }

    @Test
    void testArrivalTimeEqualToDepartureTime() {
        // Given
        CreateFlightRequest requestWithEqualTimes = new CreateFlightRequest();
        LocalDateTime sameTime = LocalDateTime.of(2025, 1, 13, 10, 0);
        requestWithEqualTimes.setDepartureTime(sameTime);
        requestWithEqualTimes.setArrivalTime(sameTime);

        // When
        boolean isValid = arrivalTimeValidator.isValid(requestWithEqualTimes, context);

        // Then
        assertTrue(isValid, "Arrival time should be valid if it is equal to departure time.");
    }

    @Test
    void testValidUpdateArrivalTimeAfterDepartureTime() {
        // Given
        UpdateFlightRequest validRequest = new UpdateFlightRequest();
        validRequest.setDepartureTime(LocalDateTime.of(2025, 1, 13, 10, 0));
        validRequest.setArrivalTime(LocalDateTime.of(2025, 1, 13, 12, 0));

        // When
        boolean isValid = arrivalTimeValidator.isValid(validRequest, context);

        // Then
        assertTrue(isValid, "Arrival time should be after departure time.");
    }

    @Test
    void testInvalidUpdateArrivalTimeBeforeDepartureTime() {
        // Given
        UpdateFlightRequest invalidRequest = new UpdateFlightRequest();
        invalidRequest.setDepartureTime(LocalDateTime.of(2025, 1, 13, 10, 0));
        invalidRequest.setArrivalTime(LocalDateTime.of(2025, 1, 13, 8, 0));

        // When
        boolean isValid = arrivalTimeValidator.isValid(invalidRequest, context);

        // Then
        assertFalse(isValid, "Arrival time cannot be earlier than departure time.");
    }

    @Test
    void testNullUpdateArrivalTime() {
        // Given
        UpdateFlightRequest requestWithNullArrival = new UpdateFlightRequest();
        requestWithNullArrival.setDepartureTime(LocalDateTime.of(2025, 1, 13, 10, 0));
        requestWithNullArrival.setArrivalTime(null);

        // When
        boolean isValid = arrivalTimeValidator.isValid(requestWithNullArrival, context);

        // Then
        assertTrue(isValid, "Arrival time should be valid when it is null.");
    }

    @Test
    void testNullUpdateDepartureTime() {
        // Given
        UpdateFlightRequest requestWithNullDeparture = new UpdateFlightRequest();
        requestWithNullDeparture.setDepartureTime(null);
        requestWithNullDeparture.setArrivalTime(LocalDateTime.of(2025, 1, 13, 12, 0));

        // When
        boolean isValid = arrivalTimeValidator.isValid(requestWithNullDeparture, context);

        // Then
        assertTrue(isValid, "Arrival time should be valid when departure time is null.");
    }

    @Test
    void testBothUpdateTimesNull() {
        // Given
        UpdateFlightRequest requestWithBothNull = new UpdateFlightRequest();
        requestWithBothNull.setDepartureTime(null);
        requestWithBothNull.setArrivalTime(null);

        // When
        boolean isValid = arrivalTimeValidator.isValid(requestWithBothNull, context);

        // Then
        assertTrue(isValid, "Both arrival and departure times should be valid when null.");
    }

    @Test
    void testUpdateArrivalTimeEqualToDepartureTime() {
        // Given
        UpdateFlightRequest requestWithEqualTimes = new UpdateFlightRequest();
        LocalDateTime sameTime = LocalDateTime.of(2025, 1, 13, 10, 0);
        requestWithEqualTimes.setDepartureTime(sameTime);
        requestWithEqualTimes.setArrivalTime(sameTime);

        // When
        boolean isValid = arrivalTimeValidator.isValid(requestWithEqualTimes, context);

        // Then
        assertTrue(isValid, "Arrival time should be valid if it is equal to departure time.");
    }

    @Test
    void testNullValue() {

        // Given
        ArrivalTimeValidator arrivalTimeValidator = new ArrivalTimeValidator();

        // When
        boolean isValid = arrivalTimeValidator.isValid(null, context);

        // Then
        assertTrue(isValid, "Validation should pass when the value is null.");

    }

    @Test
    void testUnrecognizedObjectType() {

        // Given
        ArrivalTimeValidator arrivalTimeValidator = new ArrivalTimeValidator();
        Object unrecognizedObject = new Object(); // Some other object type not recognized by the validator

        // When
        boolean isValid = arrivalTimeValidator.isValid(unrecognizedObject, context);

        // Then
        assertTrue(isValid, "Validation should pass when the object type is not recognized.");

    }

}