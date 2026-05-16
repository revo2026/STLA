package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.mappers.ResultSetMapper;
import com.stla.domain.interfaces.InstructorRepository;
import com.stla.domain.models.Instructor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstructorRepositoryImpl implements InstructorRepository {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Optional<Instructor> findById(String id) {
        String sql = "SELECT * FROM instructors WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapInstructor(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding instructor by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Instructor> findByProfileId(String profileId) {
        String sql = "SELECT * FROM instructors WHERE profile_id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapInstructor(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding instructor by profile id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Instructor> findAll() {
        String sql = "SELECT * FROM instructors ORDER BY created_at DESC";
        List<Instructor> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(ResultSetMapper.mapInstructor(rs));
        } catch (SQLException e) {
            System.err.println("Error finding all instructors: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void save(Instructor instructor) {
        String sql = """
            INSERT INTO instructors (id, profile_id, title, expertise_tags, years_experience,
                                     instructor_bio, id_front_url, id_back_url,
                                     experience_certificate_url, cv_url, verification_status)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instructor.getId());
            ps.setString(2, instructor.getProfileId());
            ps.setString(3, instructor.getTitle());
            ps.setArray(4, conn.createArrayOf("text", instructor.getExpertiseTags() != null ? instructor.getExpertiseTags() : new String[0]));
            if (instructor.getYearsExperience() != null) ps.setInt(5, instructor.getYearsExperience());
            else ps.setNull(5, Types.INTEGER);
            ps.setString(6, instructor.getInstructorBio());
            ps.setString(7, instructor.getIdFrontUrl());
            ps.setString(8, instructor.getIdBackUrl());
            ps.setString(9, instructor.getExperienceCertificateUrl());
            ps.setString(10, instructor.getCvUrl());
            ps.setString(11, instructor.getVerificationStatus() != null ? instructor.getVerificationStatus() : "PENDING");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving instructor: " + e.getMessage());
        }
    }

    @Override
    public void update(Instructor instructor) {
        String sql = """
            UPDATE instructors SET title = ?, expertise_tags = ?, years_experience = ?,
                                   instructor_bio = ?
            WHERE id = ?::uuid
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instructor.getTitle());
            ps.setArray(2, conn.createArrayOf("text", instructor.getExpertiseTags() != null ? instructor.getExpertiseTags() : new String[0]));
            if (instructor.getYearsExperience() != null) ps.setInt(3, instructor.getYearsExperience());
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, instructor.getInstructorBio());
            ps.setString(5, instructor.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating instructor: " + e.getMessage());
        }
    }

    @Override
    public int countAll() {
        String sql = "SELECT count(*) FROM instructors";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting instructors: " + e.getMessage());
        }
        return 0;
    }

    // ==================== VERIFICATION ====================

    @Override
    public void updateVerificationDocuments(String instructorId, String idFrontUrl, String idBackUrl, String certUrl, String cvUrl) {
        String sql = """
            UPDATE instructors SET id_front_url = ?, id_back_url = ?,
                                   experience_certificate_url = ?, cv_url = ?,
                                   updated_at = now()
            WHERE id = ?::uuid
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idFrontUrl);
            ps.setString(2, idBackUrl);
            ps.setString(3, certUrl);
            ps.setString(4, cvUrl);
            ps.setString(5, instructorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating verification documents: " + e.getMessage());
        }
    }

    @Override
    public void updateVerificationStatus(String instructorId, String status, String rejectionReason) {
        String sql = "UPDATE instructors SET verification_status = ?, rejection_reason = ?, updated_at = now() WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, rejectionReason);
            ps.setString(3, instructorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating verification status: " + e.getMessage());
        }
    }

    @Override
    public List<Instructor> findPendingVerificationRequests() {
        String sql = """
            SELECT i.*, p.full_name, p.email, p.phone, p.avatar_url, p.bio, p.country
            FROM instructors i
            JOIN profiles p ON p.id = i.profile_id
            WHERE i.verification_status = 'PENDING'
            ORDER BY i.created_at ASC
            """;
        List<Instructor> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Instructor inst = ResultSetMapper.mapInstructor(rs);
                // Attach lightweight profile from joined columns
                com.stla.domain.models.Profile p = new com.stla.domain.models.Profile();
                p.setId(inst.getProfileId());
                p.setFullName(rs.getString("full_name"));
                p.setEmail(rs.getString("email"));
                p.setPhone(rs.getString("phone"));
                p.setAvatarUrl(rs.getString("avatar_url"));
                p.setBio(rs.getString("bio"));
                p.setCountry(rs.getString("country"));
                inst.setProfile(p);
                list.add(inst);
            }
        } catch (SQLException e) {
            System.err.println("Error finding pending verifications: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean isInstructorVerified(String instructorId) {
        String sql = "SELECT verification_status FROM instructors WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return "VERIFIED".equalsIgnoreCase(rs.getString("verification_status"));
            }
        } catch (SQLException e) {
            System.err.println("Error checking instructor verification: " + e.getMessage());
        }
        return false;
    }
}
