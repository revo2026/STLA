package com.stla.patterns.adapter;

/**
 * Simulated payment gateway adapter for demonstration.
 */
public class SimulatedPaymentGateway implements PaymentGatewayAdapter {
    @Override
    public boolean charge(String token, double amount, String currency) {
        System.out.println("[SimulatedGateway] Charging " + currency + " " + amount);
        return true;
    }

    @Override
    public boolean refund(String transactionId) {
        System.out.println("[SimulatedGateway] Refunding transaction " + transactionId);
        return true;
    }

    @Override public String getProviderName() { return "Simulated Gateway"; }
}
