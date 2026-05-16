package com.stla.patterns.strategy;

public final class PaymentStrategyFactory {

    private PaymentStrategyFactory() {}

    public static PaymentStrategy visa(String cardholderName, String cardNumber, String expiryDate, String cvv) {
        return new VisaPaymentStrategy(cardholderName, cardNumber, expiryDate, cvv);
    }

    public static PaymentStrategy digitalWallet(String provider, String walletIdentifier) {
        return new WalletPaymentStrategy(provider, walletIdentifier);
    }
}
