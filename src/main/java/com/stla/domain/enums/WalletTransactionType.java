package com.stla.domain.enums;

public enum WalletTransactionType {
    EARNING("earning"),
    WITHDRAWAL("withdrawal"),
    ADJUSTMENT("adjustment"),
    COURSE_PURCHASE("course_purchase"),
    PLATFORM_COMMISSION("platform_commission"),
    INSTRUCTOR_REVENUE("instructor_revenue"),
    REFUND("refund");
    private final String value;
    WalletTransactionType(String value) { this.value = value; }
    public String getValue() { return value; }
    public static WalletTransactionType fromValue(String v) {
        for (WalletTransactionType t : values()) if (t.value.equalsIgnoreCase(v)) return t;
        throw new IllegalArgumentException("Unknown wallet transaction type: " + v);
    }
}
