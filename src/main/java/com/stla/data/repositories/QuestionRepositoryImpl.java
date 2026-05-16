package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.models.QuestionAcceptedAnswer;
import com.stla.domain.models.QuestionMatchPair;
import com.stla.domain.models.QuestionSequenceItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for advanced question sub-entities:
 * accepted answers, match pairs, sequence items.
 * Also handles question duplication and reordering.
 */
public class QuestionRepositoryImpl {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ==================== ACCEPTED ANSWERS ====================

    public List<QuestionAcceptedAnswer> findAcceptedAnswers(String questionId) {
        String sql = "SELECT * FROM question_accepted_answers WHERE question_id = ?::uuid ORDER BY created_at";
        List<QuestionAcceptedAnswer> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuestionAcceptedAnswer a = new QuestionAcceptedAnswer();
                    a.setId(rs.getString("id"));
                    a.setQuestionId(rs.getString("question_id"));
                    a.setAnswerText(rs.getString("answer_text"));
                    list.add(a);
                }
            }
        } catch (SQLException e) { System.err.println("Error finding accepted answers: " + e.getMessage()); }
        return list;
    }

    public String saveAcceptedAnswer(QuestionAcceptedAnswer a) {
        String sql = "INSERT INTO question_accepted_answers (question_id, answer_text) VALUES (?::uuid, ?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getQuestionId());
            ps.setString(2, a.getAnswerText());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) { System.err.println("Error saving accepted answer: " + e.getMessage()); }
        return null;
    }

    public void deleteAcceptedAnswersByQuestion(String questionId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM question_accepted_answers WHERE question_id = ?::uuid")) {
            ps.setString(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting accepted answers: " + e.getMessage()); }
    }

    // ==================== MATCH PAIRS ====================

    public List<QuestionMatchPair> findMatchPairs(String questionId) {
        String sql = "SELECT * FROM question_match_pairs WHERE question_id = ?::uuid ORDER BY pair_order";
        List<QuestionMatchPair> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuestionMatchPair p = new QuestionMatchPair();
                    p.setId(rs.getString("id"));
                    p.setQuestionId(rs.getString("question_id"));
                    p.setLeftItem(rs.getString("left_item"));
                    p.setRightItem(rs.getString("right_item"));
                    p.setPairOrder(rs.getInt("pair_order"));
                    list.add(p);
                }
            }
        } catch (SQLException e) { System.err.println("Error finding match pairs: " + e.getMessage()); }
        return list;
    }

    public String saveMatchPair(QuestionMatchPair p) {
        String sql = "INSERT INTO question_match_pairs (question_id, left_item, right_item, pair_order) VALUES (?::uuid, ?, ?, ?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getQuestionId());
            ps.setString(2, p.getLeftItem());
            ps.setString(3, p.getRightItem());
            ps.setInt(4, p.getPairOrder());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) { System.err.println("Error saving match pair: " + e.getMessage()); }
        return null;
    }

    public void deleteMatchPairsByQuestion(String questionId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM question_match_pairs WHERE question_id = ?::uuid")) {
            ps.setString(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting match pairs: " + e.getMessage()); }
    }

    // ==================== SEQUENCE ITEMS ====================

    public List<QuestionSequenceItem> findSequenceItems(String questionId) {
        String sql = "SELECT * FROM question_sequence_items WHERE question_id = ?::uuid ORDER BY correct_position";
        List<QuestionSequenceItem> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuestionSequenceItem s = new QuestionSequenceItem();
                    s.setId(rs.getString("id"));
                    s.setQuestionId(rs.getString("question_id"));
                    s.setItemText(rs.getString("item_text"));
                    s.setCorrectPosition(rs.getInt("correct_position"));
                    list.add(s);
                }
            }
        } catch (SQLException e) { System.err.println("Error finding sequence items: " + e.getMessage()); }
        return list;
    }

    public String saveSequenceItem(QuestionSequenceItem s) {
        String sql = "INSERT INTO question_sequence_items (question_id, item_text, correct_position) VALUES (?::uuid, ?, ?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getQuestionId());
            ps.setString(2, s.getItemText());
            ps.setInt(3, s.getCorrectPosition());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) { System.err.println("Error saving sequence item: " + e.getMessage()); }
        return null;
    }

    public void deleteSequenceItemsByQuestion(String questionId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM question_sequence_items WHERE question_id = ?::uuid")) {
            ps.setString(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting sequence items: " + e.getMessage()); }
    }

    // ==================== QUESTION REORDERING ====================

    public void reorderQuestions(List<String> questionIds) {
        String sql = "UPDATE quiz_questions SET question_order = ? WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < questionIds.size(); i++) {
                ps.setInt(1, i + 1);
                ps.setString(2, questionIds.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) { System.err.println("Error reordering questions: " + e.getMessage()); }
    }

    // ==================== QUESTION IMAGE ====================

    public void updateQuestionImageUrl(String questionId, String imageUrl) {
        String sql = "UPDATE quiz_questions SET question_image_url = ? WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (imageUrl != null) ps.setString(1, imageUrl);
            else ps.setNull(1, Types.VARCHAR);
            ps.setString(2, questionId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating question image: " + e.getMessage()); }
    }
}
