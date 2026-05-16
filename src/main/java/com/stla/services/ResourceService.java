package com.stla.services;

import com.stla.data.repositories.ResourceRepositoryImpl;
import com.stla.domain.models.LessonResource;

import java.io.File;
import java.util.List;

/**
 * Service for lesson resource management.
 * Handles upload, listing, and deletion of lesson resources (PDF, DOCX, ZIP, images).
 */
public class ResourceService {

    private final ResourceRepositoryImpl resourceRepo = new ResourceRepositoryImpl();
    private final SupabaseStorageAdapter storageAdapter = new SupabaseStorageAdapter();

    public List<LessonResource> getResources(String lessonId) {
        return resourceRepo.findByLessonId(lessonId);
    }

    public int getResourceCount(String lessonId) {
        return resourceRepo.findByLessonId(lessonId).size();
    }

    /**
     * Upload a resource file and save metadata to the database.
     * @param file The local file to upload
     * @param lessonId The lesson to attach the resource to
     * @param title Display title for the resource
     * @return The resource ID if successful, null otherwise
     */
    public String uploadAndSaveResource(File file, String lessonId, String title) {
        String url = storageAdapter.uploadLessonResource(file, lessonId);
        if (url == null) return null;

        LessonResource r = new LessonResource();
        r.setLessonId(lessonId);
        r.setTitle(title);
        r.setResourceUrl(url);
        r.setResourceType(getFileExtension(file.getName()));
        r.setFileSize(file.length());
        return resourceRepo.save(r);
    }

    public void deleteResource(String resourceId) {
        resourceRepo.delete(resourceId);
    }

    private String getFileExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1).toLowerCase() : "file";
    }
}
