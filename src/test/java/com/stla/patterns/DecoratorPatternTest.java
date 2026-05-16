package com.stla.patterns;

import com.stla.patterns.decorator.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Decorator Pattern — Course pricing add-ons.
 */
@DisplayName("Decorator Pattern: Course Add-ons")
class DecoratorPatternTest {

    @Test
    @DisplayName("BaseCourse should return base price and title")
    void baseCourseTest() {
        BaseCourse base = new BaseCourse("Java 101", new BigDecimal("49.99"));
        assertEquals("Java 101", base.getDescription());
        assertEquals(new BigDecimal("49.99"), base.getPrice());
    }

    @Test
    @DisplayName("CertificateDecorator should add $10 to price")
    void certificateAddsPrice() {
        CourseComponent course = new BaseCourse("React", new BigDecimal("50.00"));
        CourseComponent withCert = new CertificateDecorator(course);

        assertEquals(new BigDecimal("60.00"), withCert.getPrice());
        assertTrue(withCert.getDescription().contains("Certificate"));
    }

    @Test
    @DisplayName("QuizDecorator should be a free add-on ($0)")
    void quizIsFreeAddon() {
        CourseComponent course = new BaseCourse("Python", new BigDecimal("30.00"));
        CourseComponent withQuiz = new QuizDecorator(course);

        assertEquals(new BigDecimal("30.00"), withQuiz.getPrice());
        assertTrue(withQuiz.getDescription().contains("Quiz"));
    }

    @Test
    @DisplayName("MentorSupportDecorator should add $30 to price")
    void mentorAddsPrice() {
        CourseComponent course = new BaseCourse("AI Course", new BigDecimal("100.00"));
        CourseComponent withMentor = new MentorSupportDecorator(course);

        assertEquals(new BigDecimal("130.00"), withMentor.getPrice());
        assertTrue(withMentor.getDescription().contains("Mentor"));
    }

    @Test
    @DisplayName("Multiple decorators should stack prices correctly")
    void multipleDecoratorsStack() {
        CourseComponent course = new BaseCourse("Full Stack", new BigDecimal("80.00"));
        course = new CertificateDecorator(course);  // +10
        course = new QuizDecorator(course);          // +0 (free)
        course = new MentorSupportDecorator(course); // +30

        // 80 + 10 + 0 + 30 = 120
        assertEquals(new BigDecimal("120.00"), course.getPrice());
        String desc = course.getDescription();
        assertTrue(desc.contains("Full Stack"));
        assertTrue(desc.contains("Certificate"));
        assertTrue(desc.contains("Quiz"));
        assertTrue(desc.contains("Mentor"));
    }

    @Test
    @DisplayName("Decorators implement CourseComponent interface")
    void allImplementInterface() {
        CourseComponent base = new BaseCourse("Test", BigDecimal.TEN);
        assertInstanceOf(CourseComponent.class, new CertificateDecorator(base));
        assertInstanceOf(CourseComponent.class, new QuizDecorator(base));
        assertInstanceOf(CourseComponent.class, new MentorSupportDecorator(base));
    }
}
