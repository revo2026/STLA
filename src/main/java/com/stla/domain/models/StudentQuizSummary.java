package com.stla.domain.models;

import com.stla.domain.enums.QuizAccessStatus;

/**
 * Quiz metadata for student catalog, details, and player views.
 */
public class StudentQuizSummary {

    private Quiz quiz;
    private int questionCount;
    private int attemptsUsed;
    private double bestScorePercent;
    private boolean passed;
    private boolean locked;
    private QuizAccessStatus status;
    private String lessonTitle;
    private String courseId;
    private String courseTitle;

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public int getAttemptsUsed() { return attemptsUsed; }
    public void setAttemptsUsed(int attemptsUsed) { this.attemptsUsed = attemptsUsed; }

    public int getAttemptsRemaining() {
        if (quiz == null) return 0;
        return Math.max(0, quiz.getAttemptsAllowed() - attemptsUsed);
    }

    public double getBestScorePercent() { return bestScorePercent; }
    public void setBestScorePercent(double bestScorePercent) { this.bestScorePercent = bestScorePercent; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public QuizAccessStatus getStatus() { return status; }
    public void setStatus(QuizAccessStatus status) { this.status = status; }

    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
}
