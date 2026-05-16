package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.mappers.ResultSetMapper;
import com.stla.domain.interfaces.NotificationRepository;
import com.stla.domain.models.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepositoryImpl implements NotificationRepository {
    private Connection getConnection() throws SQLException { return DatabaseConnection.getInstance().getConnection(); }

    @Override
    public List<Notification> findByRecipient(String profileId) {
        List<Notification> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM notifications WHERE recipient_profile_id=?::uuid ORDER BY created_at DESC LIMIT 50")) {
            ps.setString(1, profileId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(ResultSetMapper.mapNotification(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public List<Notification> findRecentByRecipient(String profileId, int limit) {
        List<Notification> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM notifications WHERE recipient_profile_id=?::uuid ORDER BY created_at DESC LIMIT ?")) {
            ps.setString(1, profileId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(ResultSetMapper.mapNotification(rs));
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public List<Notification> findUnreadByRecipient(String profileId) {
        List<Notification> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM notifications WHERE recipient_profile_id=?::uuid AND is_read=false ORDER BY created_at DESC")) {
            ps.setString(1, profileId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(ResultSetMapper.mapNotification(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public int countUnread(String profileId) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM notifications WHERE recipient_profile_id=?::uuid AND is_read=false")) {
            ps.setString(1, profileId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return 0;
    }

    @Override
    public void save(Notification n) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO notifications (recipient_profile_id,actor_profile_id,type,title,body,reference_table,reference_id) VALUES (?::uuid,?::uuid,?::notification_type,?,?,?,?::uuid)")) {
            ps.setString(1, n.getRecipientProfileId()); ps.setString(2, n.getActorProfileId());
            ps.setString(3, n.getType().getValue()); ps.setString(4, n.getTitle());
            ps.setString(5, n.getBody()); ps.setString(6, n.getReferenceTable()); ps.setString(7, n.getReferenceId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    @Override
    public void markAsRead(String id) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE notifications SET is_read=true, read_at=now() WHERE id=?::uuid")) {
            ps.setString(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    @Override
    public void markAllAsRead(String profileId) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE notifications SET is_read=true, read_at=now() WHERE recipient_profile_id=?::uuid AND is_read=false")) {
            ps.setString(1, profileId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }
}
