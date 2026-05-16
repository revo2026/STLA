package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;

import java.sql.*;
import java.util.Optional;

public class StudentCourseProgressRepository {

    public record CourseProgressSnapshot(
            String enrollmentId, double progressPercent, int lessonsCompleted, int totalLessons,
            String lastLessonId, Timestamp lastAccessedAt) {}

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public Optional<CourseProgressSnapshot> findByStudentAndCourse(String studentId, String courseId) {
        String sql = """
            SELECT enrollment_id, progress_percent, lessons_completed, total_lessons, last_lesson_id, last_accessed_at
            FROM student_course_progress
            WHERE student_id = ?::uuid AND course_id = ?::uuid
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CourseProgressSnapshot(
                            rs.getString("enrollment_id"),
                            rs.getDouble("progress_percent"),
                            rs.getInt("lessons_completed"),
                            rs.getInt("total_lessons"),
                            rs.getString("last_lesson_id"),
                            rs.getTimestamp("last_accessed_at")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Course progress find error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void updateProgress(String enrollmentId, int lessonsCompleted, int totalLessons,
                               double progressPercent, String lastLessonId) {
        String sql = """
            UPDATE student_course_progress
            SET lessons_completed = ?, total_lessons = ?, progress_percent = ?,
                last_lesson_id = ?::uuid, last_accessed_at = now(), updated_at = now()
            WHERE enrollment_id = ?::uuid
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, lessonsCompleted);
            ps.setInt(2, totalLessons);
            ps.setDouble(3, progressPercent);
            if (lastLessonId != null) ps.setString(4, lastLessonId);
            else ps.setNull(4, Types.OTHER);
            ps.setString(5, enrollmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Course progress update error: " + e.getMessage());
        }
    }

    public void touchAccess(String studentId, String courseId, String lessonId) {
        String sql = lessonId != null ? """
            UPDATE student_course_progress
            SET last_lesson_id = ?::uuid, last_accessed_at = now(), updated_at = now()
            WHERE student_id = ?::uuid AND course_id = ?::uuid
            """ : """
            UPDATE student_course_progress
            SET last_accessed_at = now(), updated_at = now()
            WHERE student_id = ?::uuid AND course_id = ?::uuid
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (lessonId != null) ps.setString(i++, lessonId);
            ps.setString(i++, studentId);
            ps.setString(i, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Touch course access error: " + e.getMessage());
        }
    }

    public void ensureProgressRow(String studentId, String courseId) {
        String sql = """
            INSERT INTO student_course_progress (enrollment_id, student_id, course_id, total_lessons)
            SELECT e.id, e.student_id, e.course_id,
                   (SELECT COUNT(*) FROM course_lessons cl WHERE cl.course_id = e.course_id AND cl.is_published = true)
            FROM enrollments e
            WHERE e.student_id = ?::uuid AND e.course_id = ?::uuid
            ON CONFLICT (student_id, course_id) DO NOTHING
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ensure progress row error: " + e.getMessage());
        }
    }
}
