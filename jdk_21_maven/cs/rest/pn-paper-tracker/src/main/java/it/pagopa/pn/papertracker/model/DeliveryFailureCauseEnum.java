package it.pagopa.pn.papertracker.model;

import lombok.Getter;

import java.util.Optional;
import java.util.stream.Stream;

@Getter
public enum DeliveryFailureCauseEnum {

    M01,
    M02,
    M03,
    M04,
    M05,
    M06,
    M07,
    M08,
    M09,
    M10,
    F01,
    F02,
    F03,
    F04,
    C01,
    C02,
    C03,
    C04,
    C05,
    C06,
    UNKNOWN,
    CHECK_IF_REQUIRED;

    public static DeliveryFailureCauseEnum fromValue(String deliveryFailureCause) {
        return Stream.of(values())
                .filter(value -> value.name().equalsIgnoreCase(Optional.ofNullable(deliveryFailureCause)
                                .map(String::toUpperCase).orElse(null)))
                .findFirst()
                .orElse(UNKNOWN);
    }
}