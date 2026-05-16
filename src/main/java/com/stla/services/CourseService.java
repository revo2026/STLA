package com.stla.services;

import com.stla.core.database.DatabaseConnection;
import com.stla.domain.enums.*;
import com.stla.domain.models.*;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Comprehensive service for all student, instructor, and admin operations.
 * Controllers call this service; this service handles SQL via inline JDBC.
 */
public class CourseService {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ==================== ENROLLMENT ====================

    public List<Enrollment> getStudentEnrollments(String studentId) {
        String sql = """
            SELECT e.*, c.title as course_title, c.thumbnail_url as course_thumbnail_url,
                   p.full_name as instructor_name,
                   COALESCE(scp.progress_percent, 0) as progress,
                   COALESCE(scp.lessons_completed, 0) as lessons_completed,
                   COALESCE(scp.total_lessons, 0) as total_lessons,
                   scp.last_accessed_at,
                   cl.title as last_lesson_title,
                   EXISTS (
                     SELECT 1 FROM reviews r
                     WHERE r.student_id = e.student_id AND r.course_id = e.course_id
                   ) as has_review
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            JOIN instructors i ON i.id = c.instructor_id
            JOIN profiles p ON p.id = i.profile_id
            LEFT JOIN student_course_progress scp ON scp.enrollment_id = e.id
            LEFT JOIN course_lessons cl ON cl.id = scp.last_lesson_id
            WHERE e.student_id = ?::uuid
            ORDER BY e.enrolled_at DESC
            """;
        List<Enrollment> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Enrollment en = new Enrollment();
                    en.setId(rs.getString("id"));
                    en.setStudentId(rs.getString("student_id"));
                    en.setCourseId(rs.getString("course_id"));
                    en.setStatus(EnrollmentStatus.fromValue(rs.getString("status")));
                    en.setEnrolledAt(toLDT(rs.getTimestamp("enrolled_at")));
                    en.setCourseTitle(rs.getString("course_title"));
                    try { en.setCourseThumbnailUrl(rs.getString("course_thumbnail_url")); } catch (SQLException ignored) {}
                    try { en.setInstructorName(rs.getString("instructor_name")); } catch (SQLException ignored) {}
                    try { en.setHasReview(rs.getBoolean("has_review")); } catch (SQLException ignored) {}
                    en.setProgressPercent(rs.getDouble("progress"));
                    try { en.setLessonsCompleted(rs.getInt("lessons_completed")); } catch (SQLException ignored) {}
                    try { en.setTotalLessons(rs.getInt("total_lessons")); } catch (SQLException ignored) {}
                    try { en.setLastLessonTitle(rs.getString("last_lesson_title")); } catch (SQLException ignored) {}
                    try {
                        if (rs.getTimestamp("last_accessed_at") != null) {
                            en.setLastAccessedAt(rs.getTimestamp("last_accessed_at").toLocalDateTime());
                        }
                    } catch (SQLException ignored) {}
                    list.add(en);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public boolean enrollStudent(String studentId, String courseId) {
        String sql = "INSERT INTO enrollments (student_id, course_id) VALUES (?::uuid, ?::uuid)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            ps.executeUpdate();
            // Init progress
            initProgress(studentId, courseId);
            EventBus.getInstance().publish(new AppEvent(AppEvent.EventType.ENROLLMENT_CREATED, studentId, courseId, "Enrolled"));
            return true;
        } catch (SQLException e) { System.err.println("Enroll error: " + e.getMessage()); return false; }
    }

