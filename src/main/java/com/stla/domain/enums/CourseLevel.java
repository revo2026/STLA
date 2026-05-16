package com.stla.domain.enums;

public enum CourseLevel {
    BEGINNER("beginner"), INTERMEDIATE("intermediate"), ADVANCED("advanced");
    private final String value;
    CourseLevel(String value) { this.value = value; }
    public String getValue() { return value; }
    public static CourseLevel fromValue(String v) {
        for (CourseLevel l : values()) if (l.value.equalsIgnoreCase(v)) return l;
        throw new IllegalArgumentException("Unknown course level: " + v);
    }
}
