package com.arcane.dam;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.arcane.dam")
public class ArcaneDamServerApplication {

	@Value("${aws.sdk.print-location:false}")
	private boolean printLocation;

	@Value("${aws.sdk.disable-deprecation:false}")
	private boolean disableDeprecation;

	public static void main(String[] args) {
		SpringApplication.run(ArcaneDamServerApplication.class, args);
	}

	//Disable Deprecation
	@PostConstruct
	public void init() {
		if (printLocation) {
			System.setProperty("aws.java.v1.printLocation", "true");
		}
		if (disableDeprecation) {
			System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
		}
	}

}
