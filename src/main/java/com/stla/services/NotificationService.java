package com.stla.services;

import com.stla.data.repositories.NotificationRepositoryImpl;
import com.stla.data.repositories.WalletRepository;
import com.stla.domain.enums.NotificationType;
import com.stla.domain.models.Notification;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists notifications for students, admins, and instructors via EventBus events.
 */
public class NotificationService {

    private final NotificationRepositoryImpl notifRepo = new NotificationRepositoryImpl();
    private final WalletRepository walletRepo = new WalletRepository();

    public void handleAppEvent(AppEvent event) {
        switch (event.type()) {
            case PAYMENT_COMPLETED -> {
                notifyStudent(event.actorProfileId(), NotificationType.PAYMENT,
                        "Payment Successful ✅", event.message(), "payments", event.targetId());
                notifyAllAdmins(NotificationType.PAYMENT,
                        "New Payment Received 💳", event.message(), "payments", event.targetId());
            }
            case PAYMENT_FAILED -> {
                notifyStudent(event.actorProfileId(), NotificationType.PAYMENT,
                        "Payment Failed ❌", event.message(), "payments", event.targetId());
                notifyAllAdmins(NotificationType.PAYMENT,
                        "Payment Failed ❌", event.message(), "payments", event.targetId());
            }
            case ADMIN_COMMISSION_ADDED -> notifyAllAdmins(NotificationType.PAYMENT,
                    "Commission Added 💳", event.message(), "payments", event.targetId());
            case ENROLLMENT_CREATED -> {
                notifyStudent(event.actorProfileId(), NotificationType.ENROLLMENT,
                        "Enrollment Confirmed 🎓", event.message(), "courses", event.targetId());
                String courseTitle = resolveCourseTitle(event.targetId());
                notifyAllAdmins(NotificationType.ENROLLMENT,
                        "Student Enrolled 📚",
                        "A student enrolled in \"" + courseTitle + "\".",
                        "courses", event.targetId());
            }
            case USER_REGISTERED -> notifyAllAdmins(NotificationType.GENERAL,
                    "New User Registered 👤", event.message(), "profiles", event.targetId());
            case LESSON_COMPLETED -> {
                notifyStudent(event.actorProfileId(), NotificationType.COURSE_UPDATE,
                        "Lesson Completed 📘", event.message(), "course_lessons", event.targetId());
                String courseId = resolveCourseIdFromLesson(event.targetId());
                String studentName = resolveStudentDisplayName(event.actorProfileId());
                notifyInstructorForCourse(courseId, NotificationType.COURSE_UPDATE,
                        "Student Completed Lesson 📘",
                        studentName + " completed a lesson in \"" + resolveCourseTitle(courseId) + "\".",
                        "course_lessons", event.targetId());
            }
            case COURSE_COMPLETED -> {
                notifyStudent(event.actorProfileId(), NotificationType.ENROLLMENT,
                        "Course Completed! 🎉", event.message(), "courses", event.targetId());
                String studentName = resolveStudentDisplayName(event.actorProfileId());
                notifyInstructorForCourse(event.targetId(), NotificationType.ENROLLMENT,
                        "Student Completed Course 🎉",
                        studentName + " completed \"" + resolveCourseTitle(event.targetId()) + "\".",
                        "courses", event.targetId());
                notifyAllAdmins(NotificationType.ENROLLMENT,
                        "Course Completed 🎓",
                        studentName + " completed \"" + resolveCourseTitle(event.targetId()) + "\".",
                        "courses", event.targetId());
            }
            case COURSE_PROGRESS_UPDATED -> { /* no per-update notification */ }
            case QUIZ_SUBMITTED -> {
                notifyStudent(event.actorProfileId(), NotificationType.COURSE_UPDATE,
                        "Quiz Submitted", event.message(), "quizzes", event.targetId());
                notifyInstructorForQuiz(event.targetId(), event.actorProfileId(),
                        "Quiz Submitted 📝", event.message());
            }
            case QUIZ_PASSED -> {
                notifyStudent(event.actorProfileId(), NotificationType.COURSE_UPDATE,
                        "Quiz Passed ✅", event.message(), "quizzes", event.targetId());
                notifyInstructorForQuiz(event.targetId(), event.actorProfileId(),
                        "Student Passed Quiz ✅", event.message());
            }
            case QUIZ_FAILED -> {
                notifyStudent(event.actorProfileId(), NotificationType.COURSE_UPDATE,
                        "Quiz Attempt Failed", event.message(), "quizzes", event.targetId());
                notifyInstructorForQuiz(event.targetId(), event.actorProfileId(),
                        "Student Failed Quiz", event.message());
            }
            case NEW_LESSON_ADDED, NEW_RESOURCE_ADDED, NEW_QUIZ_ADDED ->
                    notifyEnrolledStudents(event.targetId(), event.type(), event.message());
            case CERTIFICATE_ISSUED -> notifyStudent(event.actorProfileId(), NotificationType.COURSE_UPDATE,
                    "Certificate Ready 🏆", event.message(), "issued_certificates", event.targetId());
            case STUDENT_REVIEW_ADDED -> notifyInstructor(event.actorProfileId(), NotificationType.COURSE_UPDATE,
                    "New Student Review ⭐", event.message(), "reviews", event.targetId());
            case WITHDRAWAL_COMPLETED -> {
                notifyInstructor(event.actorProfileId(), NotificationType.WITHDRAWAL,
                        "Withdrawal Completed ✅", event.message(), "wallet_transactions", event.targetId());
                notifyAllAdmins(NotificationType.WITHDRAWAL,
                        "Instructor Withdrawal 💸", event.message(), "wallet_transactions", event.targetId());
            }
            case INSTRUCTOR_REVENUE_ADDED -> notifyInstructor(event.actorProfileId(), NotificationType.GENERAL,
                    "Revenue Added 💰", event.message(), "courses", event.targetId());
            case COURSE_SUBMITTED -> notifyAllAdmins(NotificationType.COURSE_UPDATE,
                    "Course Submitted for Review 📋", event.message(), "courses", event.targetId());
            case COURSE_APPROVED -> {
                String instructorProfileId = resolveInstructorProfileFromCourse(event.targetId());
                if (instructorProfileId != null) {
                    save(instructorProfileId, event.actorProfileId(), NotificationType.COURSE_UPDATE,
                            "Course Approved! 🎉", event.message(), "courses", event.targetId());
                }
            }
            case COURSE_REJECTED -> {
                String instructorProfileId = resolveInstructorProfileFromCourse(event.targetId());
                if (instructorProfileId != null) {
                    save(instructorProfileId, event.actorProfileId(), NotificationType.COURSE_UPDATE,
                            "Course Rejected ❌", event.message(), "courses", event.targetId());
                }
            }
            case COURSE_PUBLISHED -> {
                String instructorProfileId = resolveInstructorProfileFromCourse(event.targetId());
                if (instructorProfileId != null) {
                    save(instructorProfileId, null, NotificationType.COURSE_UPDATE,
                            "Course Published 🚀", event.message(), "courses", event.targetId());
                }
            }
            case INSTRUCTOR_VERIFICATION_SUBMITTED, INSTRUCTOR_RESUBMITTED -> notifyAllAdmins(
                    NotificationType.ADMIN_ALERT,
                    event.type() == AppEvent.EventType.INSTRUCTOR_RESUBMITTED
                            ? "Verification Resubmitted 📄" : "Verification Submitted 📄",
                    event.message(), "instructors", event.targetId());
            case INSTRUCTOR_VERIFIED -> notifyInstructorById(event.targetId(), NotificationType.GENERAL,
                    "Verification Approved ✅",
                    "Your instructor account has been verified. You can now create and publish courses.",
                    "instructors", event.targetId(), event.actorProfileId());
            case INSTRUCTOR_REJECTED -> notifyInstructorById(event.targetId(), NotificationType.GENERAL,
                    "Verification Rejected ❌", event.message(), "instructors", event.targetId(), event.actorProfileId());
            default -> {}
        }
    }

