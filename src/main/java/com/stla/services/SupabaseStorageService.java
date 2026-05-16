package com.stla.services;

import com.stla.app.AppConfig;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.util.Set;

/**
 * Uploads/deletes files in Supabase Storage via the REST API.
 * Prefers service_role_key (bypasses RLS) over anon_key for uploads.
 * Validates file type and size before upload.
 */
public class SupabaseStorageService {

    private static SupabaseStorageService instance;

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp");

    private final String supabaseUrl;
    private final String uploadKey; // service_role_key preferred, fallback to anon
    private final String anonKey;

    private SupabaseStorageService() {
        AppConfig cfg = AppConfig.getInstance();
        this.supabaseUrl = cfg.getSupabaseUrl();
        this.anonKey = cfg.getSupabaseAnonKey();

        // Prefer service_role_key for uploads (bypasses storage RLS policies)
        String srvKey = cfg.getSupabaseServiceRoleKey();
        if (srvKey != null && !srvKey.isBlank()) {
            this.uploadKey = srvKey;
            System.out.println("[Storage] Using service_role_key for uploads.");
        } else {
            this.uploadKey = this.anonKey;
            System.out.println(
                    "[Storage] WARNING: No service_role_key — using anon_key. Ensure storage policies allow inserts.");
        }
        System.out.println("[Storage] Supabase URL: " + this.supabaseUrl);
    }

    public static SupabaseStorageService getInstance() {
        if (instance == null) {
            synchronized (SupabaseStorageService.class) {
                if (instance == null)
                    instance = new SupabaseStorageService();
            }
        }
        return instance;
    }

    /** Reset singleton (useful if .env changes at runtime) */
    public static void resetInstance() {
        synchronized (SupabaseStorageService.class) {
            instance = null;
        }
    }

    // ======================== PUBLIC METHODS ========================

    /** Upload student avatar → student-avatars bucket */
    public String uploadStudentAvatar(File imageFile, String profileId) {
        return uploadAvatarInternal("student-avatars", imageFile, profileId);
    }

    /** Upload instructor avatar → instructor-avatars bucket */
    public String uploadInstructorAvatar(File imageFile, String profileId) {
        return uploadAvatarInternal("instructor-avatars", imageFile, profileId);
    }

    /** Upload avatar — picks bucket based on role */
    public String uploadAvatar(File localFile, String role, String profileId) {
        String bucket = "instructor".equalsIgnoreCase(role) ? "instructor-avatars" : "student-avatars";
        return uploadAvatarInternal(bucket, localFile, profileId);
    }

    /** Upload course thumbnail → course-thumbnails bucket */
    public String uploadCourseThumbnail(File imageFile, String courseId) {
        validate(imageFile);
        String ext = getExtension(imageFile.getName());
        String path = "thumbnails/" + courseId + "/" + System.currentTimeMillis() + "_thumb" + ext;
        return uploadFile("course-thumbnails", imageFile, path);
    }

    /** Get public URL for a bucket + file path */
    public String getPublicUrl(String bucketName, String filePath) {
        if (supabaseUrl == null || supabaseUrl.isBlank())
            return null;
        return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + filePath;
    }

