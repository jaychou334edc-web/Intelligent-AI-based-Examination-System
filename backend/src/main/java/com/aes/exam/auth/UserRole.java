package com.aes.exam.auth;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    ADMIN("admin"),
    TEACHER("teacher"),
    STUDENT("student");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static UserRole fromValue(String value) {
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role: " + value);
    }
}
