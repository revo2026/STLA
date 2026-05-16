package com.stla.services;

import com.stla.app.AppConfig;
import com.stla.domain.models.LessonResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Downloads lesson resource files from storage URLs to the user's Downloads folder.
 */
public class ResourceDownloadService {

    public Path download(LessonResource resource, String courseTitle) throws IOException {
        if (resource == null || resource.getResourceUrl() == null || resource.getResourceUrl().isBlank()) {
            throw new IOException("Resource URL is missing.");
        }
        byte[] data = fetchBytes(resource.getResourceUrl());
        Path targetDir = resolveDownloadDir(courseTitle);
        Files.createDirectories(targetDir);
        Path file = targetDir.resolve(safeFileName(resource));
        Files.write(file, data);
        return file;
    }

    private static Path resolveDownloadDir(String courseTitle) {
        String userHome = System.getProperty("user.home");
        String folder = sanitize(courseTitle != null ? courseTitle : "STLA Course");
        return Paths.get(userHome, "Downloads", "STLA", folder);
    }

    private static String safeFileName(LessonResource resource) {
        String title = resource.getTitle();
        if (title == null || title.isBlank()) title = "resource";
        title = sanitize(title);
        String ext = resource.getResourceType();
        if (ext != null && !ext.isBlank() && !title.toLowerCase().endsWith("." + ext.toLowerCase())) {
            title = title + "." + ext.toLowerCase();
        }
        return title;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private byte[] fetchBytes(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(60_000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "STLA-Desktop/1.0");
        conn.setRequestProperty("Accept", "*/*");

        if (url.contains("supabase.co") || url.contains("supabase.in")) {
            String anon = AppConfig.getInstance().getSupabaseAnonKey();
            if (anon != null && !anon.isBlank()) {
                conn.setRequestProperty("apikey", anon);
                conn.setRequestProperty("Authorization", "Bearer " + anon);
            }
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            conn.disconnect();
            throw new IOException("Download failed (HTTP " + code + ")");
        }
        try (InputStream in = conn.getInputStream()) {
            return in.readAllBytes();
        } finally {
            conn.disconnect();
        }
    }
}
