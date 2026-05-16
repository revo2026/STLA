package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.models.QuizAttempt;
import com.stla.domain.models.QuizResponse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists quiz attempts and answers (quiz_attempts / quiz_answers).
 */
public class QuizAttemptRepositoryImpl {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public String createAttempt(String quizId, String studentId, int attemptNumber,
                                double scorePercent, boolean passed, int earnedPoints, int totalPoints) {
        if (usesModernSchema()) {
            return insertModernAttempt(quizId, studentId, attemptNumber, scorePercent, passed);
        }
        return insertLegacyAttempt(quizId, studentId, earnedPoints, totalPoints, passed);
    }

    private String insertModernAttempt(String quizId, String studentId, int attemptNumber,
                                       double scorePercent, boolean passed) {
        String sql = """
            INSERT INTO quiz_attempts (quiz_id, student_id, attempt_number, score, is_passed, submitted_at, started_at)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, now(), now())
            RETURNING id
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quizId);
            ps.setString(2, studentId);
            ps.setInt(3, attemptNumber);
            ps.setDouble(4, scorePercent);
            ps.setBoolean(5, passed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        } catch (SQLException e) {
            System.err.println("Error creating quiz attempt: " + e.getMessage());
        }
        return null;
    }

    private String insertLegacyAttempt(String quizId, String studentId, int earned, int total, boolean passed) {
        String sql = """
            INSERT INTO quiz_attempts (quiz_id, student_id, score, total_points, passed, completed_at)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, now())
            RETURNING id
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quizId);
            ps.setString(2, studentId);
            ps.setInt(3, earned);
            ps.setInt(4, total);
            ps.setBoolean(5, passed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        } catch (SQLException e) {
            System.err.println("Error creating legacy attempt: " + e.getMessage());
        }
        return null;
    }

    public void saveAnswer(QuizResponse response) {
        if (usesModernSchema()) {
            saveModernAnswer(response);
        } else {
            saveLegacyResponse(response);
        }
    }

    private void saveModernAnswer(QuizResponse response) {
        String sql = """
            INSERT INTO quiz_answers (attempt_id, question_id, selected_option_ids, answer_text,
                is_correct, points_awarded, matching_answers, ordering_answer)
            VALUES (?::uuid, ?::uuid, ?::uuid[], ?, ?, ?, ?::jsonb, ?::uuid[])
            ON CONFLICT (attempt_id, question_id) DO UPDATE SET
                selected_option_ids = EXCLUDED.selected_option_ids,
                answer_text = EXCLUDED.answer_text,
                is_correct = EXCLUDED.is_correct,
                points_awarded = EXCLUDED.points_awarded,
                matching_answers = EXCLUDED.matching_answers,
                ordering_answer = EXCLUDED.ordering_answer,
                updated_at = now()
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, response.getAttemptId());
            ps.setString(2, response.getQuestionId());
            ps.setArray(3, conn.createArrayOf("uuid",
                    response.getSelectedOptionIds() != null ? response.getSelectedOptionIds() : new String[0]));
            ps.setString(4, response.getAnswerText());
            if (response.isCorrect()) ps.setBoolean(5, true);
            else ps.setObject(5, null);
            ps.setDouble(6, response.getPointsAwarded());
            if (response.getMatchingAnswers() != null && !response.getMatchingAnswers().isBlank()) {
                ps.setString(7, response.getMatchingAnswers());
            } else {
                ps.setNull(7, Types.OTHER);
            }
            String[] ordering = response.getOrderingAnswer();
            if (ordering != null && ordering.length > 0) {
                ps.setArray(8, conn.createArrayOf("uuid", ordering));
            } else {
                ps.setNull(8, Types.ARRAY);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            saveModernAnswerBasic(response);
        }
    }

    private void saveModernAnswerBasic(QuizResponse response) {
        String sql = """
            INSERT INTO quiz_answers (attempt_id, question_id, selected_option_ids, answer_text, is_correct, points_awarded)
            VALUES (?::uuid, ?::uuid, ?::uuid[], ?, ?, ?)
            ON CONFLICT (attempt_id, question_id) DO UPDATE SET
                selected_option_ids = EXCLUDED.selected_option_ids,
                answer_text = EXCLUDED.answer_text,
                is_correct = EXCLUDED.is_correct,
                points_awarded = EXCLUDED.points_awarded
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, response.getAttemptId());
            ps.setString(2, response.getQuestionId());
            ps.setArray(3, conn.createArrayOf("uuid",
                    response.getSelectedOptionIds() != null ? response.getSelectedOptionIds() : new String[0]));
            ps.setString(4, response.getAnswerText());
            if (response.isCorrect()) ps.setBoolean(5, true);
            else ps.setObject(5, null);
            ps.setDouble(6, response.getPointsAwarded());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving quiz answer: " + e.getMessage());
        }
    }

    private void saveLegacyResponse(QuizResponse response) {
        String sql = "INSERT INTO quiz_responses (attempt_id, question_id, selected_option_ids, is_correct) VALUES (?::uuid, ?::uuid, ?::uuid[], ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, response.getAttemptId());
            ps.setString(2, response.getQuestionId());
            ps.setArray(3, conn.createArrayOf("uuid",
                    response.getSelectedOptionIds() != null ? response.getSelectedOptionIds() : new String[0]));
            ps.setBoolean(4, response.isCorrect());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving quiz response: " + e.getMessage());
        }
    }

    public List<QuizAttempt> findByStudentAndQuiz(String studentId, String quizId) {
        String sql = usesModernSchema()
                ? "SELECT * FROM quiz_attempts WHERE student_id = ?::uuid AND quiz_id = ?::uuid ORDER BY COALESCE(submitted_at, started_at) DESC"
                : "SELECT * FROM quiz_attempts WHERE student_id = ?::uuid AND quiz_id = ?::uuid ORDER BY started_at DESC";
        List<QuizAttempt> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttempt(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding attempts: " + e.getMessage());
        }
        return list;
    }

    public int countAttempts(String studentId, String quizId) {
        String sql = "SELECT COUNT(*) FROM quiz_attempts WHERE student_id = ?::uuid AND quiz_id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting attempts: " + e.getMessage());
        }
        return 0;
    }

    public double findBestScorePercent(String studentId, String quizId) {
        if (usesModernSchema()) {
            String sql = "SELECT COALESCE(MAX(score), 0) FROM quiz_attempts WHERE student_id = ?::uuid AND quiz_id = ?::uuid";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, studentId);
                ps.setString(2, quizId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble(1);
                }
            } catch (SQLException e) {
                System.err.println("Best score error: " + e.getMessage());
            }
            return 0;
        }
        List<QuizAttempt> attempts = findByStudentAndQuiz(studentId, quizId);
        return attempts.stream().mapToDouble(QuizAttempt::getScorePercent).max().orElse(0);
    }

    public boolean hasPassedAttempt(String studentId, String quizId) {
        if (usesModernSchema()) {
            String sql = "SELECT 1 FROM quiz_attempts WHERE student_id = ?::uuid AND quiz_id = ?::uuid AND is_passed = true LIMIT 1";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, studentId);
                ps.setString(2, quizId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            } catch (SQLException e) {
                return false;
            }
        }
        return findByStudentAndQuiz(studentId, quizId).stream().anyMatch(QuizAttempt::isPassed);
    }

    private QuizAttempt mapAttempt(ResultSet rs) throws SQLException {
        QuizAttempt a = new QuizAttempt();
        a.setId(rs.getString("id"));
        a.setQuizId(rs.getString("quiz_id"));
        a.setStudentId(rs.getString("student_id"));
        try { a.setAttemptNumber(rs.getInt("attempt_number")); } catch (SQLException ignored) {}
        try {
            a.setScorePercent(rs.getDouble("score"));
            a.setPassed(rs.getBoolean("is_passed"));
            a.setSubmittedAt(rs.getTimestamp("submitted_at") != null
                    ? rs.getTimestamp("submitted_at").toLocalDateTime() : null);
            a.setStartedAt(rs.getTimestamp("started_at") != null
                    ? rs.getTimestamp("started_at").toLocalDateTime() : null);
        } catch (SQLException e) {
            a.setScore(rs.getInt("score"));
            try { a.setTotalPoints(rs.getInt("total_points")); } catch (SQLException ex) { a.setTotalPoints(0); }
            a.setPassed(rs.getBoolean("passed"));
            a.setCompletedAt(rs.getTimestamp("completed_at") != null
                    ? rs.getTimestamp("completed_at").toLocalDateTime() : null);
            a.setStartedAt(rs.getTimestamp("started_at") != null
                    ? rs.getTimestamp("started_at").toLocalDateTime() : null);
        }
        return a;
    }

    private Boolean modernSchema;

    private boolean usesModernSchema() {
        if (modernSchema != null) return modernSchema;
        String sql = """
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'quiz_attempts' AND column_name = 'is_passed'
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            modernSchema = rs.next();
        } catch (SQLException e) {
            modernSchema = false;
        }
        return modernSchema;
    }
}
