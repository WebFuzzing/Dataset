package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.Getter;

@Getter
public enum ProductType {

    RS("RS"),

    AR("AR"),

    _890("890"),

    RIR("RIR"),

    RIS("RIS"),

    ALL("ALL"),

    UNKNOWN("UNKNOWN");

    private final String value;

    ProductType(String value) {
        this.value = value;
    }

    public static ProductType fromValue(String value) {
        for (ProductType type : ProductType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ProductType: " + value);
    }

}
