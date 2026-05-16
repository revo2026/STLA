package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;

import java.sql.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class LessonProgressRepository {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public void upsertWatchProgress(String studentId, String lessonId, String enrollmentId,
                                    int watchedSeconds, int lastPositionSeconds) {
        String sql = """
            INSERT INTO lesson_progress (student_id, lesson_id, enrollment_id, watched_seconds, last_position_seconds)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?)
            ON CONFLICT (student_id, lesson_id) DO UPDATE SET
              watched_seconds = GREATEST(lesson_progress.watched_seconds, EXCLUDED.watched_seconds),
              last_position_seconds = EXCLUDED.last_position_seconds,
              enrollment_id = COALESCE(EXCLUDED.enrollment_id, lesson_progress.enrollment_id),
              updated_at = now()
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, lessonId);
            if (enrollmentId != null) ps.setString(3, enrollmentId);
            else ps.setNull(3, Types.OTHER);
            ps.setInt(4, watchedSeconds);
            ps.setInt(5, lastPositionSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lesson watch progress error: " + e.getMessage());
        }
    }

    public int getWatchedSeconds(String studentId, String lessonId) {
        String sql = "SELECT watched_seconds FROM lesson_progress WHERE student_id=?::uuid AND lesson_id=?::uuid";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("watched_seconds");
            }
        } catch (SQLException e) {
            System.err.println("Lesson watched seconds error: " + e.getMessage());
        }
        return 0;
    }

    public boolean isCompleted(String studentId, String lessonId) {
        String sql = "SELECT completed FROM lesson_progress WHERE student_id=?::uuid AND lesson_id=?::uuid";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBoolean("completed");
            }
        } catch (SQLException e) {
            System.err.println("Lesson completed check error: " + e.getMessage());
        }
        return false;
    }

    public void markCompleted(String studentId, String lessonId, String enrollmentId, int watchedSeconds) {
        String sql = """
            INSERT INTO lesson_progress (student_id, lesson_id, enrollment_id, watched_seconds, completed, completed_at)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?, true, now())
            ON CONFLICT (student_id, lesson_id) DO UPDATE SET
              completed = true,
              completed_at = now(),
              watched_seconds = GREATEST(lesson_progress.watched_seconds, EXCLUDED.watched_seconds),
              enrollment_id = COALESCE(EXCLUDED.enrollment_id, lesson_progress.enrollment_id),
              updated_at = now()
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, lessonId);
            if (enrollmentId != null) ps.setString(3, enrollmentId);
            else ps.setNull(3, Types.OTHER);
            ps.setInt(4, watchedSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Mark lesson completed error: " + e.getMessage());
        }
    }

    public Set<String> findCompletedLessonIds(String studentId, String courseId) {
        String sql = """
            SELECT lp.lesson_id FROM lesson_progress lp
            JOIN course_lessons cl ON cl.id = lp.lesson_id
            WHERE lp.student_id = ?::uuid AND cl.course_id = ?::uuid AND lp.completed = true
            """;
        Set<String> ids = new HashSet<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("lesson_id"));
            }
        } catch (SQLException e) {
            System.err.println("Completed lessons error: " + e.getMessage());
        }
        return ids;
    }

    public Optional<String> findEnrollmentId(String studentId, String courseId) {
        String sql = "SELECT id FROM enrollments WHERE student_id=?::uuid AND course_id=?::uuid AND status='active'";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString("id"));
            }
        } catch (SQLException e) {
            System.err.println("Enrollment lookup error: " + e.getMessage());
        }
        return Optional.empty();
    }
}
