package com.stla.domain.enums;

public enum NotificationType {
    ENROLLMENT("enrollment"), PAYMENT("payment"), COURSE_UPDATE("course_update"),
    NEW_LESSON("new_lesson"), WITHDRAWAL("withdrawal"), ADMIN_ALERT("admin_alert"),
    GENERAL("general");
    private final String value;
    NotificationType(String value) { this.value = value; }
    public String getValue() { return value; }
    public static NotificationType fromValue(String v) {
        for (NotificationType t : values()) if (t.value.equalsIgnoreCase(v)) return t;
        throw new IllegalArgumentException("Unknown notification type: " + v);
    }
}
