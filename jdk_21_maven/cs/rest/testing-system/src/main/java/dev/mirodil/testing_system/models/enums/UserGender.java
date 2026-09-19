package dev.mirodil.testing_system.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = UserGenderDeserializer.class)
public enum UserGender {
    MALE("male"),
    FEMALE("female");

    private final String value;

    UserGender(String value) {
        this.value = value;
    }

    @JsonCreator
    public static UserGender fromValue(String value) {
        for (UserGender gender : UserGender.values()) {
            if (gender.value.equalsIgnoreCase(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Invalid gender value: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
