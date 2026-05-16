package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.models.LessonResource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for lesson_resources CRUD.
 * Handles both schema variants: stla.sql (file_url, file_size_bytes) and
 * migration (resource_url, file_size) by auto-detecting column names.
 */
public class ResourceRepositoryImpl {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // Detected column names (lazy-initialized)
    private volatile String colUrl = null;
    private volatile String colSize = null;
    private volatile String colType = null;

    private void detectColumns() {
        if (colUrl != null) return;
        try (Connection conn = getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, "public", "lesson_resources", null)) {
            boolean hasFileUrl = false, hasResourceUrl = false;
            boolean hasFileSize = false, hasFileSizeBytes = false;
            boolean hasResourceType = false;
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if ("file_url".equals(col)) hasFileUrl = true;
                if ("resource_url".equals(col)) hasResourceUrl = true;
                if ("file_size".equals(col)) hasFileSize = true;
                if ("file_size_bytes".equals(col)) hasFileSizeBytes = true;
                if ("resource_type".equals(col)) hasResourceType = true;
            }
            colUrl = hasResourceUrl ? "resource_url" : (hasFileUrl ? "file_url" : "file_url");
            colSize = hasFileSize ? "file_size" : (hasFileSizeBytes ? "file_size_bytes" : "file_size_bytes");
            colType = "resource_type"; // exists in both schemas
            System.out.println("[ResourceRepo] Detected columns: url=" + colUrl + ", size=" + colSize + ", type=" + colType);
        } catch (SQLException e) {
            // Fallback to stla.sql schema
            colUrl = "file_url";
            colSize = "file_size_bytes";
            colType = "resource_type";
            System.err.println("[ResourceRepo] Column detection failed, using defaults: " + e.getMessage());
        }
    }

    public List<LessonResource> findByLessonId(String lessonId) {
        detectColumns();
        String sql = "SELECT id, lesson_id, title, " + colUrl + ", " + colType + ", " + colSize + ", created_at FROM lesson_resources WHERE lesson_id = ?::uuid ORDER BY created_at ASC";
        List<LessonResource> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LessonResource r = new LessonResource();
                    r.setId(rs.getString("id"));
                    r.setLessonId(rs.getString("lesson_id"));
                    r.setTitle(rs.getString("title"));
                    r.setResourceUrl(rs.getString(colUrl));
                    try { r.setResourceType(rs.getString(colType)); } catch (SQLException ignored) {}
                    try { r.setFileSize(rs.getLong(colSize)); } catch (SQLException ignored) {}
                    r.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                    list.add(r);
                }
            }
        } catch (SQLException e) { System.err.println("Error finding resources: " + e.getMessage()); }
        return list;
    }

    public String save(LessonResource resource) {
        detectColumns();
        String sql = "INSERT INTO lesson_resources (lesson_id, title, " + colUrl + ", " + colType + ", " + colSize + ") VALUES (?::uuid, ?, ?, ?::" + getTypeCast() + ", ?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource.getLessonId());
            ps.setString(2, resource.getTitle());
            ps.setString(3, resource.getResourceUrl());
            ps.setString(4, mapResourceType(resource.getResourceType()));
            ps.setLong(5, resource.getFileSize());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) {
            System.err.println("Error saving resource: " + e.getMessage());
            // Retry without enum cast if it fails
            return saveWithTextCast(resource);
        }
        return null;
    }

    /** Fallback: insert with text type cast instead of enum */
    private String saveWithTextCast(LessonResource resource) {
        String sql = "INSERT INTO lesson_resources (lesson_id, title, " + colUrl + ", " + colType + ", " + colSize + ") VALUES (?::uuid, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource.getLessonId());
            ps.setString(2, resource.getTitle());
            ps.setString(3, resource.getResourceUrl());
            ps.setString(4, mapResourceType(resource.getResourceType()));
            ps.setLong(5, resource.getFileSize());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) { System.err.println("Error saving resource (retry): " + e.getMessage()); }
        return null;
    }

    public void delete(String resourceId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM lesson_resources WHERE id = ?::uuid")) {
            ps.setString(1, resourceId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting resource: " + e.getMessage()); }
    }

    /** Get type cast string — 'public.resource_type' for enum schema, 'text' for migration schema */
    private String getTypeCast() {
        // If using stla.sql schema (file_url), it has resource_type as enum
        return "file_url".equals(colUrl) ? "public.resource_type" : "text";
    }

    /** Map file extension to resource_type enum value */
    private String mapResourceType(String ext) {
        if (ext == null) return "other";
        return switch (ext.toLowerCase().replace(".", "")) {
            case "pdf" -> "pdf";
            case "doc", "docx" -> "doc";
            case "zip" -> "zip";
            case "png", "jpg", "jpeg", "webp" -> "image";
            case "link" -> "link";
            default -> "other";
        };
    }
}
