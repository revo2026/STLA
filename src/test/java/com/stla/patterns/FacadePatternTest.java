package com.stla.patterns;

import com.stla.patterns.facade.EnrollmentFacade;
import com.stla.patterns.strategy.VisaPaymentStrategy;
import com.stla.patterns.strategy.PayPalPaymentStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Facade Pattern — EnrollmentFacade.
 */
@DisplayName("Facade Pattern: EnrollmentFacade")
class FacadePatternTest {

    @Test
    @DisplayName("Enrollment with valid Visa should succeed")
    void enrollWithVisaSucceeds() {
        EnrollmentFacade facade = EnrollmentFacade.forGatewayTesting();
        VisaPaymentStrategy visa = new VisaPaymentStrategy("John Doe", "4111111111111111", "12/28", "123");

        var result = facade.purchaseCourse("student-1", "course-1", new BigDecimal("99.99"), visa, "Visa", null);

        assertTrue(result.success());
        assertEquals("Enrollment successful!", result.message());
    }

    @Test
    @DisplayName("Enrollment with valid PayPal should succeed")
    void enrollWithPayPalSucceeds() {
        EnrollmentFacade facade = EnrollmentFacade.forGatewayTesting();
        PayPalPaymentStrategy paypal = new PayPalPaymentStrategy("user@test.com");

        var result = facade.purchaseCourse("student-2", "course-1", new BigDecimal("50.00"), paypal, "PayPal", null);

        assertTrue(result.success());
    }

    @Test
    @DisplayName("Enrollment with invalid payment should fail")
    void enrollWithInvalidPaymentFails() {
        EnrollmentFacade facade = EnrollmentFacade.forGatewayTesting();
        VisaPaymentStrategy visa = new VisaPaymentStrategy("", "", "", "");

        var result = facade.purchaseCourse("student-1", "course-1", new BigDecimal("99.99"), visa, "Visa", null);

        assertFalse(result.success());
        assertEquals("Invalid payment details.", result.message());
    }

    @Test
    @DisplayName("EnrollmentResult record accessors work")
    void enrollmentResultAccessors() {
        var result = new EnrollmentFacade.PurchaseResult(true, "OK", "p1", "e1", null);
        assertTrue(result.success());
        assertEquals("OK", result.message());
    }
}
