package com.stla.domain.models;

import java.time.LocalDateTime;

/**
 * Lesson resource model matching public.lesson_resources table.
 * Downloadable files attached to a lesson (PDF, DOCX, ZIP, images).
 */
public class LessonResource {

    private String id;
    private String lessonId;
    private String title;
    private String resourceUrl;
    private String resourceType; // pdf, docx, zip, image
    private long fileSize;       // bytes
    private LocalDateTime createdAt;

    public LessonResource() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLessonId() { return lessonId; }
    public void setLessonId(String lessonId) { this.lessonId = lessonId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Human-readable file size */
    public String getFileSizeFormatted() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}
