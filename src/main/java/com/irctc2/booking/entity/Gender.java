package com.irctc2.booking.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    MALE("M"),
    FEMALE("F"),
    OTHER("O");

    private final String code;

    Gender(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Gender fromCode(String code) {
        for (Gender gender : Gender.values()) {
            if (gender.code.equalsIgnoreCase(code)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Invalid gender code: " + code);
    }
}
