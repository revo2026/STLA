package com.stla.patterns.decorator;

import java.math.BigDecimal;

public abstract class CourseDecorator implements CourseComponent {
    protected final CourseComponent wrapped;
    protected CourseDecorator(CourseComponent wrapped) { this.wrapped = wrapped; }
}
