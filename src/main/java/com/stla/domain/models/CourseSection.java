package com.stla.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Course section model matching public.course_sections table.
 * Groups lessons within a course for structured content delivery.
 */
public class CourseSection {

    private String id;
    private String courseId;
    private String title;
    private int sectionOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient: loaded lessons within this section
    private List<CourseLesson> lessons = new ArrayList<>();

    public CourseSection() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getSectionOrder() { return sectionOrder; }
    public void setSectionOrder(int sectionOrder) { this.sectionOrder = sectionOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<CourseLesson> getLessons() { return lessons; }
    public void setLessons(List<CourseLesson> lessons) { this.lessons = lessons; }
}
