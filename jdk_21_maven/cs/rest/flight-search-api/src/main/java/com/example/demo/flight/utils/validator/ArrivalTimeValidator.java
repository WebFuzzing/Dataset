package com.example.demo.flight.utils.validator;

import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;
import com.example.demo.flight.model.dto.request.flight.SearchFlightRequest;
import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;
import com.example.demo.flight.utils.annotations.ValidArrivalTime;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Validates that the arrival time is not earlier than the departure time in {@link Object}.
 */
@Component
public class ArrivalTimeValidator implements ConstraintValidator<ValidArrivalTime, Object> {

    /**
     * Validates that the arrival time is not earlier than the departure time.
     * This method works for both CreateFlightRequest and UpdateFlightRequest.
     *
     * @param value The object being validated. It can either be a CreateFlightRequest or UpdateFlightRequest.
     * @param context The validation context.
     * @return {@code true} if the arrival time is not earlier than the departure time, {@code false} otherwise.
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;  // Return true if the value is null (skip validation)
        }

        // Validate for CreateFlightRequest type
        if (value instanceof CreateFlightRequest createFlightRequest) {
            return isValidArrivalTime(createFlightRequest.getArrivalTime(), createFlightRequest.getDepartureTime());
        }
        // Validate for UpdateFlightRequest type
        else if (value instanceof UpdateFlightRequest updateFlightRequest) {
            return isValidArrivalTime(updateFlightRequest.getArrivalTime(), updateFlightRequest.getDepartureTime());
        }
        // Validate for SearchFlightRequest type
        else if (value instanceof SearchFlightRequest searchFlightRequest) {
            return isValidArrivalTime(searchFlightRequest.getArrivalTime(), searchFlightRequest.getDepartureTime());
        }

        return true;  // Return true if the object type is not recognized
    }

    /**
     * Checks if the arrival time is after the departure time.
     *
     * @param arrivalTime The arrival time to validate.
     * @param departureTime The departure time to compare against.
     * @return {@code true} if arrival time is after departure time, {@code false} otherwise.
     */
    private boolean isValidArrivalTime(LocalDateTime arrivalTime, LocalDateTime departureTime) {
        if (arrivalTime != null && departureTime != null) {
            return !arrivalTime.isBefore(departureTime);  // Check if arrival time is not before departure time
        }
        return true;  // If either time is null, the validation passes
    }

}
