package com.stla.domain.enums;

public enum EnrollmentStatus {
    ACTIVE("active"), CANCELLED("cancelled"), COMPLETED("completed");
    private final String value;
    EnrollmentStatus(String value) { this.value = value; }
    public String getValue() { return value; }
    public static EnrollmentStatus fromValue(String v) {
        for (EnrollmentStatus s : values()) if (s.value.equalsIgnoreCase(v)) return s;
        throw new IllegalArgumentException("Unknown enrollment status: " + v);
    }
}
