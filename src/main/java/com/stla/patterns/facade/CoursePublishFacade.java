package com.stla.patterns.facade;

import com.stla.data.repositories.CourseRepositoryImpl;
import com.stla.data.repositories.InstructorRepositoryImpl;
import com.stla.domain.models.Course;
import com.stla.domain.models.Instructor;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;
import com.stla.services.NotificationService;

import java.util.Optional;

/**
 * Facade Pattern: Orchestrates the course publish/approval workflow.
 * Enforces instructor verification before course submission.
 */
public class CoursePublishFacade {

    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();
    private final InstructorRepositoryImpl instructorRepo = new InstructorRepositoryImpl();
    private final NotificationService notificationService = new NotificationService();

    /**
     * Save a course as draft.
     */
    public void saveDraft(Course course) {
        if (course.getId() == null || course.getId().isBlank()) {
            course.setStatus(com.stla.domain.enums.CourseStatus.DRAFT);
            courseRepo.save(course);
            System.out.println("[CoursePublishFacade] Draft saved: " + course.getTitle());
        } else {
            courseRepo.update(course);
            System.out.println("[CoursePublishFacade] Draft updated: " + course.getTitle());
        }
    }

    /**
     * Submit a course for approval (instructor action).
     * Instructor must be verified before submitting courses.
     */
    public boolean submitForApproval(String courseId, String instructorId) {
        if (!instructorRepo.isInstructorVerified(instructorId)) {
            throw new IllegalStateException("Instructor must be verified before creating or publishing courses.");
        }
        courseRepo.updateStatus(courseId, "pending", null, null);

        // Get course title for notification
        Optional<Course> opt = courseRepo.findById(courseId);
        String title = opt.map(Course::getTitle).orElse("Unknown");
        notificationService.notifyCourseSubmitted(courseId, title);

        System.out.println("[CoursePublishFacade] Course " + courseId + " submitted for approval");
        return true;
    }

    /**
     * Approve a course (admin action).
     */
    public boolean approveCourse(String courseId, String adminId, String note) {
        courseRepo.updateStatus(courseId, "approved", note, adminId);

        // Get course + instructor for notification
        Optional<Course> opt = courseRepo.findById(courseId);
        if (opt.isPresent()) {
            Course c = opt.get();
            Optional<Instructor> inst = instructorRepo.findById(c.getInstructorId());
            if (inst.isPresent()) {
                notificationService.notifyCourseApproved(courseId, inst.get().getProfileId(), c.getTitle());
            }
        }

        EventBus.getInstance().publish(new AppEvent(
            AppEvent.EventType.COURSE_APPROVED, adminId, courseId, "Course approved"
        ));
        System.out.println("[CoursePublishFacade] Course " + courseId + " approved by admin " + adminId);
        return true;
    }

    /**
     * Reject a course (admin action).
     */
    public boolean rejectCourse(String courseId, String adminId, String reason) {
        courseRepo.updateStatus(courseId, "rejected", reason, adminId);
        courseRepo.updateRejectionReason(courseId, reason);

        // Get course + instructor for notification
        Optional<Course> opt = courseRepo.findById(courseId);
        if (opt.isPresent()) {
            Course c = opt.get();
            Optional<Instructor> inst = instructorRepo.findById(c.getInstructorId());
            if (inst.isPresent()) {
                notificationService.notifyCourseRejected(courseId, inst.get().getProfileId(), c.getTitle(), reason);
            }
        }

        EventBus.getInstance().publish(new AppEvent(
            AppEvent.EventType.COURSE_REJECTED, adminId, courseId, "Course rejected: " + reason
        ));
        System.out.println("[CoursePublishFacade] Course " + courseId + " rejected");
        return true;
    }
}
