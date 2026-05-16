package com.stla.domain.enums;

/**
 * User roles matching PostgreSQL enum: public.app_role
 */
public enum AppRole {
    STUDENT("student"),
    INSTRUCTOR("instructor"),
    ADMIN("admin");

    private final String value;

    AppRole(String value) { this.value = value; }

    public String getValue() { return value; }

    public static AppRole fromValue(String value) {
        for (AppRole role : values()) {
            if (role.value.equalsIgnoreCase(value)) return role;
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
