package com.stla.ui.components;

import com.stla.app.AppConfig;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin intro video preview using native {@link VideoPlayerCard} (JavaFX MediaPlayer).
 * YouTube / Vimeo / TikTok links open in the browser; uploaded MP4/Supabase files play in-app.
 */
public class IntroVideoPane extends StackPane {

    private static final Pattern YOUTUBE_ID = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?.*v=|embed/|v/|shorts/)|youtu\\.be/)([\\w-]{11})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VIMEO_ID = Pattern.compile("vimeo\\.com/(?:video/)?(\\d+)");

    private final VideoPlayerCard videoPlayer;
    private final ProgressIndicator spinner;
    private final VBox fallbackBox;

    private Path tempFile;
    private String lastResolvedUrl;
    private String rawSourceUrl;
    private PauseTransition loadTimeout;

    public IntroVideoPane() {
        setMinHeight(300);
        setPrefHeight(400);
        setMaxHeight(520);
        setStyle("-fx-background-color: #111827; -fx-background-radius: 12;");

        videoPlayer = new VideoPlayerCard();
        videoPlayer.setMinHeight(280);
        videoPlayer.setPrefHeight(380);
        videoPlayer.setMaxHeight(500);

        videoPlayer.setOnPlaybackReady(this::cancelLoadTimeout);
        videoPlayer.setOnPlaybackError(msg -> Platform.runLater(() -> onMediaPlayerError(msg)));

        spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        spinner.setVisible(false);
        spinner.setManaged(false);

        fallbackBox = new VBox(12);
        fallbackBox.setAlignment(Pos.CENTER);
        fallbackBox.setVisible(false);
        fallbackBox.setManaged(false);

        getChildren().addAll(videoPlayer, spinner, fallbackBox);
    }

    public void load(String rawUrl) {
        disposePlayback();
        rawSourceUrl = rawUrl;

        String normalized = normalizeWebUrl(rawUrl);
        String browserUrl = normalized != null ? normalized : rawUrl;

        if (browserUrl != null && isExternalVideoLink(browserUrl)) {
            lastResolvedUrl = toWatchUrl(browserUrl);
            showBrowserOnlyPanel(platformLabel(browserUrl),
                "This intro is hosted on " + platformLabel(browserUrl)
                    + ". Open it in your browser to watch.",
                lastResolvedUrl);
            return;
        }

        lastResolvedUrl = resolveVideoUrl(rawUrl);
        if (lastResolvedUrl == null || lastResolvedUrl.isBlank()) {
            showBrowserOnlyPanel("No intro video", "The instructor has not added an intro video.", null);
            return;
        }

        prepareAndPlayFile(lastResolvedUrl);
    }

    public void dispose() {
        cancelLoadTimeout();
        disposePlayback();
        hideFallback();
        videoPlayer.setVisible(true);
        videoPlayer.setManaged(true);
    }

    private void prepareAndPlayFile(String url) {
        hideFallback();
        videoPlayer.setVisible(true);
        videoPlayer.setManaged(true);
        showLoading("Preparing video…");
        startLoadTimeout();

        Task<PlaybackFile> task = new Task<>() {
            @Override protected PlaybackFile call() throws Exception {
                byte[] data = downloadVideoWithFallbacks(url);
                validateVideoBytes(data);
                String ext = detectExtension(data, url);
                String mime = mimeForExtension(ext);
                Path file = Files.createTempFile("stla-intro-", ext);
                Files.write(file, data);
                String playUrl = LocalVideoHttpServer.serve(file, mime);
                return new PlaybackFile(file, playUrl);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            PlaybackFile pf = task.getValue();
            playLocalUrl(pf.playUrl(), pf.file());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable err = task.getException();
            if (err instanceof YoutubeWatchException yt) {
                lastResolvedUrl = yt.watchUrl();
                showBrowserOnlyPanel("YouTube intro video",
                    "This intro is a YouTube link. Watch it in your browser.", yt.watchUrl());
                return;
            }
            if (err instanceof TikTokLinkException tt) {
                lastResolvedUrl = tt.pageUrl();
                showBrowserOnlyPanel("TikTok intro video",
                    "TikTok videos cannot play inside the app. Watch in your browser.", tt.pageUrl());
                return;
            }
            String msg = err != null ? err.getMessage() : "Could not load video";
            System.err.println("[IntroVideo] Download failed: " + msg);
            showPlaybackFailed(msg);
        }));
        new Thread(task, "intro-video-download").start();
    }

    private void playLocalUrl(String playUrl, Path localFile) {
        tempFile = localFile;
        System.out.println("[IntroVideo] MediaPlayer: " + playUrl);
        videoPlayer.loadVideo(playUrl);
    }

    private void onMediaPlayerError(String detail) {
        cancelLoadTimeout();
        hideLoading();
        String msg = detail != null && !detail.isBlank()
            ? detail
            : "This video could not be played in the app (use H.264 MP4 for uploads).";
        showPlaybackFailed(msg);
    }

    private void showPlaybackFailed(String detail) {
        cancelLoadTimeout();
        videoPlayer.dispose();
        videoPlayer.setVisible(false);
        videoPlayer.setManaged(false);
        showBrowserOnlyPanel(
            "Video unavailable",
            detail + "\n\nYou can open the original link in your browser instead.",
            lastResolvedUrl != null ? lastResolvedUrl : rawSourceUrl);
    }

    private void showLoading(String message) {
        hideFallback();
        videoPlayer.setVisible(true);
        videoPlayer.setManaged(true);
        spinner.setVisible(true);
        spinner.setManaged(true);
        videoPlayer.showPlaceholder("Loading video…", message);
    }

    private void hideLoading() {
        spinner.setVisible(false);
        spinner.setManaged(false);
    }

    private void hideFallback() {
        fallbackBox.setVisible(false);
        fallbackBox.setManaged(false);
        fallbackBox.getChildren().clear();
    }

    private void showBrowserOnlyPanel(String title, String subtitle, String browserUrl) {
        cancelLoadTimeout();
        hideLoading();
        disposePlayback();
        videoPlayer.setVisible(false);
        videoPlayer.setManaged(false);

        fallbackBox.getChildren().clear();
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label s = new Label(subtitle);
        s.setWrapText(true);
        s.setMaxWidth(520);
        s.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-text-alignment: center;");
        fallbackBox.getChildren().addAll(t, s);

        if (browserUrl != null && !browserUrl.isBlank()) {
            Button openBtn = new Button("Open video in browser");
            openBtn.getStyleClass().add("btn-primary");
            openBtn.setStyle("-fx-padding: 10 24; -fx-font-size: 14px;");
            openBtn.setOnAction(e -> openBrowser(browserUrl));

            Button retry = new Button("Retry");
            retry.getStyleClass().add("btn-outline");
            retry.setOnAction(e -> load(rawSourceUrl != null ? rawSourceUrl : browserUrl));

            javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(12, openBtn, retry);
            actions.setAlignment(Pos.CENTER);
            fallbackBox.getChildren().add(actions);
        }

        fallbackBox.setVisible(true);
        fallbackBox.setManaged(true);
    }

    private void startLoadTimeout() {
        cancelLoadTimeout();
        loadTimeout = new PauseTransition(Duration.seconds(30));
        loadTimeout.setOnFinished(e -> Platform.runLater(() -> {
            if (spinner.isVisible()) {
                showPlaybackFailed("Video took too long to load.");
            }
        }));
        loadTimeout.playFromStart();
    }

    private void cancelLoadTimeout() {
        hideLoading();
        if (loadTimeout != null) {
            loadTimeout.stop();
            loadTimeout = null;
        }
    }

    private void disposePlayback() {
        cancelLoadTimeout();
        videoPlayer.dispose();
        LocalVideoHttpServer.stop();
        deleteTempFile();
    }

    private void deleteTempFile() {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception ignored) {}
            tempFile = null;
        }
    }

    // ——— Download helpers (same as before, no WebView) ———

    private record PlaybackFile(Path file, String playUrl) {}

    private static byte[] downloadVideoWithFallbacks(String primaryUrl) throws Exception {
        List<String> candidates = buildDownloadCandidates(primaryUrl);
        Exception lastError = null;

        for (String candidate : candidates) {
            if (isTikTokUrl(candidate)) {
                throw new TikTokLinkException(candidate);
            }
            try {
                System.out.println("[IntroVideo] Trying: " + candidate);
                byte[] data = fetchUrl(candidate);
                if (isVideoBytes(data)) {
                    System.out.println("[IntroVideo] OK " + data.length + " bytes");
                    return data;
                }
                String ytId = extractYoutubeVideoId(candidate);
                if (ytId == null) {
                    ytId = extractYoutubeVideoIdFromHtml(
                        new String(data, 0, Math.min(data.length, 8000), StandardCharsets.UTF_8));
                }
                if (ytId != null) {
                    throw new YoutubeWatchException("https://www.youtube.com/watch?v=" + ytId);
                }
                lastError = new IllegalStateException("URL returned a web page, not a video file.");
            } catch (YoutubeWatchException | TikTokLinkException e) {
                throw e;
            } catch (Exception e) {
                lastError = e;
                System.err.println("[IntroVideo] Attempt failed: " + e.getMessage());
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("Could not download video.");
    }

    private static List<String> buildDownloadCandidates(String url) {
        Set<String> set = new LinkedHashSet<>();
        set.add(url);
        if (url.contains("/storage/v1/object/public/")) {
            set.add(url.replace("/storage/v1/object/public/", "/storage/v1/object/"));
        }
        return new ArrayList<>(set);
    }

    private static byte[] fetchUrl(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(300_000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "STLA-Desktop/1.0");
        conn.setRequestProperty("Accept", "video/*,application/octet-stream,*/*");

        if (url.contains("supabase.co") || url.contains("supabase.in")) {
            String auth = AppConfig.getInstance().getSupabaseServiceRoleKey();
            if (auth == null || auth.isBlank()) {
                auth = AppConfig.getInstance().getSupabaseAnonKey();
            }
            if (auth != null && !auth.isBlank()) {
                conn.setRequestProperty("apikey", auth);
                conn.setRequestProperty("Authorization", "Bearer " + auth);
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

    private static boolean isVideoBytes(byte[] data) {
        if (data == null || data.length < 12) return false;
        if (data[0] == '<' || data[0] == '{') return false;
        boolean mp4 = data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p';
        boolean webm = (data[0] & 0xFF) == 0x1A && (data[1] & 0xFF) == 0x45;
        boolean mov = data[4] == 'm' && data[5] == 'o' && data[6] == 'o' && data[7] == 'v';
        return mp4 || webm || mov;
    }

    private static void validateVideoBytes(byte[] data) {
        if (!isVideoBytes(data)) {
            throw new IllegalStateException("Downloaded file is not a valid video (MP4/WebM/MOV).");
        }
    }

    private static String detectExtension(byte[] data, String url) {
        if (data.length > 8 && data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p') return ".mp4";
        if ((data[0] & 0xFF) == 0x1A && (data[1] & 0xFF) == 0x45) return ".webm";
        if (data.length > 8 && data[4] == 'm' && data[5] == 'o' && data[6] == 'o' && data[7] == 'v') return ".mov";
        return guessExtension(url);
    }

    private static String guessExtension(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".webm")) return ".webm";
        if (lower.contains(".mov")) return ".mov";
        return ".mp4";
    }

    private static String mimeForExtension(String ext) {
        return switch (ext.toLowerCase()) {
            case ".webm" -> "video/webm";
            case ".mov" -> "video/quicktime";
            default -> "video/mp4";
        };
    }

    private static boolean isExternalVideoLink(String url) {
        String lower = url.toLowerCase();
        return lower.contains("youtube") || lower.contains("youtu.be")
            || lower.contains("vimeo.com") || isTikTokUrl(url);
    }

    private static boolean isTikTokUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("tiktok.com") || lower.contains("vt.tiktok.com")
            || lower.contains("vm.tiktok.com");
    }

    private static String platformLabel(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("tiktok")) return "TikTok";
        if (lower.contains("vimeo")) return "Vimeo";
        if (lower.contains("youtube") || lower.contains("youtu.be")) return "YouTube";
        return "External";
    }

    private static String toWatchUrl(String url) {
        String id = extractYoutubeVideoId(url);
        if (id != null) return "https://www.youtube.com/watch?v=" + id;
        Matcher vm = VIMEO_ID.matcher(url);
        if (vm.find()) return "https://vimeo.com/" + vm.group(1);
        return url;
    }

    private static String normalizeWebUrl(String url) {
        if (url == null) return null;
        String t = url.trim();
        if (t.isEmpty()) return null;
        if (t.startsWith("http://") || t.startsWith("https://")) return t;
        if (t.startsWith("www.") || t.contains("youtube") || t.contains("youtu.be")
                || t.contains("vimeo") || t.contains("tiktok")) {
            return "https://" + t;
        }
        return t;
    }

    private static String extractYoutubeVideoId(String url) {
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (host == null) return null;
            host = host.toLowerCase().replace("www.", "");

            if (host.equals("youtu.be")) {
                String path = uri.getPath();
                if (path != null && path.length() > 1) {
                    String id = path.substring(1).split("[/?&#]")[0];
                    if (id.length() == 11) return id;
                }
            }

            if (host.contains("youtube.com")) {
                String query = uri.getQuery();
                if (query != null) {
                    for (String part : query.split("&")) {
                        if (part.startsWith("v=") && part.length() > 2) {
                            String id = part.substring(2).split("[&#]")[0];
                            if (id.length() == 11) return id;
                        }
                    }
                }
                String path = uri.getPath();
                if (path != null) {
                    for (String prefix : new String[]{"/embed/", "/shorts/", "/v/", "/live/"}) {
                        if (path.contains(prefix)) {
                            String id = path.substring(path.indexOf(prefix) + prefix.length()).split("[/?&#]")[0];
                            if (id.length() == 11) return id;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        Matcher m = YOUTUBE_ID.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private static String extractYoutubeVideoIdFromHtml(String html) {
        if (html == null) return null;
        Matcher m = Pattern.compile("\"videoId\"\\s*:\\s*\"([\\w-]{11})\"").matcher(html);
        if (m.find()) return m.group(1);
        m = Pattern.compile("/embed/([\\w-]{11})").matcher(html);
        if (m.find()) return m.group(1);
        return extractYoutubeVideoId(html);
    }

    public static String resolveVideoUrl(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;

        String web = normalizeWebUrl(trimmed);
        if (web != null && web.startsWith("http") && isExternalVideoLink(web)) {
            return web;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String base = AppConfig.getInstance().getSupabaseUrl();
        if (base == null || base.isBlank()) return null;
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;

        if (trimmed.startsWith("/storage/v1/object/public/")) {
            return base + trimmed;
        }
        if (trimmed.startsWith("storage/v1/object/public/")) {
            return base + "/" + trimmed;
        }
        if (trimmed.startsWith("/storage/v1/object/")) {
            return base + trimmed;
        }
        if (trimmed.startsWith("storage/v1/object/")) {
            return base + "/" + trimmed;
        }
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return base + "/storage/v1/object/public/course-thumbnails/" + trimmed;
    }

    private static void openBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(URI.create(url.trim()));
            }
        } catch (Exception ignored) {}
    }

    private static final class YoutubeWatchException extends Exception {
        private final String watchUrl;
        YoutubeWatchException(String watchUrl) { this.watchUrl = watchUrl; }
        String watchUrl() { return watchUrl; }
    }

    private static final class TikTokLinkException extends Exception {
        private final String pageUrl;
        TikTokLinkException(String pageUrl) { this.pageUrl = pageUrl; }
        String pageUrl() { return pageUrl; }
    }
}
