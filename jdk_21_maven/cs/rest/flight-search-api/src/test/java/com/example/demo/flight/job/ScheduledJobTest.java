package com.example.demo.flight.job;

import com.example.demo.base.AbstractBaseServiceTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link ScheduledJob}.
 * Verifies that {@link ScheduledJob#loadMockFlightsToDatabase()} triggers
 * {@link FlightDataLoader#loadFlightDumpyData()} once.
 * Uses Awaitility to wait up to 5 seconds for the method call.
 */
class ScheduledJobTest extends AbstractBaseServiceTest {

    @InjectMocks
    private ScheduledJob scheduledJob;

    @Mock
    private FlightDataLoader flightDataLoader;

    @Test
    void testLoadMockFlightsToDatabase() {
        // Arrange: Do nothing when flight data loader is called
        doNothing().when(flightDataLoader).loadFlightDumpyData();

        // Act: Manually trigger the scheduled job
        scheduledJob.loadMockFlightsToDatabase();

        // Assert: Wait and verify that the method was called
        // Waits up to 5 seconds, checking condition periodically
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(flightDataLoader, times(1)).loadFlightDumpyData()
                );

    }

}