    /** Delete old file if URL is a Supabase URL (best-effort) */
    public void deleteOldFile(String oldUrl) {
        if (oldUrl == null || oldUrl.isBlank() || supabaseUrl == null || supabaseUrl.isBlank())
            return;
        if (!oldUrl.startsWith(supabaseUrl))
            return;

        try {
            String marker = "/storage/v1/object/public/";
            int idx = oldUrl.indexOf(marker);
            if (idx < 0)
                return;
            String bucketAndPath = oldUrl.substring(idx + marker.length());

            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketAndPath;
            HttpURLConnection conn = (HttpURLConnection) URI.create(deleteUrl).toURL().openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + uploadKey);
            conn.setRequestProperty("apikey", anonKey);
            int code = conn.getResponseCode();
            System.out.println("[Storage] Delete " + (code == 200 ? "OK" : "status=" + code) + " → " + bucketAndPath);
        } catch (Exception e) {
            System.err.println("[Storage] Delete error (non-fatal): " + e.getMessage());
        }
    }

    // ======================== INTERNAL ========================

    private String uploadAvatarInternal(String bucket, File imageFile, String profileId) {
        validate(imageFile);
        String ext = getExtension(imageFile.getName());
        String safeName = sanitizeFileName(imageFile.getName());
        String path = "avatars/" + profileId + "/" + System.currentTimeMillis() + "_" + safeName;
        return uploadFile(bucket, imageFile, path);
    }

    /** Core upload via Supabase Storage REST API */
    public String uploadFile(String bucketName, File localFile, String filePath) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            System.err.println("[Storage] Supabase URL not configured — returning local URI.");
            return localFile.toURI().toString();
        }
        if (uploadKey == null || uploadKey.isBlank()) {
            System.err.println("[Storage] No API key configured — returning local URI.");
            return localFile.toURI().toString();
        }

        try {
            String mimeType = Files.probeContentType(localFile.toPath());
            if (mimeType == null)
                mimeType = "application/octet-stream";

            // Supabase Storage REST: POST /storage/v1/object/{bucket}/{path}
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filePath;
            System.out.println("[Storage] Uploading to: " + uploadUrl);
            System.out.println(
                    "[Storage] File: " + localFile.getName() + " (" + localFile.length() + " bytes, " + mimeType + ")");

            HttpURLConnection conn = (HttpURLConnection) URI.create(uploadUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + uploadKey);
            conn.setRequestProperty("apikey", anonKey);
            conn.setRequestProperty("Content-Type", mimeType);
            conn.setRequestProperty("x-upsert", "true");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            // Write file bytes
            try (OutputStream os = conn.getOutputStream();
                    FileInputStream fis = new FileInputStream(localFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + filePath;
                System.out.println("[Storage] ✅ Upload OK → " + publicUrl);
                return publicUrl;
            } else {
                InputStream errStream = conn.getErrorStream();
                String err = errStream != null ? new String(errStream.readAllBytes()) : "Unknown error";
                System.err.println("[Storage] ❌ Upload FAILED (HTTP " + code + "): " + err);
                System.err.println("[Storage] URL was: " + uploadUrl);
                return null;
            }
        } catch (Exception e) {
            System.err.println("[Storage] ❌ Upload exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ======================== VALIDATION ========================

    private void validate(File file) {
        if (file == null || !file.exists())
            throw new IllegalArgumentException("File does not exist.");
        if (file.length() > MAX_AVATAR_SIZE)
            throw new IllegalArgumentException("File too large. Maximum 5MB.");
        String ext = getExtension(file.getName()).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new IllegalArgumentException("Invalid file type. Allowed: PNG, JPG, JPEG, WEBP.");
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".png";
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ======================== INSTRUCTOR DOCUMENTS ========================

    private static final long MAX_DOCUMENT_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final long MAX_CV_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_CV_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");

    /**
     * Upload instructor verification document (ID front/back, certificate) to
     * instructor-documents bucket.
     * 
     * @param docType one of: "id_front", "id_back", "experience_certificate"
     */
    public String uploadInstructorDocument(File file, String profileId, String docType) {
        validateDocument(file);
        String ext = getExtension(file.getName());
        String path = "docs/" + profileId + "/" + docType + "_" + System.currentTimeMillis() + ext;
        System.out.println("[Storage] Uploading doc: bucket=instructor-avatars, path=" + path);
        String url = uploadFile("instructor-avatars", file, path);
        System.out.println("[Storage] Doc upload result: " + url);
        return url;
    }

    /**
     * Upload instructor CV to instructor-cv bucket. Accepts PDF, DOC, DOCX up to
     * 10MB.
     */
    public String uploadInstructorCV(File file, String profileId) {
        validateCV(file);
        String ext = getExtension(file.getName());
        String path = "cv/" + profileId + "/cv_" + System.currentTimeMillis() + ext;
        System.out.println("[Storage] Uploading CV: bucket=instructor-avatars, path=" + path);
        String url = uploadFile("instructor-avatars", file, path);
        System.out.println("[Storage] CV upload result: " + url);
        return url;
    }

    /** Validate document file (image: PNG/JPG/JPEG/WEBP, max 5MB) */
    public void validateDocument(File file) {
        if (file == null || !file.exists())
            throw new IllegalArgumentException("File does not exist.");
        if (file.length() > MAX_DOCUMENT_SIZE)
            throw new IllegalArgumentException("Document too large. Maximum 5MB.");
        String ext = getExtension(file.getName()).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new IllegalArgumentException("Invalid document type. Allowed: PNG, JPG, JPEG, WEBP.");
    }

    /** Validate CV file (PDF/DOC/DOCX, max 10MB) */
    public void validateCV(File file) {
        if (file == null || !file.exists())
            throw new IllegalArgumentException("File does not exist.");
        if (file.length() > MAX_CV_SIZE)
            throw new IllegalArgumentException("CV too large. Maximum 10MB.");
        String ext = getExtension(file.getName()).toLowerCase();
        if (!ALLOWED_CV_EXTENSIONS.contains(ext) && !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Invalid CV type. Allowed: PDF, DOC, DOCX, PNG, JPG.");
        }
    }
}
