package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.mappers.ResultSetMapper;
import com.stla.domain.interfaces.AdminRepository;
import com.stla.domain.models.Admin;

import java.sql.*;
import java.util.Optional;

public class AdminRepositoryImpl implements AdminRepository {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Optional<Admin> findById(String id) {
        String sql = "SELECT * FROM admins WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapAdmin(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding admin by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Admin> findByProfileId(String profileId) {
        String sql = "SELECT * FROM admins WHERE profile_id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapAdmin(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding admin by profile id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void save(Admin admin) {
        String sql = "INSERT INTO admins (id, profile_id, admin_level, permissions) VALUES (?::uuid, ?::uuid, ?, ?::jsonb)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, admin.getId());
            ps.setString(2, admin.getProfileId());
            ps.setInt(3, admin.getAdminLevel());
            ps.setString(4, admin.getPermissions() != null ? admin.getPermissions() : "{}");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving admin: " + e.getMessage());
        }
    }

    @Override
    public void update(Admin admin) {
        String sql = "UPDATE admins SET admin_level = ?, permissions = ?::jsonb WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, admin.getAdminLevel());
            ps.setString(2, admin.getPermissions() != null ? admin.getPermissions() : "{}");
            ps.setString(3, admin.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating admin: " + e.getMessage());
        }
    }
}
