package com.stla.services;

import java.io.File;
import java.util.Set;

/**
 * Adapter Pattern: Wraps SupabaseStorageService for course-specific uploads.
 * Handles validation and bucket routing for thumbnails, videos, and resources.
 */
public class SupabaseStorageAdapter {

    private final SupabaseStorageService storage = SupabaseStorageService.getInstance();

    private static final Set<String> ALLOWED_VIDEO_EXT = Set.of(".mp4", ".mov", ".mkv", ".webm");
    private static final Set<String> ALLOWED_RESOURCE_EXT = Set.of(".pdf", ".docx", ".doc", ".zip", ".png", ".jpg", ".jpeg", ".webp");
    private static final long MAX_VIDEO_SIZE = 500L * 1024 * 1024; // 500 MB
    private static final long MAX_RESOURCE_SIZE = 50L * 1024 * 1024; // 50 MB

    /**
     * Upload course thumbnail to course-thumbnails bucket.
     */
    public String uploadCourseThumbnail(File file, String courseId) {
        if (file == null || !file.exists()) throw new IllegalArgumentException("Thumbnail file does not exist.");
        String ext = getExtension(file.getName());
        String path = "thumbnails/" + courseId + "/thumb_" + System.currentTimeMillis() + ext;
        System.out.println("[StorageAdapter] Uploading thumbnail: " + path);
        return storage.uploadFile("course-thumbnails", file, path);
    }

    /**
     * Upload lesson video with organized path.
     * Path: lesson-videos/{courseId}/{sectionId}/{lessonId}_{timestamp}.ext
     */
    public String uploadLessonVideo(File file, String lessonId) {
        validateVideo(file);
        String ext = getExtension(file.getName());
        String path = "videos/" + lessonId + "/video_" + System.currentTimeMillis() + ext;
        System.out.println("[StorageAdapter] Uploading video: " + path);
        return storage.uploadFile("course-thumbnails", file, path);
    }

    /**
     * Upload lesson video with full path organization.
     */
    public String uploadLessonVideo(File file, String courseId, String sectionId, String lessonId) {
        validateVideo(file);
        String ext = getExtension(file.getName());
        String path = "lesson-videos/" + courseId + "/" + sectionId + "/" + lessonId + "_" + System.currentTimeMillis() + ext;
        System.out.println("[StorageAdapter] Uploading video: " + path);
        return storage.uploadFile("course-thumbnails", file, path);
    }

    /**
     * Upload lesson resource (PDF, DOCX, ZIP, images).
     */
    public String uploadLessonResource(File file, String lessonId) {
        validateResource(file);
        String ext = getExtension(file.getName());
        String safeName = file.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String path = "lesson-resources/" + lessonId + "/" + System.currentTimeMillis() + "_" + safeName;
        System.out.println("[StorageAdapter] Uploading resource: " + path);
        return storage.uploadFile("course-thumbnails", file, path);
    }

    /**
     * Upload lesson resource with full path organization.
     */
    public String uploadLessonResource(File file, String courseId, String lessonId) {
        validateResource(file);
        String ext = getExtension(file.getName());
        String safeName = file.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String path = "lesson-resources/" + courseId + "/" + lessonId + "/" + System.currentTimeMillis() + "_" + safeName;
        System.out.println("[StorageAdapter] Uploading resource: " + path);
        return storage.uploadFile("course-thumbnails", file, path);
    }

    private void validateVideo(File file) {
        if (file == null || !file.exists()) throw new IllegalArgumentException("Video file does not exist.");
        if (file.length() > MAX_VIDEO_SIZE) throw new IllegalArgumentException("Video too large. Maximum 500MB.");
        String ext = getExtension(file.getName()).toLowerCase();
        if (!ALLOWED_VIDEO_EXT.contains(ext)) throw new IllegalArgumentException("Invalid video type. Allowed: MP4, MOV, MKV, WEBM.");
    }

    private void validateResource(File file) {
        if (file == null || !file.exists()) throw new IllegalArgumentException("Resource file does not exist.");
        if (file.length() > MAX_RESOURCE_SIZE) throw new IllegalArgumentException("Resource too large. Maximum 50MB.");
        String ext = getExtension(file.getName()).toLowerCase();
        if (!ALLOWED_RESOURCE_EXT.contains(ext)) throw new IllegalArgumentException("Invalid resource type. Allowed: PDF, DOCX, ZIP, PNG, JPG.");
    }

    /**
     * Upload question image (PNG, JPG, JPEG, WEBP, max 10MB).
     */
    public String uploadQuestionImage(File file, String questionId) {
        if (file == null || !file.exists()) throw new IllegalArgumentException("Image file does not exist.");
        if (file.length() > 10L * 1024 * 1024) throw new IllegalArgumentException("Image too large. Maximum 10MB.");
        String ext = getExtension(file.getName()).toLowerCase();
        if (!Set.of(".png", ".jpg", ".jpeg", ".webp").contains(ext))
            throw new IllegalArgumentException("Invalid image type. Allowed: PNG, JPG, JPEG, WEBP.");
        String path = "quiz-images/" + questionId + "/img_" + System.currentTimeMillis() + ext;
        System.out.println("[StorageAdapter] Uploading question image: " + path);
        return storage.uploadFile("course-thumbnails", file, path);
    }

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot) : "";
    }
}
