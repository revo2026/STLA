package com.stla.patterns.decorator;

import java.math.BigDecimal;

/**
 * Decorator Pattern: Base interface for course pricing with add-ons.
 */
public interface CourseComponent {
    String getDescription();
    BigDecimal getPrice();
}
