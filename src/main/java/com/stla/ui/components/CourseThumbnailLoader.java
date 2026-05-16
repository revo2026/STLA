package com.stla.ui.components;

import com.stla.app.AppConfig;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Async thumbnail loader with cache. Downloads image bytes over HTTP so Supabase
 * URLs and relative storage paths work reliably in JavaFX.
 */
public final class CourseThumbnailLoader {

    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();
    private static final String THUMBNAIL_BUCKET = "course-thumbnails";

    private CourseThumbnailLoader() {}

    public static Image getCached(String url) {
        String resolved = resolveUrl(url);
        return resolved != null ? CACHE.get(resolved) : null;
    }

    public static void loadWithPlaceholder(String url, ImageView target, Node placeholder) {
        if (target == null || placeholder == null) {
            return;
        }
        placeholder.setVisible(true);
        placeholder.setManaged(true);
        target.setVisible(false);
        target.setImage(null);

        load(url, target,
                () -> {
                    placeholder.setVisible(false);
                    placeholder.setManaged(false);
                    target.setVisible(true);
                },
                () -> {
                    placeholder.setVisible(true);
                    placeholder.setManaged(true);
                    target.setVisible(false);
                });
    }

    public static void load(String url, ImageView target, Runnable onSuccess, Runnable onFailure) {
        String resolved = resolveUrl(url);
        if (resolved == null || target == null) {
            if (onFailure != null) Platform.runLater(onFailure);
            return;
        }

        Image cached = CACHE.get(resolved);
        if (cached != null && !cached.isError()) {
            Platform.runLater(() -> applyImage(target, cached, onSuccess));
            return;
        }

        Task<byte[]> task = new Task<>() {
            @Override protected byte[] call() throws Exception {
                return downloadBytes(resolved);
            }
        };
        task.setOnSucceeded(e -> {
            byte[] data = task.getValue();
            if (data == null || data.length == 0) {
                Platform.runLater(() -> { if (onFailure != null) onFailure.run(); });
                return;
            }
            Platform.runLater(() -> {
                try {
                    Image img = new Image(new ByteArrayInputStream(data));
                    if (img.isError() || img.getWidth() <= 0) {
                        if (onFailure != null) onFailure.run();
                        return;
                    }
                    CACHE.put(resolved, img);
                    applyImage(target, img, onSuccess);
                } catch (Exception ex) {
                    System.err.println("[Thumbnail] FX image error: " + ex.getMessage());
                    if (onFailure != null) onFailure.run();
                }
            });
        });
        task.setOnFailed(e -> {
            Throwable err = task.getException();
            System.err.println("[Thumbnail] Download failed for " + resolved + ": "
                    + (err != null ? err.getMessage() : "unknown"));
            Platform.runLater(() -> { if (onFailure != null) onFailure.run(); });
        });
        new Thread(task, "course-thumbnail-loader").start();
    }

    private static byte[] downloadBytes(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(20_000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "STLA-Desktop/1.0");
        conn.setRequestProperty("Accept", "image/*,*/*");

        if (url.contains("supabase.co") || url.contains("supabase.in")) {
            String anon = AppConfig.getInstance().getSupabaseAnonKey();
            if (anon != null && !anon.isBlank()) {
                conn.setRequestProperty("apikey", anon);
                conn.setRequestProperty("Authorization", "Bearer " + anon);
            }
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code);
        }
        try (InputStream in = conn.getInputStream()) {
            return in.readAllBytes();
        } finally {
            conn.disconnect();
        }
    }

    private static void applyImage(ImageView target, Image image, Runnable onSuccess) {
        target.setImage(image);
        target.setVisible(true);
        if (onSuccess != null) onSuccess.run();
    }

    /**
     * Turns DB values into a loadable URL (full HTTPS, storage path, or bucket-relative path).
     */
    public static String resolveUrl(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String base = AppConfig.getInstance().getSupabaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;

        if (trimmed.startsWith("/storage/v1/object/public/")) {
            return base + trimmed;
        }
        if (trimmed.startsWith("storage/v1/object/public/")) {
            return base + "/" + trimmed;
        }
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.startsWith(THUMBNAIL_BUCKET + "/")) {
            return base + "/storage/v1/object/public/" + trimmed;
        }
        return base + "/storage/v1/object/public/" + THUMBNAIL_BUCKET + "/" + trimmed;
    }

    /** @deprecated use {@link #resolveUrl(String)} */
    public static String normalizeUrl(String url) {
        return resolveUrl(url);
    }
}
