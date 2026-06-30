package com.aes.exam.auth;

public enum UserStatus {
    ACTIVE("active"),
    DISABLED("disabled");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static UserStatus fromValue(String value) {
        for (UserStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status: " + value);
    }
}
