package com.stla.domain.models;

import java.time.LocalDateTime;

/**
 * Quiz attempt model matching public.quiz_attempts table.
 * Records a student's attempt at a quiz with score and pass/fail.
 */
public class QuizAttempt {

    private String id;
    private String quizId;
    private String studentId;
    private int score;
    private int totalPoints;
    private boolean passed;
    private int attemptNumber;
    private double scorePercent;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime submittedAt;

    // Transient
    private String quizTitle;

    public QuizAttempt() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getQuizTitle() { return quizTitle; }
    public void setQuizTitle(String quizTitle) { this.quizTitle = quizTitle; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public void setScorePercent(double scorePercent) { this.scorePercent = scorePercent; }

    public double getScorePercent() {
        if (attemptNumber > 0 || submittedAt != null) {
            return scorePercent;
        }
        return totalPoints > 0 ? (score * 100.0 / totalPoints) : scorePercent;
    }

    public int getCorrectCount() { return score; }
    public int getWrongCount() {
        return totalPoints > 0 ? totalPoints - score : 0;
    }
}
