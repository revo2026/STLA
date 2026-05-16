package com.stla.patterns.proxy;

import com.stla.domain.models.CourseLesson;
import com.stla.services.EnrollmentService;

/**
 * Proxy Pattern: Controls access to course lessons based on enrollment and preview status.
 */
public class CourseAccessProxy {

    private final EnrollmentService enrollmentService = new EnrollmentService();

    public boolean canAccessLesson(String studentId, String courseId, CourseLesson lesson) {
        if (lesson == null) return false;
        if (lesson.isPreview()) return true;
        return studentId != null && enrollmentService.isEnrolled(studentId, courseId);
    }

    public boolean canAccessQuizzes(String studentId, String courseId) {
        return studentId != null && enrollmentService.isEnrolled(studentId, courseId);
    }

    public boolean canDownloadResources(String studentId, String courseId) {
        return studentId != null && enrollmentService.isEnrolled(studentId, courseId);
    }

    public String getLockMessage() {
        return "Enroll to Access Course";
    }

    public String getQuizLockMessage() {
        return "Enroll in this course to access quizzes.";
    }
}
