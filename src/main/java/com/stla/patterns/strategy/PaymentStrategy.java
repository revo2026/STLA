package com.stla.patterns.strategy;

import java.math.BigDecimal;

/**
 * Strategy Pattern: Payment method interface.
 */
public interface PaymentStrategy {
    String getMethodName();
    boolean processPayment(BigDecimal amount, String studentId, String courseId);
    boolean validatePaymentDetails();
}
