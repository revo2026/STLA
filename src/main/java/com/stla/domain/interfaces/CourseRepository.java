package com.stla.domain.interfaces;

import com.stla.domain.models.Course;
import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Optional<Course> findById(String id);
    List<Course> findAll();
    List<Course> findApproved();
    List<Course> findFeatured();
    List<Course> findByInstructorId(String instructorId);
    List<Course> findByCategory(String categoryId);
    List<Course> findByStatus(String status);
    List<Course> searchByTitle(String keyword);
    void save(Course course);
    void update(Course course);
    void updateStatus(String id, String status, String approvalNote, String adminId);
    int countAll();
    int countByStatus(String status);
}
