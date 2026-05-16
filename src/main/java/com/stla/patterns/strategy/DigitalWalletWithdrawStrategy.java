package com.stla.patterns.strategy;

/**
 * Strategy for digital wallet withdrawals.
 * Accepts UI lines like {@code Provider • phone} (optional {@code • account}) or legacy phone/ID-only text.
 */
public class DigitalWalletWithdrawStrategy implements WithdrawStrategy {
    @Override
    public String validate(String methodDetails) {
        if (methodDetails == null || methodDetails.isBlank()) {
            return "Wallet phone number or ID is required";
        }
        if (methodDetails.contains("•")) {
            String[] parts = methodDetails.split("•");
            if (parts.length < 2) {
                return "Wallet phone number or ID is required";
            }
            if (parts[0].trim().isBlank()) {
                return "Select a wallet provider.";
            }
            String phoneDigits = parts[1].trim().replaceAll("[^0-9+]", "");
            if (phoneDigits.length() < 6) {
                return "Enter a valid phone number or wallet ID (min 6 characters)";
            }
            return null;
        }
        String cleaned = methodDetails.replaceAll("[^0-9+]", "");
        if (cleaned.length() < 6) {
            return "Enter a valid phone number or wallet ID (min 6 characters)";
        }
        return null;
    }

    @Override public String getMethodLabel() { return "📱 Digital Wallet"; }
    @Override public String getDbValue() { return "digital_wallet"; }
}
