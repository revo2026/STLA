package com.stla.data.repositories;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.models.Quiz;
import com.stla.domain.models.QuizOption;
import com.stla.domain.models.QuizQuestion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for quizzes, quiz_questions, and quiz_options CRUD.
 * Extended to support all 9 question types with new columns.
 */
public class QuizRepositoryImpl {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ==================== QUIZ ====================

    public List<Quiz> findPublishedByCourseId(String courseId) {
        String sql = "SELECT * FROM quizzes WHERE course_id = ?::uuid AND is_published = true ORDER BY created_at ASC";
        List<Quiz> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapQuiz(rs)); }
        } catch (SQLException e) { System.err.println("Error finding published quizzes: " + e.getMessage()); }
        return list;
    }

    public int countPublishedByCourseId(String courseId) {
        String sql = "SELECT COUNT(*) FROM quizzes WHERE course_id = ?::uuid AND is_published = true";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Error counting quizzes: " + e.getMessage()); }
        return 0;
    }

    public List<Quiz> findByCourseId(String courseId) {
        String sql = "SELECT * FROM quizzes WHERE course_id = ?::uuid ORDER BY created_at ASC";
        List<Quiz> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapQuiz(rs)); }
        } catch (SQLException e) { System.err.println("Error finding quizzes: " + e.getMessage()); }
        return list;
    }

    public Optional<Quiz> findById(String quizId) {
        String sql = "SELECT * FROM quizzes WHERE id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quizId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(mapQuiz(rs)); }
        } catch (SQLException e) { System.err.println("Error finding quiz: " + e.getMessage()); }
        return Optional.empty();
    }

    public Optional<Quiz> findByLessonId(String lessonId) {
        String sql = "SELECT * FROM quizzes WHERE lesson_id = ?::uuid LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(mapQuiz(rs)); }
        } catch (SQLException e) { System.err.println("Error finding quiz by lesson: " + e.getMessage()); }
        return Optional.empty();
    }

    public String saveQuiz(Quiz quiz) {
        String sql = "INSERT INTO quizzes (course_id, lesson_id, title, description, time_limit_minutes, passing_score, attempts_allowed, shuffle_questions, show_answers_after_submit, is_published) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quiz.getCourseId());
            if (quiz.getLessonId() != null) ps.setString(2, quiz.getLessonId());
            else ps.setNull(2, Types.OTHER);
            ps.setString(3, quiz.getTitle());
            ps.setString(4, quiz.getDescription());
            if (quiz.getTimeLimitMinutes() != null) ps.setInt(5, quiz.getTimeLimitMinutes());
            else ps.setNull(5, Types.INTEGER);
            ps.setInt(6, quiz.getPassingScore());
            ps.setInt(7, quiz.getAttemptsAllowed());
            ps.setBoolean(8, quiz.isShuffleQuestions());
            ps.setBoolean(9, quiz.isShowAnswersAfterSubmit());
            ps.setBoolean(10, quiz.isPublished());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) { System.err.println("Error saving quiz: " + e.getMessage()); }
        return null;
    }

    public void updateQuiz(Quiz quiz) {
        String sql = "UPDATE quizzes SET title=?, description=?, time_limit_minutes=?, passing_score=?, attempts_allowed=?, shuffle_questions=?, show_answers_after_submit=?, is_published=?, updated_at=now() WHERE id=?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quiz.getTitle());
            ps.setString(2, quiz.getDescription());
            if (quiz.getTimeLimitMinutes() != null) ps.setInt(3, quiz.getTimeLimitMinutes());
            else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, quiz.getPassingScore());
            ps.setInt(5, quiz.getAttemptsAllowed());
            ps.setBoolean(6, quiz.isShuffleQuestions());
            ps.setBoolean(7, quiz.isShowAnswersAfterSubmit());
            ps.setBoolean(8, quiz.isPublished());
            ps.setString(9, quiz.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating quiz: " + e.getMessage()); }
    }

    public void deleteQuiz(String quizId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM quizzes WHERE id = ?::uuid")) {
            ps.setString(1, quizId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting quiz: " + e.getMessage()); }
    }

    public void togglePublish(String quizId, boolean published) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE quizzes SET is_published=?, updated_at=now() WHERE id=?::uuid")) {
            ps.setBoolean(1, published); ps.setString(2, quizId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error toggling quiz publish: " + e.getMessage()); }
    }

    // ==================== QUESTIONS ====================

    public List<QuizQuestion> findQuestionsByQuizId(String quizId) {
        String sql = "SELECT * FROM quiz_questions WHERE quiz_id = ?::uuid ORDER BY question_order ASC";
        List<QuizQuestion> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quizId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapQuestion(rs)); }
        } catch (SQLException e) { System.err.println("Error finding questions: " + e.getMessage()); }
        return list;
    }

    public int countQuestionsByQuizId(String quizId) {
        String sql = "SELECT COUNT(*) FROM quiz_questions WHERE quiz_id = ?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quizId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Error counting questions: " + e.getMessage()); }
        return 0;
    }

    public String saveQuestion(QuizQuestion q) {
        String sql = "INSERT INTO quiz_questions (quiz_id,question_text,description,explanation,question_type,question_image_url,difficulty,points,question_order,is_required,max_word_count,model_answer,manual_grading,blank_template) VALUES (?::uuid,?,?,?,?::quiz_question_type,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getQuizId());
            ps.setString(2, q.getQuestionText());
            setNullStr(ps, 3, q.getDescription());
            setNullStr(ps, 4, q.getExplanation());
            ps.setString(5, q.getQuestionType());
            setNullStr(ps, 6, q.getQuestionImageUrl());
            ps.setString(7, q.getDifficulty() != null ? q.getDifficulty() : "medium");
            ps.setInt(8, q.getPoints());
            ps.setInt(9, q.getQuestionOrder());
            ps.setBoolean(10, q.isRequired());
            if (q.getMaxWordCount() != null) ps.setInt(11, q.getMaxWordCount()); else ps.setNull(11, Types.INTEGER);
            setNullStr(ps, 12, q.getModelAnswer());
            ps.setBoolean(13, q.isManualGrading());
            setNullStr(ps, 14, q.getBlankTemplate());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) { System.err.println("Error saving question: " + e.getMessage()); }
        return null;
    }

    public void updateQuestion(QuizQuestion q) {
        String sql = "UPDATE quiz_questions SET question_text=?,description=?,explanation=?,question_type=?::quiz_question_type,question_image_url=?,difficulty=?,points=?,question_order=?,is_required=?,max_word_count=?,model_answer=?,manual_grading=?,blank_template=? WHERE id=?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getQuestionText());
            setNullStr(ps, 2, q.getDescription());
            setNullStr(ps, 3, q.getExplanation());
            ps.setString(4, q.getQuestionType());
            setNullStr(ps, 5, q.getQuestionImageUrl());
            ps.setString(6, q.getDifficulty() != null ? q.getDifficulty() : "medium");
            ps.setInt(7, q.getPoints());
            ps.setInt(8, q.getQuestionOrder());
            ps.setBoolean(9, q.isRequired());
            if (q.getMaxWordCount() != null) ps.setInt(10, q.getMaxWordCount()); else ps.setNull(10, Types.INTEGER);
            setNullStr(ps, 11, q.getModelAnswer());
            ps.setBoolean(12, q.isManualGrading());
            setNullStr(ps, 13, q.getBlankTemplate());
            ps.setString(14, q.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating question: " + e.getMessage()); }
    }

    public void deleteQuestion(String questionId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM quiz_questions WHERE id=?::uuid")) {
            ps.setString(1, questionId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting question: " + e.getMessage()); }
    }

    // ==================== OPTIONS ====================

    public List<QuizOption> findOptionsByQuestionId(String questionId) {
        String sql = "SELECT * FROM quiz_options WHERE question_id=?::uuid ORDER BY option_order ASC";
        List<QuizOption> list = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questionId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapOption(rs)); }
        } catch (SQLException e) { System.err.println("Error finding options: " + e.getMessage()); }
        return list;
    }

    public String saveOption(QuizOption o) {
        String sql = "INSERT INTO quiz_options (question_id,option_text,is_correct,option_order) VALUES (?::uuid,?,?,?) RETURNING id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getQuestionId()); ps.setString(2, o.getOptionText());
            ps.setBoolean(3, o.isCorrect()); ps.setInt(4, o.getOptionOrder());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("id"); }
        } catch (SQLException e) { System.err.println("Error saving option: " + e.getMessage()); }
        return null;
    }

    public void updateOption(QuizOption o) {
        String sql = "UPDATE quiz_options SET option_text=?,is_correct=?,option_order=? WHERE id=?::uuid";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getOptionText()); ps.setBoolean(2, o.isCorrect());
            ps.setInt(3, o.getOptionOrder()); ps.setString(4, o.getId()); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating option: " + e.getMessage()); }
    }

    public void deleteOption(String optionId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM quiz_options WHERE id=?::uuid")) {
            ps.setString(1, optionId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting option: " + e.getMessage()); }
    }

    public void deleteOptionsByQuestion(String questionId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM quiz_options WHERE question_id=?::uuid")) {
            ps.setString(1, questionId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting options: " + e.getMessage()); }
    }

    // ==================== MAPPERS ====================

    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        Quiz q = new Quiz();
        q.setId(rs.getString("id")); q.setCourseId(rs.getString("course_id"));
        try { q.setLessonId(rs.getString("lesson_id")); } catch (SQLException ignored) {}
        q.setTitle(rs.getString("title")); q.setDescription(rs.getString("description"));
        q.setTimeLimitMinutes(rs.getObject("time_limit_minutes") != null ? rs.getInt("time_limit_minutes") : null);
        q.setPassingScore(rs.getInt("passing_score")); q.setAttemptsAllowed(rs.getInt("attempts_allowed"));
        q.setShuffleQuestions(rs.getBoolean("shuffle_questions"));
        q.setShowAnswersAfterSubmit(rs.getBoolean("show_answers_after_submit"));
        q.setPublished(rs.getBoolean("is_published"));
        q.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        q.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return q;
    }

    private QuizQuestion mapQuestion(ResultSet rs) throws SQLException {
        QuizQuestion qq = new QuizQuestion();
        qq.setId(rs.getString("id")); qq.setQuizId(rs.getString("quiz_id"));
        qq.setQuestionText(rs.getString("question_text"));
        try { qq.setDescription(rs.getString("description")); } catch (SQLException ignored) {}
        try { qq.setExplanation(rs.getString("explanation")); } catch (SQLException ignored) {}
        qq.setQuestionType(rs.getString("question_type"));
        qq.setPoints(rs.getInt("points")); qq.setQuestionOrder(rs.getInt("question_order"));
        qq.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        try { qq.setQuestionImageUrl(rs.getString("question_image_url")); } catch (SQLException ignored) {}
        try { qq.setDifficulty(rs.getString("difficulty")); } catch (SQLException ignored) {}
        try { qq.setRequired(rs.getBoolean("is_required")); } catch (SQLException ignored) {}
        try { Object v = rs.getObject("max_word_count"); qq.setMaxWordCount(v != null ? rs.getInt("max_word_count") : null); } catch (SQLException ignored) {}
        try { qq.setModelAnswer(rs.getString("model_answer")); } catch (SQLException ignored) {}
        try { qq.setManualGrading(rs.getBoolean("manual_grading")); } catch (SQLException ignored) {}
        try { qq.setBlankTemplate(rs.getString("blank_template")); } catch (SQLException ignored) {}
        return qq;
    }

    private QuizOption mapOption(ResultSet rs) throws SQLException {
        QuizOption o = new QuizOption();
        o.setId(rs.getString("id")); o.setQuestionId(rs.getString("question_id"));
        o.setOptionText(rs.getString("option_text")); o.setCorrect(rs.getBoolean("is_correct"));
        o.setOptionOrder(rs.getInt("option_order"));
        return o;
    }

    private void setNullStr(PreparedStatement ps, int i, String v) throws SQLException {
        if (v != null && !v.isBlank()) ps.setString(i, v); else ps.setNull(i, Types.VARCHAR);
    }
}
