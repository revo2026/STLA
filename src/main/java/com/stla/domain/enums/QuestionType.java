package com.stla.domain.enums;

/**
 * All supported quiz question types.
 * DB column: quiz_question_type enum.
 */
public enum QuestionType {
    SINGLE_CHOICE("single_choice",   "Single Choice",    "○"),
    MULTIPLE_CHOICE("multiple_choice", "Multiple Choice", "☑"),
    TRUE_FALSE("true_false",         "True / False",      "✓"),
    SHORT_ANSWER("short_answer",     "Short Answer",      "✎"),
    ESSAY("essay",                   "Essay",             "📝"),
    FILL_BLANK("fill_blank",         "Fill in the Blank", "▁"),
    MATCHING("matching",             "Matching",          "⇔"),
    ORDERING("ordering",             "Ordering",          "↕"),
    IMAGE_QUESTION("image_question", "Image Question",    "🖼");

    private final String dbValue;
    private final String displayLabel;
    private final String icon;

    QuestionType(String dbValue, String displayLabel, String icon) {
        this.dbValue = dbValue;
        this.displayLabel = displayLabel;
        this.icon = icon;
    }

    public String getDbValue()      { return dbValue; }
    public String getDisplayLabel() { return displayLabel; }
    public String getIcon()         { return icon; }

    public static QuestionType fromDbValue(String value) {
        if (value == null) return SINGLE_CHOICE;
        for (QuestionType t : values()) {
            if (t.dbValue.equals(value)) return t;
        }
        return SINGLE_CHOICE;
    }

    /** Whether this type uses quiz_options */
    public boolean usesOptions() {
        return this == SINGLE_CHOICE || this == MULTIPLE_CHOICE || this == TRUE_FALSE || this == IMAGE_QUESTION;
    }

    /** Whether this type uses accepted answers */
    public boolean usesAcceptedAnswers() {
        return this == SHORT_ANSWER || this == FILL_BLANK;
    }

    /** Whether this type uses match pairs */
    public boolean usesMatchPairs() {
        return this == MATCHING;
    }

    /** Whether this type uses sequence items */
    public boolean usesSequenceItems() {
        return this == ORDERING;
    }

    /** Whether this type requires manual grading */
    public boolean requiresManualGrading() {
        return this == ESSAY;
    }
}
