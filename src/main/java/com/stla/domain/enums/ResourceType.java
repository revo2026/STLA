package com.stla.domain.enums;

public enum ResourceType {
    PDF("pdf"), DOC("doc"), ZIP("zip"), LINK("link"), IMAGE("image"), OTHER("other");
    private final String value;
    ResourceType(String value) { this.value = value; }
    public String getValue() { return value; }
    public static ResourceType fromValue(String v) {
        for (ResourceType t : values()) if (t.value.equalsIgnoreCase(v)) return t;
        throw new IllegalArgumentException("Unknown resource type: " + v);
    }
}
