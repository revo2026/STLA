package com.stla.services;

import com.stla.data.repositories.CourseRepositoryImpl;
import com.stla.domain.models.*;
import com.stla.core.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseDetailsService {

    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final CourseService courseService = new CourseService();

    public Optional<CourseDetailsView> getCourseDetails(String courseId, String studentId) {
        Optional<Course> courseOpt = courseRepo.findById(courseId);
        if (courseOpt.isEmpty()) return Optional.empty();

        Course course = courseOpt.get();
        CourseDetailsView view = new CourseDetailsView();
        view.setCourse(course);
        view.setEnrolled(studentId != null && enrollmentService.isEnrolled(studentId, courseId));
        loadInstructorInfo(course.getInstructorId(), view);
        loadStats(courseId, view);
        view.setPreviewLessons(courseService.getCourseLessons(courseId).stream().filter(CourseLesson::isPreview).toList());
        view.setReviews(loadReviews(courseId));
        return Optional.of(view);
    }

    private void loadInstructorInfo(String instructorId, CourseDetailsView view) {
        String sql = """
            SELECT p.full_name, p.bio
            FROM instructors i
            JOIN profiles p ON p.id = i.profile_id
            WHERE i.id = ?::uuid
            """;
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    view.setInstructorName(rs.getString("full_name"));
                    view.setInstructorBio(rs.getString("bio"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Instructor info error: " + e.getMessage());
        }
    }

    private void loadStats(String courseId, CourseDetailsView view) {
        try (Connection c = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(DISTINCT section_id) FROM course_lessons WHERE course_id=?::uuid AND section_id IS NOT NULL")) {
                ps.setString(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) view.setSectionsCount(rs.getInt(1));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*), COALESCE(SUM(duration_seconds),0)/60 FROM course_lessons WHERE course_id=?::uuid")) {
                ps.setString(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        view.setLessonsCount(rs.getInt(1));
                        view.setTotalDurationMinutes(rs.getInt(2));
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM course_lessons WHERE course_id=?::uuid AND is_preview=true")) {
                ps.setString(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) view.setPreviewLessonsCount(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Course stats error: " + e.getMessage());
        }
    }

    private List<CourseReview> loadReviews(String courseId) {
        String sql = """
            SELECT r.*, p.full_name as student_name
            FROM reviews r
            JOIN students s ON s.id = r.student_id
            JOIN profiles p ON p.id = s.profile_id
            WHERE r.course_id = ?::uuid AND r.is_published = true
            ORDER BY r.created_at DESC LIMIT 10
            """;
        List<CourseReview> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CourseReview r = new CourseReview();
                    r.setId(rs.getString("id"));
                    r.setCourseId(rs.getString("course_id"));
                    r.setStudentId(rs.getString("student_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setTitle(rs.getString("title"));
                    r.setComment(rs.getString("comment"));
                    r.setStudentName(rs.getString("student_name"));
                    if (rs.getTimestamp("created_at") != null) {
                        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Reviews error: " + e.getMessage());
        }
        return list;
    }
}
