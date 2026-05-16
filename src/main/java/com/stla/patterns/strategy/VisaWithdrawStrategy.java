package com.stla.patterns.strategy;

/**
 * Strategy for Visa card withdrawals.
 * Accepts UI-generated masked summaries ({@code Name • ****1234 • exp MM/YY}) or legacy last-4 style input.
 */
public class VisaWithdrawStrategy implements WithdrawStrategy {
    @Override
    public String validate(String methodDetails) {
        if (methodDetails == null || methodDetails.isBlank()) {
            return "Visa card details are required";
        }
        if (methodDetails.contains("****") && methodDetails.contains("exp ") && methodDetails.contains("•")) {
            return null;
        }
        String digits = methodDetails.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return "Enter at least the last 4 digits of your Visa card";
        }
        if (digits.length() > 16) {
            return "Invalid card number format";
        }
        return null;
    }

    @Override public String getMethodLabel() { return "💳 Visa Card"; }
    @Override public String getDbValue() { return "visa"; }
}
