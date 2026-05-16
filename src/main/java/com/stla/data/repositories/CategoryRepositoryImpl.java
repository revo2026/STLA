package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.mappers.ResultSetMapper;
import com.stla.domain.interfaces.CategoryRepository;
import com.stla.domain.models.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepositoryImpl implements CategoryRepository {
    private Connection getConnection() throws SQLException { return DatabaseConnection.getInstance().getConnection(); }

    @Override
    public Optional<Category> findById(String id) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM categories WHERE id=?::uuid")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(ResultSetMapper.mapCategory(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return Optional.empty();
    }

    @Override
    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM categories ORDER BY name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(ResultSetMapper.mapCategory(rs));
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public List<Category> findActive() {
        List<Category> list = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM categories WHERE is_active=true ORDER BY name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(ResultSetMapper.mapCategory(rs));
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    @Override
    public void save(Category cat) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO categories (name,slug,description,icon_name) VALUES (?,?,?,?)")) {
            ps.setString(1, cat.getName()); ps.setString(2, cat.getSlug()); ps.setString(3, cat.getDescription()); ps.setString(4, cat.getIconName());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    @Override
    public void update(Category cat) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE categories SET name=?,slug=?,description=?,icon_name=? WHERE id=?::uuid")) {
            ps.setString(1, cat.getName()); ps.setString(2, cat.getSlug()); ps.setString(3, cat.getDescription()); ps.setString(4, cat.getIconName()); ps.setString(5, cat.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    @Override
    public void deactivate(String id) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE categories SET is_active=false WHERE id=?::uuid")) {
            ps.setString(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }
}
