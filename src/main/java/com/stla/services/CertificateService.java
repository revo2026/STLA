package com.stla.services;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.repositories.CourseRepositoryImpl;
import com.stla.domain.models.Course;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues course completion certificates when eligible.
 */
public class CertificateService {

    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();

    public Optional<String> issueCertificateIfEligible(String studentId, String courseId, String enrollmentId) {
        Optional<Course> courseOpt = courseRepo.findById(courseId);
        if (courseOpt.isEmpty() || !courseOpt.get().isHasCertificate()) {
            return Optional.empty();
        }

        if (alreadyIssued(studentId, courseId)) {
            return Optional.empty();
        }

        Optional<String> certificateTemplateId = findCertificateTemplate(courseId);
        if (certificateTemplateId.isEmpty()) {
            return Optional.empty();
        }

        String certNo = "STLA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = """
            INSERT INTO issued_certificates (certificate_id, student_id, course_id, enrollment_id, certificate_no)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, ?)
            RETURNING id
            """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, certificateTemplateId.get());
            ps.setString(2, studentId);
            ps.setString(3, courseId);
            if (enrollmentId != null) ps.setString(4, enrollmentId);
            else ps.setNull(4, Types.OTHER);
            ps.setString(5, certNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String issuedId = rs.getString("id");
                    EventBus.getInstance().publish(new AppEvent(
                            AppEvent.EventType.CERTIFICATE_ISSUED,
                            studentId, courseId,
                            "Your certificate for \"" + courseOpt.get().getTitle() + "\" is ready."));
                    return Optional.of(issuedId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Certificate issue error: " + e.getMessage());
        }
        return Optional.empty();
    }

    private boolean alreadyIssued(String studentId, String courseId) {
        String sql = "SELECT 1 FROM issued_certificates WHERE student_id=?::uuid AND course_id=?::uuid LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private Optional<String> findCertificateTemplate(String courseId) {
        String sql = "SELECT id FROM certificates WHERE course_id=?::uuid LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString("id"));
            }
        } catch (SQLException e) {
            System.err.println("Certificate template lookup: " + e.getMessage());
        }
        return Optional.empty();
    }
}
