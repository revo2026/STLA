package com.stla.domain.models;

import java.time.LocalDateTime;

public class CourseLesson {
    private String id;
    private String courseId;
    private String sectionId;
    private String title;
    private String description;
    private int lessonOrder;
    private String videoUrl;
    private int durationSeconds;
    private boolean isPreview;
    private boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient
    private boolean completed;

    public CourseLesson() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public int getLessonOrder() { return lessonOrder; }
    public void setLessonOrder(int o) { this.lessonOrder = o; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String u) { this.videoUrl = u; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int d) { this.durationSeconds = d; }
    public boolean isPreview() { return isPreview; }
    public void setPreview(boolean p) { isPreview = p; }
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean p) { isPublished = p; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime u) { this.updatedAt = u; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean c) { this.completed = c; }

    public String getDurationFormatted() {
        int m = durationSeconds / 60;
        int s = durationSeconds % 60;
        return String.format("%d:%02d", m, s);
    }
}
