package com.stla.patterns.decorator;

import java.math.BigDecimal;

public class MentorSupportDecorator extends CourseDecorator {
    public MentorSupportDecorator(CourseComponent w) { super(w); }
    @Override public String getDescription() { return wrapped.getDescription() + " + Mentor Support"; }
    @Override public BigDecimal getPrice() { return wrapped.getPrice().add(new BigDecimal("30.00")); }
}
