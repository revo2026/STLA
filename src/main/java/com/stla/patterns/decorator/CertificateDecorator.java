package com.stla.patterns.decorator;

import java.math.BigDecimal;

public class CertificateDecorator extends CourseDecorator {
    public CertificateDecorator(CourseComponent w) { super(w); }
    @Override public String getDescription() { return wrapped.getDescription() + " + Certificate"; }
    @Override public BigDecimal getPrice() { return wrapped.getPrice().add(new BigDecimal("10.00")); }
}