    private void initProgress(String studentId, String courseId) {
        String sql = """
            INSERT INTO student_course_progress (enrollment_id, student_id, course_id, total_lessons)
            SELECT e.id, e.student_id, e.course_id, COUNT(cl.id)
            FROM enrollments e
            LEFT JOIN course_lessons cl ON cl.course_id = e.course_id AND cl.is_published = true
            WHERE e.student_id = ?::uuid AND e.course_id = ?::uuid
            GROUP BY e.id, e.student_id, e.course_id
            ON CONFLICT (student_id, course_id) DO NOTHING
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId); ps.setString(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Init progress error: " + e.getMessage()); }
    }

    public boolean isEnrolled(String studentId, String courseId) {
        String sql = "SELECT 1 FROM enrollments WHERE student_id=?::uuid AND course_id=?::uuid AND status='active'";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId); ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    // ==================== LESSONS ====================

    public List<CourseLesson> getCourseLessons(String courseId) {
        String sql = "SELECT * FROM course_lessons WHERE course_id=?::uuid ORDER BY lesson_order";
        List<CourseLesson> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CourseLesson l = new CourseLesson();
                    l.setId(rs.getString("id")); l.setCourseId(rs.getString("course_id"));
                    l.setTitle(rs.getString("title")); l.setDescription(rs.getString("description"));
                    l.setLessonOrder(rs.getInt("lesson_order")); l.setVideoUrl(rs.getString("video_url"));
                    l.setDurationSeconds(rs.getInt("duration_seconds"));
                    l.setPreview(rs.getBoolean("is_preview")); l.setPublished(rs.getBoolean("is_published"));
                    l.setCreatedAt(toLDT(rs.getTimestamp("created_at")));
                    list.add(l);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public void saveLesson(CourseLesson lesson) {
        String sql = "INSERT INTO course_lessons (course_id, title, description, lesson_order, video_url, duration_seconds, is_preview) VALUES (?::uuid,?,?,?,?,?,?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, lesson.getCourseId()); ps.setString(2, lesson.getTitle());
            ps.setString(3, lesson.getDescription()); ps.setInt(4, lesson.getLessonOrder());
            ps.setString(5, lesson.getVideoUrl()); ps.setInt(6, lesson.getDurationSeconds());
            ps.setBoolean(7, lesson.isPreview());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    public void deleteLesson(String lessonId) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("DELETE FROM course_lessons WHERE id=?::uuid")) {
            ps.setString(1, lessonId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    public void markLessonComplete(String studentId, String lessonId) {
        String sql = "INSERT INTO lesson_progress (student_id, lesson_id, completed, completed_at) VALUES (?::uuid,?::uuid,true,now()) ON CONFLICT (student_id, lesson_id) DO UPDATE SET completed=true, completed_at=now()";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId); ps.setString(2, lessonId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    // ==================== PAYMENTS ====================

    public List<Payment> getAllPayments() {
        String sql = """
            SELECT p.*, pr.full_name as student_name, c.title as course_name
            FROM payments p
            JOIN students s ON s.id = p.student_id
            JOIN profiles pr ON pr.id = s.profile_id
            JOIN courses c ON c.id = p.course_id
            ORDER BY p.created_at DESC
            """;
        return queryPayments(sql);
    }

    public List<Payment> getStudentPayments(String studentId) {
        String sql = """
            SELECT p.*, pr.full_name as student_name, c.title as course_name
            FROM payments p
            JOIN students s ON s.id = p.student_id
            JOIN profiles pr ON pr.id = s.profile_id
            JOIN courses c ON c.id = p.course_id
            WHERE p.student_id = ?::uuid
            ORDER BY p.created_at DESC
            """;
        List<Payment> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapPayment(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public void createPayment(String studentId, String courseId, BigDecimal amount, String method) {
        String sql = "INSERT INTO payments (student_id, course_id, amount, status, gateway_provider, paid_at) VALUES (?::uuid,?::uuid,?,'paid',?,now())";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId); ps.setString(2, courseId);
            ps.setBigDecimal(3, amount); ps.setString(4, method);
            ps.executeUpdate();
            EventBus.getInstance().publish(new AppEvent(AppEvent.EventType.PAYMENT_COMPLETED, studentId, courseId, "Payment of $" + amount));
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private List<Payment> queryPayments(String sql) {
        List<Payment> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapPayment(rs));
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getString("id")); p.setStudentId(rs.getString("student_id"));
        p.setCourseId(rs.getString("course_id")); p.setAmount(rs.getBigDecimal("amount"));
        p.setCurrency(rs.getString("currency"));
        p.setStatus(PaymentStatus.fromValue(rs.getString("status")));
        p.setGatewayProvider(rs.getString("gateway_provider"));
        p.setPaidAt(toLDT(rs.getTimestamp("paid_at")));
        p.setCreatedAt(toLDT(rs.getTimestamp("created_at")));
        try { p.setStudentName(rs.getString("student_name")); } catch (SQLException ignored) {}
        try { p.setCourseName(rs.getString("course_name")); } catch (SQLException ignored) {}
        return p;
    }

    // ==================== QUIZ ====================

    public List<Quiz> getCourseQuizzes(String courseId) {
        List<Quiz> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM quizzes WHERE course_id=?::uuid ORDER BY created_at")) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Quiz q = new Quiz();
                    q.setId(rs.getString("id")); q.setCourseId(rs.getString("course_id"));
                    q.setTitle(rs.getString("title")); q.setDescription(rs.getString("description"));
                    q.setPassingScore(rs.getInt("passing_score")); q.setAttemptsAllowed(rs.getInt("attempts_allowed"));
                    q.setPublished(rs.getBoolean("is_published")); q.setCreatedAt(toLDT(rs.getTimestamp("created_at")));
                    list.add(q);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public List<QuizQuestion> getQuizQuestions(String quizId) {
        List<QuizQuestion> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM quiz_questions WHERE quiz_id=?::uuid ORDER BY question_order")) {
            ps.setString(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuizQuestion qq = new QuizQuestion();
                    qq.setId(rs.getString("id")); qq.setQuizId(rs.getString("quiz_id"));
                    qq.setQuestionText(rs.getString("question_text"));
                    qq.setQuestionType(rs.getString("question_type"));
                    qq.setPoints(rs.getInt("points")); qq.setQuestionOrder(rs.getInt("question_order"));
                    list.add(qq);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public List<QuizOption> getQuestionOptions(String questionId) {
        List<QuizOption> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM quiz_options WHERE question_id=?::uuid ORDER BY option_order")) {
            ps.setString(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuizOption o = new QuizOption();
                    o.setId(rs.getString("id")); o.setQuestionId(rs.getString("question_id"));
                    o.setOptionText(rs.getString("option_text")); o.setCorrect(rs.getBoolean("is_correct"));
                    o.setOptionOrder(rs.getInt("option_order"));
                    list.add(o);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public void saveQuiz(Quiz quiz) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("INSERT INTO quizzes (course_id, title, description, passing_score, attempts_allowed) VALUES (?::uuid,?,?,?,?)")) {
            ps.setString(1, quiz.getCourseId()); ps.setString(2, quiz.getTitle());
            ps.setString(3, quiz.getDescription()); ps.setInt(4, quiz.getPassingScore()); ps.setInt(5, quiz.getAttemptsAllowed());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    // ==================== CERTIFICATES ====================

    public List<IssuedCertificate> getStudentCertificates(String studentId) {
        List<IssuedCertificate> list = new ArrayList<>();
        String sql = "SELECT ic.*, c.title as course_name FROM issued_certificates ic JOIN courses c ON c.id=ic.course_id WHERE ic.student_id=?::uuid ORDER BY ic.issued_at DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IssuedCertificate cert = new IssuedCertificate();
                    cert.setId(rs.getString("id")); cert.setCertificateNo(rs.getString("certificate_no"));
                    cert.setStudentId(rs.getString("student_id")); cert.setCourseId(rs.getString("course_id"));
                    cert.setIssuedAt(toLDT(rs.getTimestamp("issued_at")));
                    cert.setCourseName(rs.getString("course_name"));
                    list.add(cert);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    // ==================== WALLET ====================

    public Optional<InstructorWallet> getInstructorWallet(String instructorId) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT * FROM instructor_wallets WHERE instructor_id=?::uuid")) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    InstructorWallet w = new InstructorWallet();
                    w.setId(rs.getString("id")); w.setInstructorId(rs.getString("instructor_id"));
                    w.setCurrency(rs.getString("currency"));
                    w.setPendingBalance(rs.getBigDecimal("pending_balance"));
                    w.setAvailableBalance(rs.getBigDecimal("available_balance"));
                    w.setTotalEarned(rs.getBigDecimal("total_earned"));
                    w.setTotalWithdrawn(rs.getBigDecimal("total_withdrawn"));
                    return Optional.of(w);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return Optional.empty();
    }

    // ==================== WITHDRAWALS ====================

    public List<WithdrawalRequest> getAllWithdrawals() {
        List<WithdrawalRequest> list = new ArrayList<>();
        String sql = "SELECT wr.*, p.full_name as instructor_name FROM withdrawal_requests wr JOIN instructors i ON i.id=wr.instructor_id JOIN profiles p ON p.id=i.profile_id ORDER BY wr.requested_at DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { list.add(mapWithdrawal(rs)); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    public void approveWithdrawal(String withdrawalId, String adminId) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("UPDATE withdrawal_requests SET status='approved', reviewed_by_admin_id=?::uuid, reviewed_at=now() WHERE id=?::uuid")) {
            ps.setString(1, adminId); ps.setString(2, withdrawalId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    public void rejectWithdrawal(String withdrawalId, String adminId, String note) {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("UPDATE withdrawal_requests SET status='rejected', reviewed_by_admin_id=?::uuid, review_note=?, reviewed_at=now() WHERE id=?::uuid")) {
            ps.setString(1, adminId); ps.setString(2, note); ps.setString(3, withdrawalId); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
    }

    private WithdrawalRequest mapWithdrawal(ResultSet rs) throws SQLException {
        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setId(rs.getString("id")); wr.setInstructorId(rs.getString("instructor_id"));
        wr.setAmount(rs.getBigDecimal("amount")); wr.setMethod(rs.getString("method"));
        wr.setStatus(WithdrawalStatus.fromValue(rs.getString("status")));
        wr.setRequestedAt(toLDT(rs.getTimestamp("requested_at")));
        try { wr.setInstructorName(rs.getString("instructor_name")); } catch (SQLException ignored) {}
        return wr;
    }

    // ==================== ENROLLED STUDENTS (INSTRUCTOR) ====================

    public List<Enrollment> getEnrolledStudentsForInstructor(String instructorId) {
        String sql = """
            SELECT e.*, c.title as course_title, p.full_name as student_name,
                   COALESCE(scp.progress_percent, 0) as progress
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            JOIN students s ON s.id = e.student_id
            JOIN profiles p ON p.id = s.profile_id
            LEFT JOIN student_course_progress scp ON scp.enrollment_id = e.id
            WHERE c.instructor_id = ?::uuid
            ORDER BY e.enrolled_at DESC
            """;
        List<Enrollment> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Enrollment en = new Enrollment();
                    en.setId(rs.getString("id")); en.setStudentId(rs.getString("student_id"));
                    en.setCourseId(rs.getString("course_id"));
                    en.setStatus(EnrollmentStatus.fromValue(rs.getString("status")));
                    en.setEnrolledAt(toLDT(rs.getTimestamp("enrolled_at")));
                    en.setCourseTitle(rs.getString("course_title"));
                    en.setStudentName(rs.getString("student_name"));
                    en.setProgressPercent(rs.getDouble("progress"));
                    list.add(en);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    // ==================== REVIEWS ====================

    public List<Review> getCourseReviews(String courseId) {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT r.*, p.full_name as student_name FROM reviews r JOIN students s ON s.id=r.student_id JOIN profiles p ON p.id=s.profile_id WHERE r.course_id=?::uuid ORDER BY r.created_at DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review rev = new Review();
                    rev.setId(rs.getString("id")); rev.setCourseId(rs.getString("course_id"));
                    rev.setRating(rs.getInt("rating")); rev.setComment(rs.getString("comment"));
                    rev.setCreatedAt(toLDT(rs.getTimestamp("created_at")));
                    rev.setStudentName(rs.getString("student_name"));
                    list.add(rev);
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return list;
    }

    // ==================== INSTRUCTOR REVENUE STATS ====================

    public BigDecimal getInstructorRevenue(String instructorId) {
        String sql = """
            SELECT COALESCE(SUM(p.instructor_share), 0)
            FROM payments p
            JOIN courses c ON c.id = p.course_id
            WHERE c.instructor_id = ?::uuid AND p.status = 'paid'
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getBigDecimal(1); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return BigDecimal.ZERO;
    }

    public double getInstructorAverageRating(String instructorId) {
        String sql = """
            SELECT COALESCE(AVG(r.rating), 0)
            FROM reviews r
            JOIN courses c ON c.id = r.course_id
            WHERE c.instructor_id = ?::uuid
            """;
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
                }
            }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return 0.0;
    }

    public int getInstructorTotalStudents(String instructorId) {
        String sql = "SELECT COUNT(DISTINCT e.student_id) FROM enrollments e JOIN courses c ON c.id=e.course_id WHERE c.instructor_id=?::uuid";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return 0;
    }

    // ==================== ADMIN STATS ====================

    public int getTotalPayments() {
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM payments"); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("Error: " + e.getMessage()); }
        return 0;
    }

    private LocalDateTime toLDT(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
