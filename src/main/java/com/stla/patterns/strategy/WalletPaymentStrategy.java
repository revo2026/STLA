package com.stla.patterns.strategy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

public class WalletPaymentStrategy implements PaymentStrategy {
    private static final Set<String> PROVIDERS = Set.of(
            "Vodafone Cash", "Orange Cash", "Etisalat Cash", "PayPal", "Fawry", "InstaPay"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+]{10,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private final String provider;
    private final String walletIdentifier;

    public WalletPaymentStrategy(String provider, String walletIdentifier) {
        this.provider = provider;
        this.walletIdentifier = walletIdentifier != null ? walletIdentifier.trim() : "";
    }

    @Override public String getMethodName() { return "Digital Wallet"; }

    @Override
    public boolean processPayment(BigDecimal amount, String studentId, String courseId) {
        return validatePaymentDetails();
    }

    @Override
    public boolean validatePaymentDetails() {
        if (provider == null || provider.isBlank() || !PROVIDERS.contains(provider)) return false;
        if (walletIdentifier.isBlank()) return false;
        if ("PayPal".equals(provider)) {
            return EMAIL_PATTERN.matcher(walletIdentifier).matches();
        }
        return PHONE_PATTERN.matcher(walletIdentifier.replaceAll("\\s", "")).matches();
    }

    public String getProvider() { return provider; }
    public String getWalletIdentifier() { return walletIdentifier; }
}
