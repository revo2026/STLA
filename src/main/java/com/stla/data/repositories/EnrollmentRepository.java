package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.enums.EnrollmentStatus;
import com.stla.domain.models.Enrollment;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class EnrollmentRepository {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean isEnrolled(String studentId, String courseId) {
        String sql = "SELECT 1 FROM enrollments WHERE student_id=?::uuid AND course_id=?::uuid AND status='active'";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            return false;
        }
    }

    public Set<String> findEnrolledCourseIds(String studentId) {
        String sql = "SELECT course_id FROM enrollments WHERE student_id=?::uuid AND status='active'";
        Set<String> ids = new HashSet<>();
        if (studentId == null || studentId.isBlank()) return ids;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("course_id"));
            }
        } catch (SQLException e) {
            System.err.println("Enrolled course ids error: " + e.getMessage());
        }
        return ids;
    }

    public String createEnrollment(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "INSERT INTO enrollments (student_id, course_id, status) VALUES (?::uuid, ?::uuid, 'active') RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String enrollmentId = rs.getString("id");
                    initProgress(conn, studentId, courseId);
                    return enrollmentId;
                }
            }
        }
        throw new SQLException("Failed to create enrollment");
    }

    private void initProgress(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = """
            INSERT INTO student_course_progress (enrollment_id, student_id, course_id, total_lessons)
            SELECT e.id, e.student_id, e.course_id, COUNT(cl.id)
            FROM enrollments e
            LEFT JOIN course_lessons cl ON cl.course_id = e.course_id AND cl.is_published = true
            WHERE e.student_id = ?::uuid AND e.course_id = ?::uuid
            GROUP BY e.id, e.student_id, e.course_id
            ON CONFLICT (student_id, course_id) DO NOTHING
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            ps.executeUpdate();
        }
    }

    public List<Enrollment> findByStudentId(String studentId) {
        String sql = """
            SELECT e.*, c.title as course_title,
                   COALESCE(scp.progress_percent, 0) as progress,
                   COALESCE(scp.lessons_completed, 0) as lessons_completed,
                   COALESCE(scp.total_lessons, 0) as total_lessons,
                   scp.last_accessed_at,
                   cl.title as last_lesson_title
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            LEFT JOIN student_course_progress scp ON scp.enrollment_id = e.id
            LEFT JOIN course_lessons cl ON cl.id = scp.last_lesson_id
            WHERE e.student_id = ?::uuid
            ORDER BY e.enrolled_at DESC
            """;
        List<Enrollment> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEnrollment(rs));
            }
        } catch (SQLException e) {
            System.err.println("Enrollments error: " + e.getMessage());
        }
        return list;
    }

    public Optional<Enrollment> findByStudentAndCourse(String studentId, String courseId) {
        String sql = "SELECT * FROM enrollments WHERE student_id=?::uuid AND course_id=?::uuid";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapEnrollment(rs));
            }
        } catch (SQLException e) {
            System.err.println("Enrollment find error: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Enrollment mapEnrollment(ResultSet rs) throws SQLException {
        Enrollment en = new Enrollment();
        en.setId(rs.getString("id"));
        en.setStudentId(rs.getString("student_id"));
        en.setCourseId(rs.getString("course_id"));
        en.setStatus(EnrollmentStatus.fromValue(rs.getString("status")));
        if (rs.getTimestamp("enrolled_at") != null) {
            en.setEnrolledAt(rs.getTimestamp("enrolled_at").toLocalDateTime());
        }
        try { en.setCourseTitle(rs.getString("course_title")); } catch (SQLException ignored) {}
        try { en.setProgressPercent(rs.getDouble("progress")); } catch (SQLException ignored) {}
        try { en.setLessonsCompleted(rs.getInt("lessons_completed")); } catch (SQLException ignored) {}
        try { en.setTotalLessons(rs.getInt("total_lessons")); } catch (SQLException ignored) {}
        try { en.setLastLessonTitle(rs.getString("last_lesson_title")); } catch (SQLException ignored) {}
        try {
            if (rs.getTimestamp("last_accessed_at") != null) {
                en.setLastAccessedAt(rs.getTimestamp("last_accessed_at").toLocalDateTime());
            }
        } catch (SQLException ignored) {}
        return en;
    }
}
