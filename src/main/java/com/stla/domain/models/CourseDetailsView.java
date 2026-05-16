package com.stla.domain.models;

import java.math.BigDecimal;
import java.util.List;

/** Aggregated course details for the student course page. */
public class CourseDetailsView {
    private Course course;
    private String instructorName;
    private String instructorBio;
    private int sectionsCount;
    private int lessonsCount;
    private int previewLessonsCount;
    private int totalDurationMinutes;
    private List<CourseLesson> previewLessons;
    private List<CourseReview> reviews;
    private boolean enrolled;

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public String getInstructorBio() { return instructorBio; }
    public void setInstructorBio(String instructorBio) { this.instructorBio = instructorBio; }
    public int getSectionsCount() { return sectionsCount; }
    public void setSectionsCount(int sectionsCount) { this.sectionsCount = sectionsCount; }
    public int getLessonsCount() { return lessonsCount; }
    public void setLessonsCount(int lessonsCount) { this.lessonsCount = lessonsCount; }
    public int getPreviewLessonsCount() { return previewLessonsCount; }
    public void setPreviewLessonsCount(int previewLessonsCount) { this.previewLessonsCount = previewLessonsCount; }
    public int getTotalDurationMinutes() { return totalDurationMinutes; }
    public void setTotalDurationMinutes(int totalDurationMinutes) { this.totalDurationMinutes = totalDurationMinutes; }
    public List<CourseLesson> getPreviewLessons() { return previewLessons; }
    public void setPreviewLessons(List<CourseLesson> previewLessons) { this.previewLessons = previewLessons; }
    public List<CourseReview> getReviews() { return reviews; }
    public void setReviews(List<CourseReview> reviews) { this.reviews = reviews; }
    public boolean isEnrolled() { return enrolled; }
    public void setEnrolled(boolean enrolled) { this.enrolled = enrolled; }

    public BigDecimal getEffectivePrice() {
        if (course == null || course.getPrice() == null) return BigDecimal.ZERO;
        return course.getPrice();
    }
}
