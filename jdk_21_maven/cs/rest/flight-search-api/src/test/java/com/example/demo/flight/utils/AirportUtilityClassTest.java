package com.example.demo.flight.utils;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.flight.exception.AirportNameAlreadyExistException;
import com.example.demo.flight.repository.AirportRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test class for {@link AirportUtilityClass}.
 * This class tests the utility methods of {@link AirportUtilityClass}, ensuring
 * they function as expected. It also verifies that the utility class cannot be
 * instantiated, as it is intended to be a utility class.
 */
class AirportUtilityClassTest extends AbstractBaseServiceTest {

    @Mock
    private AirportRepository airportRepository;

    @Test
    void utilityClass_ShouldNotBeInstantiated() {
        assertThrows(InvocationTargetException.class, () -> {
            // Attempt to use reflection to create an instance of the utility class
            java.lang.reflect.Constructor<AirportUtilityClass> constructor = AirportUtilityClass.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }

    @Test
    void checkAirportNameUniqueness_ShouldThrowException_WhenNameExists() {

        // Given
        String existingAirportName = "Existing Airport";

        // When
        when(airportRepository.existsByName(existingAirportName)).thenReturn(true);

        // Then
        assertThrows(AirportNameAlreadyExistException.class, () ->
                AirportUtilityClass.checkAirportNameUniqueness(airportRepository, existingAirportName)
        );

        // Verify
        verify(airportRepository).existsByName(existingAirportName);

    }

    @Test
    void checkAirportNameUniqueness_ShouldNotThrowException_WhenNameDoesNotExist() {

        // Given
        String newAirportName = "New Airport";

        // When
        when(airportRepository.existsByName(newAirportName)).thenReturn(false);

        // Then
        assertDoesNotThrow(() ->
                AirportUtilityClass.checkAirportNameUniqueness(airportRepository, newAirportName)
        );

        // Verify
        verify(airportRepository).existsByName(newAirportName);

    }

}