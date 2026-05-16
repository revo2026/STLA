package com.stla.domain.enums;

/**
 * Question difficulty levels.
 */
public enum QuestionDifficulty {
    EASY("easy",     "Easy",   "#10B981"),
    MEDIUM("medium", "Medium", "#F59E0B"),
    HARD("hard",     "Hard",   "#EF4444");

    private final String dbValue;
    private final String displayLabel;
    private final String color;

    QuestionDifficulty(String dbValue, String displayLabel, String color) {
        this.dbValue = dbValue;
        this.displayLabel = displayLabel;
        this.color = color;
    }

    public String getDbValue()      { return dbValue; }
    public String getDisplayLabel() { return displayLabel; }
    public String getColor()        { return color; }

    public static QuestionDifficulty fromDbValue(String value) {
        if (value == null) return MEDIUM;
        for (QuestionDifficulty d : values()) {
            if (d.dbValue.equalsIgnoreCase(value)) return d;
        }
        return MEDIUM;
    }
}
