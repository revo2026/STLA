package com.stla.domain;

import com.stla.domain.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Domain Enums — fromValue mapping.
 */
@DisplayName("Domain Enums")
class EnumsTest {

    @Test
    @DisplayName("AppRole fromValue should map correctly")
    void appRoleFromValue() {
        assertEquals(AppRole.STUDENT, AppRole.fromValue("student"));
        assertEquals(AppRole.INSTRUCTOR, AppRole.fromValue("instructor"));
        assertEquals(AppRole.ADMIN, AppRole.fromValue("admin"));
    }

    @Test
    @DisplayName("AppRole fromValue should throw on unknown")
    void appRoleFromValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> AppRole.fromValue("unknown"));
    }

    @Test
    @DisplayName("CourseStatus fromValue should map correctly")
    void courseStatusFromValue() {
        assertEquals(CourseStatus.DRAFT, CourseStatus.fromValue("draft"));
        assertEquals(CourseStatus.PENDING, CourseStatus.fromValue("pending"));
        assertEquals(CourseStatus.APPROVED, CourseStatus.fromValue("approved"));
        assertEquals(CourseStatus.REJECTED, CourseStatus.fromValue("rejected"));
    }

    @Test
    @DisplayName("CourseLevel fromValue should map correctly")
    void courseLevelFromValue() {
        assertEquals(CourseLevel.BEGINNER, CourseLevel.fromValue("beginner"));
        assertEquals(CourseLevel.INTERMEDIATE, CourseLevel.fromValue("intermediate"));
        assertEquals(CourseLevel.ADVANCED, CourseLevel.fromValue("advanced"));
    }

    @Test
    @DisplayName("EnrollmentStatus fromValue should map correctly")
    void enrollmentStatusFromValue() {
        assertEquals(EnrollmentStatus.ACTIVE, EnrollmentStatus.fromValue("active"));
        assertEquals(EnrollmentStatus.COMPLETED, EnrollmentStatus.fromValue("completed"));
        assertEquals(EnrollmentStatus.CANCELLED, EnrollmentStatus.fromValue("cancelled"));
    }

    @Test
    @DisplayName("PaymentStatus fromValue should map correctly")
    void paymentStatusFromValue() {
        assertEquals(PaymentStatus.PENDING, PaymentStatus.fromValue("pending"));
        assertEquals(PaymentStatus.PAID, PaymentStatus.fromValue("paid"));
        assertEquals(PaymentStatus.FAILED, PaymentStatus.fromValue("failed"));
        assertEquals(PaymentStatus.REFUNDED, PaymentStatus.fromValue("refunded"));
    }

    @Test
    @DisplayName("Enum values should return correct string")
    void enumValuesReturnString() {
        assertEquals("student", AppRole.STUDENT.getValue());
        assertEquals("draft", CourseStatus.DRAFT.getValue());
        assertEquals("beginner", CourseLevel.BEGINNER.getValue());
        assertEquals("active", EnrollmentStatus.ACTIVE.getValue());
        assertEquals("pending", PaymentStatus.PENDING.getValue());
    }
}
