package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.models.CourseLesson;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for course_lessons CRUD. Replaces inline SQL previously in CourseService.
 */
public class LessonRepositoryImpl {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<CourseLesson> findByCourseId(String courseId) {
        String sql = "SELECT * FROM course_lessons WHERE course_id = ?::uuid ORDER BY lesson_order ASC";
        List<CourseLesson> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapLesson(rs)); }
        } catch (SQLException e) { System.err.println("Error finding lessons: " + e.getMessage()); }
        return list;
    }

    public List<CourseLesson> findBySectionId(String sectionId) {
        String sql = "SELECT * FROM course_lessons WHERE section_id = ?::uuid ORDER BY lesson_order ASC";
        List<CourseLesson> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapLesson(rs)); }
        } catch (SQLException e) { System.err.println("Error finding lessons by section: " + e.getMessage()); }
        return list;
    }

    public Optional<CourseLesson> findById(String id) {
        String sql = "SELECT * FROM course_lessons WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(mapLesson(rs)); }
        } catch (SQLException e) { System.err.println("Error finding lesson: " + e.getMessage()); }
        return Optional.empty();
    }

    public String save(CourseLesson lesson) {
        String sql = """
            INSERT INTO course_lessons (course_id, section_id, title, description, lesson_order, video_url, duration_seconds, is_preview)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lesson.getCourseId());
            if (lesson.getSectionId() != null) ps.setString(2, lesson.getSectionId());
            else ps.setNull(2, Types.OTHER);
            ps.setString(3, lesson.getTitle());
            ps.setString(4, lesson.getDescription());
            ps.setInt(5, lesson.getLessonOrder());
            ps.setString(6, lesson.getVideoUrl());
            ps.setInt(7, lesson.getDurationSeconds());
            ps.setBoolean(8, lesson.isPreview());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) { System.err.println("Error saving lesson: " + e.getMessage()); }
        return null;
    }

    public void update(CourseLesson lesson) {
        String sql = """
            UPDATE course_lessons SET title = ?, description = ?, video_url = ?,
            duration_seconds = ?, is_preview = ?, lesson_order = ?, section_id = ?::uuid, updated_at = now()
            WHERE id = ?::uuid
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lesson.getTitle());
            ps.setString(2, lesson.getDescription());
            ps.setString(3, lesson.getVideoUrl());
            ps.setInt(4, lesson.getDurationSeconds());
            ps.setBoolean(5, lesson.isPreview());
            ps.setInt(6, lesson.getLessonOrder());
            if (lesson.getSectionId() != null) ps.setString(7, lesson.getSectionId());
            else ps.setNull(7, Types.OTHER);
            ps.setString(8, lesson.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating lesson: " + e.getMessage()); }
    }

    public void delete(String lessonId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM course_lessons WHERE id = ?::uuid")) {
            ps.setString(1, lessonId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting lesson: " + e.getMessage()); }
    }

    public void reorder(String lessonId, int newOrder) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE course_lessons SET lesson_order = ?, updated_at = now() WHERE id = ?::uuid")) {
            ps.setInt(1, newOrder);
            ps.setString(2, lessonId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error reordering lesson: " + e.getMessage()); }
    }

    /**
     * Batch reorder lessons using negative temp values to avoid unique constraint violation
     * on (course_id, lesson_order). Steps:
     * 1. Set all to negative (temp) values
     * 2. Set all to final positive values
     */
    public void batchReorder(java.util.List<String> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return;
        String sql = "UPDATE course_lessons SET lesson_order = ?, updated_at = now() WHERE id = ?::uuid";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // Step 1: Set negative temp values
                for (int i = 0; i < lessonIds.size(); i++) {
                    ps.setInt(1, -(i + 1));
                    ps.setString(2, lessonIds.get(i));
                    ps.addBatch();
                }
                ps.executeBatch();

                // Step 2: Set final positive values
                ps.clearBatch();
                for (int i = 0; i < lessonIds.size(); i++) {
                    ps.setInt(1, i + 1);
                    ps.setString(2, lessonIds.get(i));
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error batch reordering lessons: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) { System.err.println("Error in batch reorder: " + e.getMessage()); }
    }

    public int countByCourse(String courseId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM course_lessons WHERE course_id = ?::uuid")) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Error counting lessons: " + e.getMessage()); }
        return 0;
    }

    private CourseLesson mapLesson(ResultSet rs) throws SQLException {
        CourseLesson l = new CourseLesson();
        l.setId(rs.getString("id"));
        l.setCourseId(rs.getString("course_id"));
        try { l.setSectionId(rs.getString("section_id")); } catch (SQLException ignored) {}
        l.setTitle(rs.getString("title"));
        l.setDescription(rs.getString("description"));
        l.setLessonOrder(rs.getInt("lesson_order"));
        l.setVideoUrl(rs.getString("video_url"));
        l.setDurationSeconds(rs.getInt("duration_seconds"));
        l.setPreview(rs.getBoolean("is_preview"));
        l.setPublished(rs.getBoolean("is_published"));
        l.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        l.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return l;
    }
}
