package com.stla.patterns;

import com.stla.patterns.strategy.PaymentStrategy;
import com.stla.patterns.strategy.VisaPaymentStrategy;
import com.stla.patterns.strategy.PayPalPaymentStrategy;
import com.stla.patterns.strategy.WalletPaymentStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Strategy Pattern — Payment strategies.
 */
@DisplayName("Strategy Pattern: Payment Strategies")
class StrategyPatternTest {

    @Test
    @DisplayName("VisaPaymentStrategy should have correct name")
    void visaMethodName() {
        VisaPaymentStrategy visa = new VisaPaymentStrategy("John Doe", "4111111111111111", "12/30", "123");
        assertEquals("Visa", visa.getMethodName());
    }

    @Test
    @DisplayName("VisaPaymentStrategy should validate valid card details")
    void visaValidateValid() {
        VisaPaymentStrategy visa = new VisaPaymentStrategy("John Doe", "4111111111111111", "12/30", "123");
        assertTrue(visa.validatePaymentDetails());
    }

    @Test
    @DisplayName("VisaPaymentStrategy should reject empty card number")
    void visaValidateInvalid() {
        VisaPaymentStrategy visa = new VisaPaymentStrategy("", "", "12/30", "123");
        assertFalse(visa.validatePaymentDetails());
    }

    @Test
    @DisplayName("VisaPaymentStrategy should process payment")
    void visaProcess() {
        VisaPaymentStrategy visa = new VisaPaymentStrategy("John Doe", "4111111111111111", "12/30", "123");
        assertTrue(visa.processPayment(new BigDecimal("99.99"), "student-1", "course-1"));
    }

    @Test
    @DisplayName("PayPalPaymentStrategy should have correct name")
    void paypalMethodName() {
        PayPalPaymentStrategy pp = new PayPalPaymentStrategy("test@email.com");
        assertEquals("PayPal", pp.getMethodName());
    }

    @Test
    @DisplayName("PayPalPaymentStrategy should validate with email")
    void paypalValidate() {
        PayPalPaymentStrategy pp = new PayPalPaymentStrategy("user@test.com");
        assertTrue(pp.validatePaymentDetails());
    }

    @Test
    @DisplayName("PayPalPaymentStrategy should reject empty email")
    void paypalValidateEmpty() {
        PayPalPaymentStrategy pp = new PayPalPaymentStrategy("");
        assertFalse(pp.validatePaymentDetails());
    }

    @Test
    @DisplayName("WalletPaymentStrategy should have correct name")
    void walletMethodName() {
        WalletPaymentStrategy ws = new WalletPaymentStrategy("Vodafone Cash", "01012345678");
        assertEquals("Digital Wallet", ws.getMethodName());
    }

    @Test
    @DisplayName("WalletPaymentStrategy should validate sufficient balance")
    void walletValidate() {
        WalletPaymentStrategy ws = new WalletPaymentStrategy("Vodafone Cash", "01012345678");
        assertTrue(ws.validatePaymentDetails());
    }

    @Test
    @DisplayName("WalletPaymentStrategy should process when sufficient balance")
    void walletProcessSufficientBalance() {
        WalletPaymentStrategy ws = new WalletPaymentStrategy("PayPal", "user@test.com");
        assertTrue(ws.processPayment(new BigDecimal("100"), "s1", "c1"));
    }

    @Test
    @DisplayName("All strategies implement PaymentStrategy interface")
    void allImplementInterface() {
        assertInstanceOf(PaymentStrategy.class, new VisaPaymentStrategy("Jane", "4111111111111111", "12/30", "123"));
        assertInstanceOf(PaymentStrategy.class, new PayPalPaymentStrategy("a@b.com"));
        assertInstanceOf(PaymentStrategy.class, new WalletPaymentStrategy("Orange Cash", "01098765432"));
    }
}
