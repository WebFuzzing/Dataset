package com.example.demo.flight.utils.annotations;

import com.example.demo.flight.utils.validator.ArrivalTimeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for validating arrival time.
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ArrivalTimeValidator.class)
public @interface ValidArrivalTime {
    String message() default "Arrival time must be the same as or later than departure time!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

