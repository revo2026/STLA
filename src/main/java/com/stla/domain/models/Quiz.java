package com.stla.domain.models;

import java.time.LocalDateTime;

public class Quiz {
    private String id;
    private String courseId;
    private String lessonId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private int passingScore;
    private int attemptsAllowed;
    private boolean shuffleQuestions;
    private boolean showAnswersAfterSubmit;
    private boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Quiz() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String cid) { this.courseId = cid; }
    public String getLessonId() { return lessonId; }
    public void setLessonId(String lid) { this.lessonId = lid; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer t) { this.timeLimitMinutes = t; }
    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int p) { this.passingScore = p; }
    public int getAttemptsAllowed() { return attemptsAllowed; }
    public void setAttemptsAllowed(int a) { this.attemptsAllowed = a; }
    public boolean isShuffleQuestions() { return shuffleQuestions; }
    public void setShuffleQuestions(boolean s) { this.shuffleQuestions = s; }
    public boolean isShowAnswersAfterSubmit() { return showAnswersAfterSubmit; }
    public void setShowAnswersAfterSubmit(boolean s) { this.showAnswersAfterSubmit = s; }
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean p) { isPublished = p; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime u) { this.updatedAt = u; }
}