    public void notifyEnrolledStudents(String courseId, AppEvent.EventType type, String message) {
        List<String> profileIds = findEnrolledStudentProfileIds(courseId);
        String title = switch (type) {
            case NEW_LESSON_ADDED -> "New Lesson Available 📚";
            case NEW_RESOURCE_ADDED -> "New Resource Added 📎";
            case NEW_QUIZ_ADDED -> "New Quiz Available 🎯";
            default -> "Course Update";
        };
        NotificationType nType = type == AppEvent.EventType.NEW_LESSON_ADDED
                ? NotificationType.NEW_LESSON : NotificationType.COURSE_UPDATE;
        for (String profileId : profileIds) {
            save(profileId, null, nType, title, message, "courses", courseId);
        }
    }

    public void notifyAllAdmins(NotificationType type, String title, String body, String refTable, String refId) {
        for (String adminId : walletRepo.findAllAdminProfileIds()) {
            save(adminId, null, type, title, body, refTable, refId);
        }
    }

    private void notifyStudent(String studentId, NotificationType type, String title, String body,
                               String refTable, String refId) {
        String profileId = resolveStudentProfileId(studentId);
        if (profileId != null) save(profileId, null, type, title, body, refTable, refId);
    }

    private void notifyInstructor(String instructorId, NotificationType type, String title, String body,
                                  String refTable, String refId) {
        String profileId = resolveInstructorProfileId(instructorId);
        if (profileId != null) save(profileId, null, type, title, body, refTable, refId);
    }

    private void notifyInstructorById(String instructorId, NotificationType type, String title, String body,
                                      String refTable, String refId, String actorProfileId) {
        String profileId = resolveInstructorProfileId(instructorId);
        if (profileId == null) profileId = instructorId;
        save(profileId, actorProfileId, type, title, body, refTable, refId);
    }

    private void notifyInstructorForCourse(String courseId, NotificationType type, String title, String body,
                                           String refTable, String refId) {
        String profileId = resolveInstructorProfileFromCourse(courseId);
        if (profileId != null) save(profileId, null, type, title, body, refTable, refId);
    }

