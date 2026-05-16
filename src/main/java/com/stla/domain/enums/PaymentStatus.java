package com.stla.domain.enums;

public enum PaymentStatus {
    PENDING("pending"), PAID("paid"), FAILED("failed"), REFUNDED("refunded");
    private final String value;
    PaymentStatus(String value) { this.value = value; }
    public String getValue() { return value; }
    public static PaymentStatus fromValue(String v) {
        for (PaymentStatus s : values()) if (s.value.equalsIgnoreCase(v)) return s;
        throw new IllegalArgumentException("Unknown payment status: " + v);
    }
}
