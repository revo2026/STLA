package com.stla.patterns.decorator;

import java.math.BigDecimal;

public class QuizDecorator extends CourseDecorator {
    public QuizDecorator(CourseComponent w) { super(w); }
    @Override public String getDescription() { return wrapped.getDescription() + " + Quiz"; }
    @Override public BigDecimal getPrice() { return wrapped.getPrice(); } // Quiz is free add-on
}
