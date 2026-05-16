package com.stla.domain.models;

import java.time.LocalDateTime;

public class Review {
    private String id;
    private String courseId;
    private String studentId;
    private String enrollmentId;
    private int rating;
    private String title;
    private String comment;
    private boolean isPublished;
    private LocalDateTime createdAt;

    // Transient (joined in queries)
    private String studentName;
    private String courseTitle;
    private String instructorName;

    public Review() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String cid) { this.courseId = cid; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String sid) { this.studentId = sid; }
    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String eid) { this.enrollmentId = eid; }
    public int getRating() { return rating; }
    public void setRating(int r) { this.rating = r; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getComment() { return comment; }
    public void setComment(String c) { this.comment = c; }
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean p) { isPublished = p; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String n) { this.studentName = n; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
}
