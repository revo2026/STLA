package com.stla.services;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.models.Review;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Student course reviews (ratings for instructor/course).
 */
public class ReviewService {

    public Optional<Review> findStudentReview(String studentId, String courseId) {
        String sql = """
            SELECT * FROM reviews
            WHERE student_id = ?::uuid AND course_id = ?::uuid
            """;
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapReview(rs));
            }
        } catch (SQLException e) {
            System.err.println("Find review error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean hasStudentReviewed(String studentId, String courseId) {
        return findStudentReview(studentId, courseId).isPresent();
    }

    public List<Review> findReviewsByStudent(String studentId) {
        String sql = """
            SELECT r.*, c.title AS course_title, p.full_name AS instructor_name
            FROM reviews r
            JOIN courses c ON c.id = r.course_id
            JOIN instructors i ON i.id = c.instructor_id
            JOIN profiles p ON p.id = i.profile_id
            WHERE r.student_id = ?::uuid
            ORDER BY r.created_at DESC
            """;
        List<Review> list = new ArrayList<>();
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review review = mapReview(rs);
                    try { review.setCourseTitle(rs.getString("course_title")); } catch (SQLException ignored) {}
                    try { review.setInstructorName(rs.getString("instructor_name")); } catch (SQLException ignored) {}
                    list.add(review);
                }
            }
        } catch (SQLException e) {
            System.err.println("List reviews error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Create or update the student's review and refresh course aggregate ratings.
     */
    public void submitReview(String studentId, String courseId, String enrollmentId,
                             int rating, String title, String comment) throws SQLException {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        String sql = """
            INSERT INTO reviews (course_id, student_id, enrollment_id, rating, title, comment, is_published)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?, true)
            ON CONFLICT (course_id, student_id) DO UPDATE SET
              rating = EXCLUDED.rating,
              title = EXCLUDED.title,
              comment = EXCLUDED.comment,
              enrollment_id = COALESCE(EXCLUDED.enrollment_id, reviews.enrollment_id),
              updated_at = now()
            """;
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, courseId);
            ps.setString(2, studentId);
            if (enrollmentId != null) ps.setString(3, enrollmentId);
            else ps.setNull(3, java.sql.Types.OTHER);
            ps.setInt(4, rating);
            ps.setString(5, title != null && !title.isBlank() ? title.trim() : null);
            ps.setString(6, comment != null && !comment.isBlank() ? comment.trim() : null);
            ps.executeUpdate();
        }
        refreshCourseRatings(courseId);
        notifyInstructor(courseId, rating, comment);
    }

    private void refreshCourseRatings(String courseId) throws SQLException {
        String sql = """
            UPDATE courses c SET
              rating_avg = sub.avg_rating,
              rating_count = sub.cnt,
              updated_at = now()
            FROM (
              SELECT COALESCE(AVG(rating)::numeric, 0) AS avg_rating,
                     COUNT(*)::int AS cnt
              FROM reviews
              WHERE course_id = ?::uuid AND is_published = true
            ) sub
            WHERE c.id = ?::uuid
            """;
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, courseId);
            ps.setString(2, courseId);
            ps.executeUpdate();
        }
    }

    private void notifyInstructor(String courseId, int rating, String comment) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT instructor_id, title FROM courses WHERE id = ?::uuid")) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String instructorId = rs.getString("instructor_id");
                    String title = rs.getString("title");
                    String msg = "New " + rating + "-star review on \"" + title + "\""
                            + (comment != null && !comment.isBlank() ? ": " + truncate(comment, 80) : "");
                    EventBus.getInstance().publish(new AppEvent(
                            AppEvent.EventType.STUDENT_REVIEW_ADDED,
                            instructorId, courseId, msg));
                }
            }
        } catch (Exception e) {
            System.err.println("Review notify error: " + e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private Review mapReview(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getString("id"));
        r.setCourseId(rs.getString("course_id"));
        r.setStudentId(rs.getString("student_id"));
        r.setEnrollmentId(rs.getString("enrollment_id"));
        r.setRating(rs.getInt("rating"));
        r.setTitle(rs.getString("title"));
        r.setComment(rs.getString("comment"));
        r.setPublished(rs.getBoolean("is_published"));
        if (rs.getTimestamp("created_at") != null) {
            r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        return r;
    }

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }
}
