package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.mappers.ResultSetMapper;
import com.stla.domain.interfaces.ProfileRepository;
import com.stla.domain.models.Profile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ProfileRepository.
 * All SQL operations use PreparedStatement for security.
 */
public class ProfileRepositoryImpl implements ProfileRepository {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Optional<Profile> findById(String id) {
        String sql = "SELECT * FROM profiles WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapProfile(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding profile by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Profile> findByEmail(String email) {
        String sql = "SELECT * FROM profiles WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapProfile(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding profile by email: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Profile> findAll() {
        String sql = "SELECT * FROM profiles ORDER BY created_at DESC";
        List<Profile> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(ResultSetMapper.mapProfile(rs));
        } catch (SQLException e) {
            System.err.println("Error finding all profiles: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Profile> findByRole(String role) {
        String sql = "SELECT * FROM profiles WHERE role::text = ? ORDER BY created_at DESC";
        List<Profile> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(ResultSetMapper.mapProfile(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding profiles by role: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void save(Profile profile) {
        String sql = """
            INSERT INTO profiles (id, role, full_name, email, password_hash, phone, avatar_url, bio, country, timezone, is_active)
            VALUES (?::uuid, ?::app_role, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getId());
            ps.setString(2, profile.getRole().getValue());
            ps.setString(3, profile.getFullName());
            ps.setString(4, profile.getEmail());
            ps.setString(5, profile.getPasswordHash());
            ps.setString(6, profile.getPhone());
            ps.setString(7, profile.getAvatarUrl());
            ps.setString(8, profile.getBio());
            ps.setString(9, profile.getCountry());
            ps.setString(10, profile.getTimezone());
            ps.setBoolean(11, profile.isActive());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving profile: " + e.getMessage());
        }
    }

    @Override
    public void update(Profile profile) {
        String sql = """
            UPDATE profiles SET full_name = ?, phone = ?, avatar_url = ?, bio = ?, country = ?, timezone = ?
            WHERE id = ?::uuid
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getFullName());
            ps.setString(2, profile.getPhone());
            ps.setString(3, profile.getAvatarUrl());
            ps.setString(4, profile.getBio());
            ps.setString(5, profile.getCountry());
            ps.setString(6, profile.getTimezone());
            ps.setString(7, profile.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating profile: " + e.getMessage());
        }
    }

    @Override
    public void updateAvatarUrl(String profileId, String avatarUrl) {
        String sql = "UPDATE profiles SET avatar_url = ?, updated_at = now() WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, avatarUrl);
            ps.setString(2, profileId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating avatar URL: " + e.getMessage());
        }
    }

    @Override
    public void updateLastLogin(String id) {
        String sql = "UPDATE profiles SET last_login_at = now() WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }

    @Override
    public void deactivate(String id) {
        String sql = "UPDATE profiles SET is_active = false WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deactivating profile: " + e.getMessage());
        }
    }

    @Override
    public void activate(String id) {
        String sql = "UPDATE profiles SET is_active = true WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error activating profile: " + e.getMessage());
        }
    }

    @Override
    public int countAll() {
        String sql = "SELECT count(*) FROM profiles";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting profiles: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public int countByRole(String role) {
        String sql = "SELECT count(*) FROM profiles WHERE role::text = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting profiles by role: " + e.getMessage());
        }
        return 0;
    }
}
