package com.stla.patterns.facade;

import com.stla.domain.models.Payment;
import com.stla.patterns.strategy.PaymentStrategy;
import com.stla.services.PaymentService;

import java.math.BigDecimal;

/**
 * Facade Pattern: Orchestrates payment, enrollment, wallet distribution, and notifications.
 */
public class EnrollmentFacade {

    private final PaymentService paymentService;
    private final boolean gatewayOnly;

    public EnrollmentFacade() {
        this(new PaymentService(), false);
    }

    public EnrollmentFacade(PaymentService paymentService, boolean gatewayOnly) {
        this.paymentService = paymentService;
        this.gatewayOnly = gatewayOnly;
    }

    /** For unit tests — validates gateway only without database. */
    public static EnrollmentFacade forGatewayTesting() {
        return new EnrollmentFacade(new PaymentService(), true);
    }

    public PurchaseResult purchaseCourse(String studentId, String courseId, BigDecimal amount,
                                         PaymentStrategy paymentStrategy, String paymentMethod, String walletProvider) {
        if (!paymentStrategy.validatePaymentDetails()) {
            return new PurchaseResult(false, "Invalid payment details.", null, null, null);
        }
        if (!paymentStrategy.processPayment(amount, studentId, courseId)) {
            return new PurchaseResult(false, "Payment failed.", null, null, null);
        }
        if (gatewayOnly) {
            return new PurchaseResult(true, "Enrollment successful!", "TEST-PAY", "TEST-ENR", null);
        }
        return paymentService.processCoursePurchase(studentId, courseId, amount, paymentStrategy, paymentMethod, walletProvider);
    }

    /** @deprecated Use {@link #purchaseCourse} */
    public PurchaseResult enroll(String studentId, String courseId, BigDecimal amount, PaymentStrategy paymentStrategy) {
        return purchaseCourse(studentId, courseId, amount, paymentStrategy, paymentStrategy.getMethodName(), null);
    }

    public record PurchaseResult(boolean success, String message, String paymentId, String enrollmentId, Payment payment) {}

    /** @deprecated Use {@link PurchaseResult} */
    public record EnrollmentResult(boolean success, String message) {
        public static EnrollmentResult from(PurchaseResult r) {
            return new EnrollmentResult(r.success(), r.message());
        }
    }
}
