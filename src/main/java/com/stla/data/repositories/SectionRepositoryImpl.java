package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.models.CourseSection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for course_sections CRUD.
 */
public class SectionRepositoryImpl {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<CourseSection> findByCourseId(String courseId) {
        String sql = "SELECT * FROM course_sections WHERE course_id = ?::uuid ORDER BY section_order ASC";
        List<CourseSection> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSection(rs));
            }
        } catch (SQLException e) { System.err.println("Error finding sections: " + e.getMessage()); }
        return list;
    }

    public Optional<CourseSection> findById(String id) {
        String sql = "SELECT * FROM course_sections WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapSection(rs));
            }
        } catch (SQLException e) { System.err.println("Error finding section: " + e.getMessage()); }
        return Optional.empty();
    }

    public String save(CourseSection section) {
        String sql = "INSERT INTO course_sections (course_id, title, section_order) VALUES (?::uuid, ?, ?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, section.getCourseId());
            ps.setString(2, section.getTitle());
            ps.setInt(3, section.getSectionOrder());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        } catch (SQLException e) { System.err.println("Error saving section: " + e.getMessage()); }
        return null;
    }

    public void update(CourseSection section) {
        String sql = "UPDATE course_sections SET title = ?, section_order = ?, updated_at = now() WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, section.getTitle());
            ps.setInt(2, section.getSectionOrder());
            ps.setString(3, section.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating section: " + e.getMessage()); }
    }

    public void delete(String sectionId) {
        String sql = "DELETE FROM course_sections WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sectionId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting section: " + e.getMessage()); }
    }

    public void reorder(String sectionId, int newOrder) {
        String sql = "UPDATE course_sections SET section_order = ?, updated_at = now() WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newOrder);
            ps.setString(2, sectionId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error reordering section: " + e.getMessage()); }
    }

    public int countByCourse(String courseId) {
        String sql = "SELECT COUNT(*) FROM course_sections WHERE course_id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Error counting sections: " + e.getMessage()); }
        return 0;
    }

    private CourseSection mapSection(ResultSet rs) throws SQLException {
        CourseSection s = new CourseSection();
        s.setId(rs.getString("id"));
        s.setCourseId(rs.getString("course_id"));
        s.setTitle(rs.getString("title"));
        s.setSectionOrder(rs.getInt("section_order"));
        s.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        s.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return s;
    }
}
