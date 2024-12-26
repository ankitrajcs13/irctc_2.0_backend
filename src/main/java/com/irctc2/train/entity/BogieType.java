package com.irctc2.train.entity;

public enum BogieType {
    SLEEPER("S"),
    THIRD_AC("B"),
    SECOND_AC("A"),
    FIRST_AC("H"),
    GENERAL("G");

    private final String prefix;

    BogieType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public static BogieType fromString(String type) {
        for (BogieType bogieType : values()) {
            if (bogieType.name().equalsIgnoreCase(type.replace(" ", "_"))) {
                return bogieType;
            }
        }
        throw new IllegalArgumentException("Invalid bogie type: " + type);
    }
}
