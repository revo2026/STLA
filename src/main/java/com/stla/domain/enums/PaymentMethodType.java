package com.stla.domain.enums;

public enum PaymentMethodType {
    VISA("visa"), DIGITAL_WALLET("digital_wallet"), PAYPAL("paypal");
    private final String value;
    PaymentMethodType(String value) { this.value = value; }
    public String getValue() { return value; }
    public static PaymentMethodType fromValue(String v) {
        for (PaymentMethodType t : values()) if (t.value.equalsIgnoreCase(v)) return t;
        throw new IllegalArgumentException("Unknown payment method type: " + v);
    }
}
