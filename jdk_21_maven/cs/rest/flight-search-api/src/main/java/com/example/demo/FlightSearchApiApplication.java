package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Flight Search Api application with Couchbase integration.
 * This class is responsible for launching the Spring Boot application, which initializes all necessary components,
 * including configurations for Couchbase, and starts the application context.
 * The {@link SpringApplication#run(Class, String...)} method is invoked to start the application.
 */
@SpringBootApplication
public class FlightSearchApiApplication {

	/**
	 * The main method which serves as the entry point to launch the Spring Boot application.
	 * This method initializes and runs the {@link SpringApplication} to start the application.
	 * It automatically configures the application context and any required components.
	 *
	 * @param args Command-line arguments passed to the application.
	 *             These are forwarded to the {@link SpringApplication#run(Class, String...)} method.
	 */
	public static void main(String[] args) {
		SpringApplication.run(FlightSearchApiApplication.class, args);
	}

}
