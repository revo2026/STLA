package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.mappers.ResultSetMapper;
import com.stla.domain.interfaces.StudentRepository;
import com.stla.domain.models.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentRepositoryImpl implements StudentRepository {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Optional<Student> findById(String id) {
        String sql = "SELECT * FROM students WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapStudent(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding student by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Student> findByProfileId(String profileId) {
        String sql = "SELECT * FROM students WHERE profile_id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(ResultSetMapper.mapStudent(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding student by profile id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Student> findAll() {
        String sql = "SELECT * FROM students ORDER BY created_at DESC";
        List<Student> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(ResultSetMapper.mapStudent(rs));
        } catch (SQLException e) {
            System.err.println("Error finding all students: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void save(Student student) {
        String sql = "INSERT INTO students (id, profile_id, headline, interests, learning_goals) VALUES (?::uuid, ?::uuid, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getId());
            ps.setString(2, student.getProfileId());
            ps.setString(3, student.getHeadline());
            ps.setArray(4, conn.createArrayOf("text", student.getInterests() != null ? student.getInterests() : new String[0]));
            ps.setString(5, student.getLearningGoals());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving student: " + e.getMessage());
        }
    }

    @Override
    public void update(Student student) {
        String sql = "UPDATE students SET headline = ?, interests = ?, learning_goals = ?, skill_level = ?, preferred_language = ?, daily_goal_minutes = ? WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getHeadline());
            ps.setArray(2, conn.createArrayOf("text", student.getInterests() != null ? student.getInterests() : new String[0]));
            ps.setString(3, student.getLearningGoals());
            ps.setString(4, student.getSkillLevel() != null ? student.getSkillLevel() : "beginner");
            ps.setString(5, student.getPreferredLanguage() != null ? student.getPreferredLanguage() : "en");
            if (student.getDailyGoalMinutes() != null) ps.setInt(6, student.getDailyGoalMinutes());
            else ps.setInt(6, 30);
            ps.setString(7, student.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating student: " + e.getMessage());
        }
    }

    @Override
    public int countAll() {
        String sql = "SELECT count(*) FROM students";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting students: " + e.getMessage());
        }
        return 0;
    }
}
