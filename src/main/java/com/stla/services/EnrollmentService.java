package com.stla.services;

import com.stla.data.repositories.EnrollmentRepository;
import com.stla.domain.models.Enrollment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepo = new EnrollmentRepository();

    public boolean isEnrolled(String studentId, String courseId) {
        return enrollmentRepo.isEnrolled(studentId, courseId);
    }

    public List<Enrollment> getStudentEnrollments(String studentId) {
        return enrollmentRepo.findByStudentId(studentId);
    }

    public Optional<Enrollment> getEnrollment(String studentId, String courseId) {
        return enrollmentRepo.findByStudentAndCourse(studentId, courseId);
    }

    public Set<String> getEnrolledCourseIds(String studentId) {
        return enrollmentRepo.findEnrolledCourseIds(studentId);
    }
}
