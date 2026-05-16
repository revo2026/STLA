package com.stla.services;

import com.stla.data.repositories.LessonRepositoryImpl;
import com.stla.data.repositories.ResourceRepositoryImpl;
import com.stla.data.repositories.SectionRepositoryImpl;
import com.stla.domain.models.CourseLesson;
import com.stla.domain.models.CourseSection;
import com.stla.domain.models.LessonResource;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;

import java.io.File;
import java.util.List;

/**
 * Service for course sections, lessons, and resources.
 * Controllers call this service; SQL is in repositories only.
 */
public class LessonService {

    private final SectionRepositoryImpl sectionRepo = new SectionRepositoryImpl();
    private final LessonRepositoryImpl lessonRepo = new LessonRepositoryImpl();
    private final ResourceRepositoryImpl resourceRepo = new ResourceRepositoryImpl();
    private final SupabaseStorageAdapter storageAdapter = new SupabaseStorageAdapter();

    // ==================== SECTIONS ====================

    public List<CourseSection> getSections(String courseId) {
        List<CourseSection> sections = sectionRepo.findByCourseId(courseId);
        for (CourseSection s : sections) {
            s.setLessons(lessonRepo.findBySectionId(s.getId()));
        }
        return sections;
    }

    public String addSection(CourseSection section) {
        if (section.getSectionOrder() <= 0) {
            section.setSectionOrder(sectionRepo.countByCourse(section.getCourseId()) + 1);
        }
        return sectionRepo.save(section);
    }

    public void updateSection(CourseSection section) { sectionRepo.update(section); }
    public void deleteSection(String sectionId) { sectionRepo.delete(sectionId); }

    public void reorderSections(List<CourseSection> sections) {
        for (int i = 0; i < sections.size(); i++) {
            sectionRepo.reorder(sections.get(i).getId(), i + 1);
        }
    }

    // ==================== LESSONS ====================

    public List<CourseLesson> getLessons(String sectionId) {
        return lessonRepo.findBySectionId(sectionId);
    }

    public List<CourseLesson> getLessonsByCourse(String courseId) {
        return lessonRepo.findByCourseId(courseId);
    }

    public String addLesson(CourseLesson lesson) {
        if (lesson.getLessonOrder() <= 0) {
            lesson.setLessonOrder(lessonRepo.countByCourse(lesson.getCourseId()) + 1);
        }
        String id = lessonRepo.save(lesson);
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.NEW_LESSON_ADDED, null, lesson.getCourseId(),
                "New lesson \"" + lesson.getTitle() + "\" was added to your course."));
        return id;
    }

    public int getTotalPublishedLessons(String courseId) {
        return new LearningProgressService().countPublishedLessons(courseId);
    }

    public boolean isLessonCompleted(String studentId, String lessonId) {
        return new com.stla.data.repositories.LessonProgressRepository().isCompleted(studentId, lessonId);
    }

    public void updateLesson(CourseLesson lesson) { lessonRepo.update(lesson); }
    public void deleteLesson(String lessonId) { lessonRepo.delete(lessonId); }

    public void reorderLessons(List<CourseLesson> lessons) {
        List<String> ids = lessons.stream().map(CourseLesson::getId).toList();
        lessonRepo.batchReorder(ids);
    }

    // ==================== VIDEO UPLOAD ====================

    public String uploadVideo(File videoFile, String lessonId) {
        return storageAdapter.uploadLessonVideo(videoFile, lessonId);
    }

    /** Upload video with organized path: lesson-videos/{courseId}/{sectionId}/{lessonId}_{ts}.ext */
    public String uploadVideo(File videoFile, String courseId, String sectionId, String lessonId) {
        return storageAdapter.uploadLessonVideo(videoFile, courseId, sectionId, lessonId);
    }

    // ==================== RESOURCES ====================

    public List<LessonResource> getResources(String lessonId) {
        return resourceRepo.findByLessonId(lessonId);
    }

    public int getResourceCount(String lessonId) {
        return resourceRepo.findByLessonId(lessonId).size();
    }

    public String uploadAndSaveResource(File file, String lessonId, String title) {
        String url = storageAdapter.uploadLessonResource(file, lessonId);
        if (url == null) return null;
        LessonResource r = new LessonResource();
        r.setLessonId(lessonId);
        r.setTitle(title);
        r.setResourceUrl(url);
        r.setResourceType(getFileExtension(file.getName()));
        r.setFileSize(file.length());
        String resourceId = resourceRepo.save(r);
        String courseId = resolveCourseIdForLesson(lessonId);
        if (courseId != null) {
            EventBus.getInstance().publish(new AppEvent(
                    AppEvent.EventType.NEW_RESOURCE_ADDED, null, courseId,
                    "New resource \"" + title + "\" was added to a lesson."));
        }
        return resourceId;
    }

    private String resolveCourseIdForLesson(String lessonId) {
        return lessonRepo.findById(lessonId).map(CourseLesson::getCourseId).orElse(null);
    }

    public void deleteResource(String resourceId) { resourceRepo.delete(resourceId); }

    // ==================== DUPLICATE ====================

    /** Duplicate a lesson with "(Copy)" suffix */
    public String duplicateLesson(CourseLesson original) {
        CourseLesson copy = new CourseLesson();
        copy.setCourseId(original.getCourseId());
        copy.setSectionId(original.getSectionId());
        copy.setTitle(original.getTitle() + " (Copy)");
        copy.setDescription(original.getDescription());
        // Use max order + 1 to avoid unique constraint violation on (course_id, lesson_order)
        copy.setLessonOrder(lessonRepo.countByCourse(original.getCourseId()) + 1);
        copy.setVideoUrl(original.getVideoUrl());
        copy.setDurationSeconds(original.getDurationSeconds());
        copy.setPreview(original.isPreview());
        return lessonRepo.save(copy);
    }

    // ==================== QUIZ LOOKUP ====================

    /** Get quiz attached to a specific lesson (if any) */
    public com.stla.domain.models.Quiz getLessonQuiz(String lessonId) {
        com.stla.data.repositories.QuizRepositoryImpl quizRepo = new com.stla.data.repositories.QuizRepositoryImpl();
        return quizRepo.findByLessonId(lessonId).orElse(null);
    }

    private String getFileExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1).toLowerCase() : "file";
    }
}
