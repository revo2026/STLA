package com.stla.patterns.strategy;

/**
 * Strategy Pattern: Interface for withdrawal method validation.
 */
public interface WithdrawStrategy {
    /** Validate method-specific details. Returns null if valid, error message if invalid. */
    String validate(String methodDetails);
    /** Human-readable label for this method */
    String getMethodLabel();
    /** Database enum value */
    String getDbValue();
}
