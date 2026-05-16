package com.stla.patterns.strategy;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class VisaPaymentStrategy implements PaymentStrategy {
    private static final Pattern CARD_PATTERN = Pattern.compile("^[0-9]{13,19}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3,4}$");

    private final String cardholderName;
    private final String cardNumber;
    private final String expiryDate;
    private final String cvv;

    public VisaPaymentStrategy(String cardholderName, String cardNumber, String expiryDate, String cvv) {
        this.cardholderName = cardholderName;
        this.cardNumber = cardNumber != null ? cardNumber.replaceAll("\\s", "") : "";
        this.expiryDate = expiryDate;
        this.cvv = cvv;
    }

    @Override public String getMethodName() { return "Visa"; }

    @Override
    public boolean processPayment(BigDecimal amount, String studentId, String courseId) {
        return validatePaymentDetails();
    }

    @Override
    public boolean validatePaymentDetails() {
        if (cardholderName == null || cardholderName.isBlank()) return false;
        if (!CARD_PATTERN.matcher(cardNumber).matches()) return false;
        if (!EXPIRY_PATTERN.matcher(expiryDate != null ? expiryDate.trim() : "").matches()) return false;
        if (!CVV_PATTERN.matcher(cvv != null ? cvv.trim() : "").matches()) return false;
        return isNotExpired();
    }

    private boolean isNotExpired() {
        try {
            String[] parts = expiryDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = 2000 + Integer.parseInt(parts[1]);
            java.time.YearMonth exp = java.time.YearMonth.of(year, month);
            return !exp.isBefore(java.time.YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }

    public String getMaskedCardNumber() {
        if (cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    public String getCardholderName() { return cardholderName; }
    public String getCardNumber() { return cardNumber; }
}
