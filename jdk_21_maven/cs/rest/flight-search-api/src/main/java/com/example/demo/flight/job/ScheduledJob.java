package com.example.demo.flight.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ScheduledJob is a scheduled task that runs at midnight every day
 * to load mock flight data into the database.
 */
@Component
@RequiredArgsConstructor
public class ScheduledJob {

    private final FlightDataLoader flightDataLoader;

    /**
     * Executes the flight data loading job at midnight (00:00:00) daily.
     * This method calls {@link FlightDataLoader#loadFlightDumpyData()}
     * to populate the database with mock flight data.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void loadMockFlightsToDatabase() {
        flightDataLoader.loadFlightDumpyData();
    }

}
