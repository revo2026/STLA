package com.stla.patterns.strategy;

import java.math.BigDecimal;

public class PayPalPaymentStrategy implements PaymentStrategy {
    private String paypalEmail;
    public PayPalPaymentStrategy(String paypalEmail) { this.paypalEmail = paypalEmail; }
    @Override public String getMethodName() { return "PayPal"; }
    @Override public boolean processPayment(BigDecimal amount, String studentId, String courseId) {
        System.out.println("[PayPal] Processing $" + amount); return true;
    }
    @Override public boolean validatePaymentDetails() { return paypalEmail != null && paypalEmail.contains("@"); }
}
