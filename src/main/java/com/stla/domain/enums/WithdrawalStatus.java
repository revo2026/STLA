package com.stla.domain.enums;

public enum WithdrawalStatus {
    PENDING("pending"), APPROVED("approved"), REJECTED("rejected"), COMPLETED("completed");
    private final String value;
    WithdrawalStatus(String value) { this.value = value; }
    public String getValue() { return value; }
    public static WithdrawalStatus fromValue(String v) {
        for (WithdrawalStatus s : values()) if (s.value.equalsIgnoreCase(v)) return s;
        throw new IllegalArgumentException("Unknown withdrawal status: " + v);
    }
}
