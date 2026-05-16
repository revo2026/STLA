package com.stla.services;

import com.stla.core.database.DatabaseConnection;
import com.stla.data.repositories.LessonProgressRepository;
import com.stla.data.repositories.StudentCourseProgressRepository;
import com.stla.domain.models.CourseLesson;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Tracks lesson watch progress, completions, and course-level progress.
 */
public class LearningProgressService {

    private static final double COMPLETION_THRESHOLD = 0.90;

    private final LessonProgressRepository lessonProgressRepo = new LessonProgressRepository();
    private final StudentCourseProgressRepository courseProgressRepo = new StudentCourseProgressRepository();
    private final CertificateService certificateService = new CertificateService();

    public void recordWatchProgress(String studentId, String lessonId, int watchedSeconds, int totalSeconds) {
        if (studentId == null || lessonId == null) return;
        String courseId = resolveCourseId(lessonId);
        if (courseId == null) return;

        String enrollmentId = lessonProgressRepo.findEnrollmentId(studentId, courseId).orElse(null);
        lessonProgressRepo.upsertWatchProgress(studentId, lessonId, enrollmentId, watchedSeconds, watchedSeconds);
        courseProgressRepo.touchAccess(studentId, courseId, lessonId);
    }

    public boolean canCompleteLesson(String studentId, CourseLesson lesson) {
        if (studentId == null || lesson == null) return false;
        if (lessonProgressRepo.isCompleted(studentId, lesson.getId())) return false;

        int duration = lesson.getDurationSeconds();
        if (duration <= 0) return true;

        String url = lesson.getVideoUrl();
        if (url == null || url.isBlank()) return true;

        int watched = lessonProgressRepo.getWatchedSeconds(studentId, lesson.getId());
        return watched >= (int) Math.ceil(duration * COMPLETION_THRESHOLD);
    }

    public boolean isLessonCompleted(String studentId, String lessonId) {
        return lessonProgressRepo.isCompleted(studentId, lessonId);
    }

    public void markLessonCompleted(String studentId, CourseLesson lesson) {
        if (studentId == null || lesson == null) return;
        String courseId = lesson.getCourseId() != null ? lesson.getCourseId() : resolveCourseId(lesson.getId());
        if (courseId == null) return;

        String enrollmentId = lessonProgressRepo.findEnrollmentId(studentId, courseId).orElse(null);
        int watched = Math.max(lessonProgressRepo.getWatchedSeconds(studentId, lesson.getId()),
                lesson.getDurationSeconds());
        lessonProgressRepo.markCompleted(studentId, lesson.getId(), enrollmentId, watched);

        courseProgressRepo.ensureProgressRow(studentId, courseId);
        recalculateCourseProgress(studentId, courseId);

        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.LESSON_COMPLETED,
                studentId, lesson.getId(),
                "Lesson completed: " + lesson.getTitle()));
    }

    public void recalculateCourseProgress(String studentId, String courseId) {
        Optional<String> enrollmentIdOpt = lessonProgressRepo.findEnrollmentId(studentId, courseId);
        if (enrollmentIdOpt.isEmpty()) return;
        recalculateCourseProgress(enrollmentIdOpt.get(), studentId, courseId);
    }

    public void recalculateCourseProgress(String enrollmentId, String studentId, String courseId) {
        int total = countPublishedLessons(courseId);
        int completed = countCompletedLessons(studentId, courseId);
        double percent = total > 0 ? (completed * 100.0 / total) : 0;

        String lastLessonId = findLastCompletedLessonId(studentId, courseId);
        courseProgressRepo.updateProgress(enrollmentId, completed, total, percent, lastLessonId);

        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.COURSE_PROGRESS_UPDATED,
                studentId, courseId,
                String.format("Course progress: %.0f%% (%d/%d lessons)", percent, completed, total)));

        if (percent >= 100.0) {
            completeCourseIfEligible(enrollmentId, studentId, courseId);
        }
    }

    public void completeCourseIfEligible(String enrollmentId, String studentId, String courseId) {
        String sql = """
            UPDATE enrollments SET status = 'completed', completed_at = now(), updated_at = now()
            WHERE id = ?::uuid AND status = 'active'
            """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, enrollmentId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                EventBus.getInstance().publish(new AppEvent(
                        AppEvent.EventType.COURSE_COMPLETED,
                        studentId, courseId,
                        "Congratulations! You completed the course."));
                certificateService.issueCertificateIfEligible(studentId, courseId, enrollmentId);
            }
        } catch (SQLException e) {
            System.err.println("Complete enrollment error: " + e.getMessage());
        }
    }

    public void markQuizPassed(String studentId, String courseId) {
        courseProgressRepo.touchAccess(studentId, courseId, null);
        recalculateCourseProgress(studentId, courseId);
    }

    public StudentCourseProgressRepository.CourseProgressSnapshot getCourseProgress(String studentId, String courseId) {
        courseProgressRepo.ensureProgressRow(studentId, courseId);
        return courseProgressRepo.findByStudentAndCourse(studentId, courseId).orElse(null);
    }

    public int countPublishedLessons(String courseId) {
        return countPublishedLessonsInternal(courseId);
    }

    private int countCompletedLessons(String studentId, String courseId) {
        String sql = """
            SELECT COUNT(*) FROM lesson_progress lp
            JOIN course_lessons cl ON cl.id = lp.lesson_id
            WHERE lp.student_id = ?::uuid AND cl.course_id = ?::uuid
              AND lp.completed = true AND cl.is_published = true
            """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Count completed lessons: " + e.getMessage());
        }
        return 0;
    }

    private int countPublishedLessonsInternal(String courseId) {
        String sql = "SELECT COUNT(*) FROM course_lessons WHERE course_id=?::uuid AND is_published=true";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Count published lessons: " + e.getMessage());
        }
        return 0;
    }

    private String findLastCompletedLessonId(String studentId, String courseId) {
        String sql = """
            SELECT lp.lesson_id FROM lesson_progress lp
            JOIN course_lessons cl ON cl.id = lp.lesson_id
            WHERE lp.student_id = ?::uuid AND cl.course_id = ?::uuid AND lp.completed = true
            ORDER BY lp.completed_at DESC NULLS LAST LIMIT 1
            """;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("lesson_id");
            }
        } catch (SQLException ignored) {}
        return null;
    }

    private String resolveCourseId(String lessonId) {
        String sql = "SELECT course_id FROM course_lessons WHERE id=?::uuid";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("course_id");
            }
        } catch (SQLException ignored) {}
        return null;
    }
}
