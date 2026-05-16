package com.stla.domain.enums;

public enum QuizAccessStatus {
    LOCKED,
    AVAILABLE,
    COMPLETED;

    public String getLabel() {
        return switch (this) {
            case LOCKED -> "Locked";
            case AVAILABLE -> "Available";
            case COMPLETED -> "Passed";
        };
    }
}