    private void notifyInstructorForQuiz(String quizId, String studentId, String title, String body) {
        String courseId = resolveCourseIdFromQuiz(quizId);
        String studentName = resolveStudentDisplayName(studentId);
        String message = studentName + ": " + body;
        notifyInstructorForCourse(courseId, NotificationType.COURSE_UPDATE, title, message, "quizzes", quizId);
    }

    public void save(String recipientProfileId, String actorProfileId, NotificationType type,
                     String title, String body, String refTable, String refId) {
        if (recipientProfileId == null) return;
        Notification n = new Notification();
        n.setRecipientProfileId(recipientProfileId);
        n.setActorProfileId(actorProfileId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setReferenceTable(refTable);
        n.setReferenceId(refId);
        notifRepo.save(n);
    }

    /** @deprecated use save — kept for student controller compatibility */
    public void createStudentNotification(String recipientProfileId, NotificationType type,
                                          String title, String body, String refTable, String refId) {
        save(recipientProfileId, null, type, title, body, refTable, refId);
    }

    public List<Notification> getNotifications(String profileId) {
        return notifRepo.findByRecipient(profileId);
    }

    public List<Notification> getStudentNotifications(String profileId) {
        return getNotifications(profileId);
    }

    public List<Notification> getRecentNotifications(String profileId, int limit) {
        return notifRepo.findRecentByRecipient(profileId, limit);
    }

    public int countUnread(String profileId) {
        return notifRepo.countUnread(profileId);
    }

    public void markAsRead(String notificationId) {
        notifRepo.markAsRead(notificationId);
    }

    public void markAllAsRead(String profileId) {
        notifRepo.markAllAsRead(profileId);
    }

    public void notifyPaymentFailed(String studentId, String courseTitle, String reason) {
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.PAYMENT_FAILED, studentId, null,
                "Payment for \"" + courseTitle + "\" failed: " + reason));
    }

    public void notifyCourseApproved(String courseId, String instructorProfileId, String courseTitle) {
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.COURSE_APPROVED, instructorProfileId, courseId,
                "Your course \"" + courseTitle + "\" has been approved. You can now manage sections, lessons, and publish content."));
    }

    public void notifyCourseRejected(String courseId, String instructorProfileId, String courseTitle, String reason) {
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.COURSE_REJECTED, instructorProfileId, courseId,
                "Your course \"" + courseTitle + "\" was rejected. Reason: " + reason));
    }

    public void notifyCourseSubmitted(String courseId, String courseTitle) {
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.COURSE_SUBMITTED, null, courseId,
                "New course submitted for review: \"" + courseTitle + "\"."));
    }

    public void notifyGeneral(String recipientProfileId, String title, String body, String refTable, String refId) {
        if (recipientProfileId == null) {
            System.out.println("[NotificationService] " + title + ": " + body);
            return;
        }
        save(recipientProfileId, null, NotificationType.GENERAL, title, body, refTable, refId);
    }

    private List<String> findEnrolledStudentProfileIds(String courseId) {
        String sql = """
            SELECT s.profile_id FROM enrollments e
            JOIN students s ON s.id = e.student_id
            WHERE e.course_id = ?::uuid AND e.status = 'active'
            """;
        List<String> ids = new ArrayList<>();
        try (var conn = com.stla.core.database.DatabaseConnection.getInstance().getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("profile_id"));
            }
        } catch (Exception e) {
            System.err.println("Enrolled profiles error: " + e.getMessage());
        }
        return ids;
    }

    private String resolveStudentProfileId(String studentId) {
        return queryString("SELECT profile_id FROM students WHERE id=?::uuid", studentId);
    }

    private String resolveInstructorProfileId(String instructorId) {
        return queryString("SELECT profile_id FROM instructors WHERE id=?::uuid", instructorId);
    }

    private String resolveInstructorProfileFromCourse(String courseId) {
        return queryString("""
            SELECT i.profile_id FROM courses c
            JOIN instructors i ON i.id = c.instructor_id
            WHERE c.id = ?::uuid
            """, courseId);
    }

    private String resolveCourseIdFromLesson(String lessonId) {
        return queryString("SELECT course_id FROM course_lessons WHERE id=?::uuid", lessonId);
    }

    private String resolveCourseIdFromQuiz(String quizId) {
        return queryString("SELECT course_id FROM quizzes WHERE id=?::uuid", quizId);
    }

    private String resolveCourseTitle(String courseId) {
        if (courseId == null) return "course";
        String title = queryString("SELECT title FROM courses WHERE id=?::uuid", courseId);
        return title != null ? title : "course";
    }

    private String resolveStudentDisplayName(String studentId) {
        String name = queryString("""
            SELECT p.full_name FROM students s
            JOIN profiles p ON p.id = s.profile_id
            WHERE s.id = ?::uuid
            """, studentId);
        return name != null ? name : "A student";
    }

    private String queryString(String sql, String id) {
        if (id == null) return null;
        try (var conn = com.stla.core.database.DatabaseConnection.getInstance().getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            System.err.println("Query error: " + e.getMessage());
        }
        return null;
    }
}
