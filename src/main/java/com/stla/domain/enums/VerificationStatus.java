package com.stla.domain.enums;

/**
 * Verification status for instructor accounts.
 */
public enum VerificationStatus {
    PENDING("PENDING"),
    VERIFIED("VERIFIED"),
    REJECTED("REJECTED");

    private final String value;

    VerificationStatus(String value) { this.value = value; }

    public String getValue() { return value; }

    public static VerificationStatus fromValue(String v) {
        if (v == null || v.isBlank()) return PENDING;
        for (VerificationStatus s : values()) {
            if (s.value.equalsIgnoreCase(v)) return s;
        }
        return PENDING;
    }
}
