package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.mappers.ResultSetMapper;
import com.stla.domain.interfaces.CourseRepository;
import com.stla.domain.models.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseRepositoryImpl implements CourseRepository {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Optional<Course> findById(String id) {
        String sql = "SELECT * FROM courses WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapCourse(rs));
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return Optional.empty();
    }

    private static final String LIVE_ENROLLMENT = """
            COALESCE((
              SELECT COUNT(*)::int FROM enrollments e
              WHERE e.course_id = c.id AND e.status IN ('active', 'completed')
            ), 0) AS live_enrollment_count
            """;

    @Override public List<Course> findAll() {
        return queryList("SELECT c.*, " + LIVE_ENROLLMENT + " FROM courses c ORDER BY c.created_at DESC");
    }
    private static final String APPROVED_CATALOG_SQL = """
            SELECT c.*, p.full_name AS instructor_name, cat.name AS category_name
            FROM courses c
            JOIN instructors i ON i.id = c.instructor_id
            JOIN profiles p ON p.id = i.profile_id
            LEFT JOIN categories cat ON cat.id = c.category_id
            WHERE c.status = 'approved' AND c.is_archived = false
            ORDER BY c.created_at DESC
            """;

    private static final String FEATURED_CATALOG_SQL = """
            SELECT c.*, p.full_name AS instructor_name, cat.name AS category_name
            FROM courses c
            JOIN instructors i ON i.id = c.instructor_id
            JOIN profiles p ON p.id = i.profile_id
            LEFT JOIN categories cat ON cat.id = c.category_id
            WHERE c.is_featured = true AND c.status = 'approved'
            ORDER BY c.created_at DESC
            """;

    @Override public List<Course> findApproved() { return queryCatalogList(APPROVED_CATALOG_SQL); }
    @Override public List<Course> findFeatured() { return queryCatalogList(FEATURED_CATALOG_SQL); }

    @Override
    public List<Course> findByInstructorId(String instructorId) {
        String sql = "SELECT c.*, " + LIVE_ENROLLMENT + " FROM courses c WHERE c.instructor_id = ?::uuid ORDER BY c.created_at DESC";
        List<Course> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(ResultSetMapper.mapCourse(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public List<Course> findByCategory(String categoryId) {
        String sql = "SELECT * FROM courses WHERE category_id = ?::uuid AND status = 'approved' ORDER BY created_at DESC";
        List<Course> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(ResultSetMapper.mapCourse(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public List<Course> findByStatus(String status) {
        String sql = "SELECT c.*, " + LIVE_ENROLLMENT + " FROM courses c WHERE c.status = ?::course_status ORDER BY c.created_at DESC";
        List<Course> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(ResultSetMapper.mapCourse(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public List<Course> searchByTitle(String keyword) {
        String sql = "SELECT * FROM courses WHERE (title ILIKE ? OR subtitle ILIKE ? OR description ILIKE ?) AND status = 'approved' ORDER BY created_at DESC";
        List<Course> list = new ArrayList<>();
        String pattern = "%" + keyword + "%";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern); ps.setString(2, pattern); ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(ResultSetMapper.mapCourse(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public void save(Course course) {
        String sql = """
            INSERT INTO courses (instructor_id, category_id, title, slug, subtitle, description,
            thumbnail_url, intro_video_url, language, level, price, estimated_hours, status,
            what_you_will_learn, requirements, target_audience,
            has_certificate, has_quiz, has_mentor_support, has_resources)
            VALUES (?::uuid,?::uuid,?,?,?,?,?,?,?,?::course_level,?,?,?::course_status,?,?,?,?,?,?,?)
            RETURNING id
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course.getInstructorId());
            ps.setString(2, course.getCategoryId());
            ps.setString(3, course.getTitle());
            ps.setString(4, course.getSlug());
            ps.setString(5, course.getSubtitle());
            ps.setString(6, course.getDescription());
            ps.setString(7, course.getThumbnailUrl());
            ps.setString(8, course.getIntroVideoUrl());
            ps.setString(9, course.getLanguage());
            ps.setString(10, course.getLevel().getValue());
            ps.setBigDecimal(11, course.getPrice());
            if (course.getEstimatedHours() != null) ps.setInt(12, course.getEstimatedHours());
            else ps.setNull(12, java.sql.Types.INTEGER);
            ps.setString(13, course.getStatus().getValue());
            ps.setString(14, course.getWhatYouWillLearn());
            ps.setString(15, course.getRequirements());
            ps.setString(16, course.getTargetAudience());
            ps.setBoolean(17, course.isHasCertificate());
            ps.setBoolean(18, course.isHasQuiz());
            ps.setBoolean(19, course.isHasMentorSupport());
            ps.setBoolean(20, course.isHasResources());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) course.setId(rs.getString("id"));
            }
        } catch (SQLException e) { System.err.println("Error saving course: " + e.getMessage()); }
    }

    @Override
    public void update(Course course) {
        String sql = """
            UPDATE courses SET title=?, subtitle=?, description=?, thumbnail_url=?, intro_video_url=?,
            level=?::course_level, price=?, category_id=?::uuid, language=?, estimated_hours=?,
            what_you_will_learn=?, requirements=?, target_audience=?,
            has_certificate=?, has_quiz=?, has_mentor_support=?, has_resources=?, updated_at=now()
            WHERE id=?::uuid
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course.getTitle());
            ps.setString(2, course.getSubtitle());
            ps.setString(3, course.getDescription());
            ps.setString(4, course.getThumbnailUrl());
            ps.setString(5, course.getIntroVideoUrl());
            ps.setString(6, course.getLevel().getValue());
            ps.setBigDecimal(7, course.getPrice());
            ps.setString(8, course.getCategoryId());
            ps.setString(9, course.getLanguage());
            if (course.getEstimatedHours() != null) ps.setInt(10, course.getEstimatedHours());
            else ps.setNull(10, java.sql.Types.INTEGER);
            ps.setString(11, course.getWhatYouWillLearn());
            ps.setString(12, course.getRequirements());
            ps.setString(13, course.getTargetAudience());
            ps.setBoolean(14, course.isHasCertificate());
            ps.setBoolean(15, course.isHasQuiz());
            ps.setBoolean(16, course.isHasMentorSupport());
            ps.setBoolean(17, course.isHasResources());
            ps.setString(18, course.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating course: " + e.getMessage()); }
    }

    @Override
    public void updateStatus(String id, String status, String approvalNote, String adminId) {
        String sql = "UPDATE courses SET status=?::course_status, approval_note=?, approved_by_admin_id=?::uuid, approved_at=now() WHERE id=?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status); ps.setString(2, approvalNote);
            ps.setString(3, adminId); ps.setString(4, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating course status: " + e.getMessage()); }
    }

    @Override public int countAll() { return countQuery("SELECT count(*) FROM courses"); }
    @Override public int countByStatus(String status) {
        String sql = "SELECT count(*) FROM courses WHERE status = ?::course_status";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return 0;
    }

    /** Approved and rejected courses only (draft/pending belong in Course Reviews). */
    public List<Course> findForAdminManagement() {
        String sql = """
            SELECT c.*, %s
            FROM courses c
            WHERE c.status NOT IN ('draft'::course_status, 'pending'::course_status)
            ORDER BY c.created_at DESC
            """.formatted(LIVE_ENROLLMENT);
        return queryList(sql);
    }

    /** Find pending courses with instructor name for admin review */
    public List<Course> findPendingReview() {
        String sql = """
            SELECT c.*, p.full_name as instructor_name, cat.name as category_name
            FROM courses c
            JOIN instructors i ON i.id = c.instructor_id
            JOIN profiles p ON p.id = i.profile_id
            LEFT JOIN categories cat ON cat.id = c.category_id
            WHERE c.status = 'pending'
            ORDER BY c.created_at ASC
            """;
        List<Course> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Course c = ResultSetMapper.mapCourse(rs);
                try { c.setInstructorName(rs.getString("instructor_name")); } catch (SQLException ignored) {}
                try { c.setCategoryName(rs.getString("category_name")); } catch (SQLException ignored) {}
                list.add(c);
            }
        } catch (SQLException e) { System.err.println("Error finding pending courses: " + e.getMessage()); }
        return list;
    }

    public void updateRejectionReason(String courseId, String reason) {
        String sql = "UPDATE courses SET rejection_reason = ?, updated_at = now() WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setString(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating rejection: " + e.getMessage()); }
    }

    private List<Course> queryList(String sql) {
        List<Course> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(ResultSetMapper.mapCourse(rs));
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    private List<Course> queryCatalogList(String sql) {
        List<Course> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Course c = ResultSetMapper.mapCourse(rs);
                try { c.setInstructorName(rs.getString("instructor_name")); } catch (SQLException ignored) {}
                try { c.setCategoryName(rs.getString("category_name")); } catch (SQLException ignored) {}
                list.add(c);
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    private int countQuery(String sql) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return 0;
    }
}
