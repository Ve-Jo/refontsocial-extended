package ru.rizonchik.refontsocial.model;

import java.util.Locale;

public enum Gender {
    MALE("male"),
    FEMALE("female"),
    NONBINARY("nonbinary"),
    OTHER("other"),
    UNDISCLOSED("undisclosed");

    private final String key;

    Gender(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public static Gender fromInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return UNDISCLOSED;
        }
        String normalized = input.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").trim();
        switch (normalized) {
            case "m":
            case "male":
            case "man":
            case "boy":
                return MALE;
            case "f":
            case "female":
            case "woman":
            case "girl":
                return FEMALE;
            case "nb":
            case "nonbinary":
            case "nonbinaryperson":
                return NONBINARY;
            case "other":
            case "custom":
                return OTHER;
            case "none":
            case "undisclosed":
            case "unknown":
            case "unset":
                return UNDISCLOSED;
            default:
                return null;
        }
    }
}
