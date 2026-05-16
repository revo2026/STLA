package com.stla.patterns.adapter;

/**
 * Adapter Pattern: Unified interface for payment gateway interactions.
 */
public interface PaymentGatewayAdapter {
    boolean charge(String token, double amount, String currency);
    boolean refund(String transactionId);
    String getProviderName();
}
