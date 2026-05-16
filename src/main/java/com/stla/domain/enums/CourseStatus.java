package com.stla.domain.enums;

public enum CourseStatus {
    DRAFT("draft"), PENDING("pending"), APPROVED("approved"), REJECTED("rejected");
    private final String value;
    CourseStatus(String value) { this.value = value; }
    public String getValue() { return value; }
    public static CourseStatus fromValue(String v) {
        for (CourseStatus s : values()) if (s.value.equalsIgnoreCase(v)) return s;
        throw new IllegalArgumentException("Unknown course status: " + v);
    }
}
