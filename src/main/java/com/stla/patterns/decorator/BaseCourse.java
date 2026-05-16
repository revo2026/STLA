package com.stla.patterns.decorator;

import java.math.BigDecimal;

public class BaseCourse implements CourseComponent {
    private final String title;
    private final BigDecimal basePrice;

    public BaseCourse(String title, BigDecimal basePrice) {
        this.title = title;
        this.basePrice = basePrice;
    }

    @Override public String getDescription() { return title; }
    @Override public BigDecimal getPrice() { return basePrice; }
}
