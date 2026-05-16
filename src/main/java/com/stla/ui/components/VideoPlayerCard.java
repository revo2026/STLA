package com.stla.ui.components;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Professional video player with fullscreen support.
 * Preserves playback state across fullscreen transitions.
 */
public class VideoPlayerCard extends VBox {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private StackPane videoContainer;

    private Button playPauseBtn, muteBtn, speedBtn, fullscreenBtn;
    private Slider volumeSlider, timelineSlider;
    private Label currentTimeLabel, durationLabel;
    private ProgressIndicator bufferSpinner;
    private HBox controlsBar;
    private VBox controlsOverlay, placeholder;

    private boolean isPlaying = false, isMuted = false, isSeeking = false, isFullscreen = false;
    private double previousVolume = 0.7, currentSpeed = 1.0;
    private final double[] SPEEDS = {0.5, 1.0, 1.25, 1.5, 2.0};
    private String currentUrl;
    private Consumer<WatchProgress> watchProgressListener;
    private Runnable onPlaybackReady;
    private Consumer<String> onPlaybackError;
    private double maxWatchedSeconds;
    private long lastProgressNotifyMs;

    public record WatchProgress(double currentSeconds, double totalSeconds, double percentWatched) {}

    // Fullscreen state
    private Stage fullscreenStage;
    private PauseTransition hideTimer;

    public VideoPlayerCard() {
        getStyleClass().add("player-container");
        setMinHeight(400); setPrefHeight(480);
        buildUI();
    }

    private void buildUI() {
        videoContainer = new StackPane();
        videoContainer.getStyleClass().add("video-area");
        videoContainer.setStyle("-fx-background-color: #000000; -fx-background-radius: 16;");
        VBox.setVgrow(videoContainer, Priority.ALWAYS);

        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        videoContainer.widthProperty().addListener((o, ov, nv) -> { if (nv.doubleValue() > 0) mediaView.setFitWidth(nv.doubleValue()); });
        videoContainer.heightProperty().addListener((o, ov, nv) -> { if (nv.doubleValue() > 0) mediaView.setFitHeight(nv.doubleValue() - 60); });

        bufferSpinner = new ProgressIndicator();
        bufferSpinner.getStyleClass().add("player-spinner");
        bufferSpinner.setMaxSize(48, 48); bufferSpinner.setVisible(false);

        placeholder = new VBox(12); placeholder.setAlignment(Pos.CENTER); placeholder.getStyleClass().add("video-placeholder");
        placeholder.getChildren().addAll(
            new Label("🎬") {{ getStyleClass().add("video-placeholder-icon"); }},
            new Label("No video uploaded yet") {{ getStyleClass().add("video-placeholder-text"); }},
            new Label("Upload a video to preview it here") {{ setStyle("-fx-text-fill: #4B5563; -fx-font-size: 13px;"); }}
        );

        controlsOverlay = new VBox(); controlsOverlay.setAlignment(Pos.BOTTOM_CENTER);
        controlsOverlay.setPadding(new Insets(0, 12, 12, 12)); controlsOverlay.setPickOnBounds(false); controlsOverlay.setVisible(false);
        buildControls();
        controlsOverlay.getChildren().add(controlsBar);

        videoContainer.getChildren().addAll(mediaView, placeholder, bufferSpinner, controlsOverlay);
        StackPane.setAlignment(bufferSpinner, Pos.CENTER);
        StackPane.setAlignment(controlsOverlay, Pos.BOTTOM_CENTER);

        videoContainer.setOnMouseEntered(e -> showControls());
        videoContainer.setOnMouseExited(e -> { if (isPlaying) hideControlsDelayed(); });
        videoContainer.setOnMouseMoved(e -> { showControls(); if (isPlaying) hideControlsDelayed(); });
        videoContainer.setOnMouseClicked(e -> {
            if (!isClickOnControls(e)) handleVideoClick(e);
        });

        getChildren().add(videoContainer);
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) { togglePlayPause(); e.consume(); }
            else if (e.getCode() == KeyCode.M) { toggleMute(); e.consume(); }
            else if (e.getCode() == KeyCode.F) { toggleFullscreen(); e.consume(); }
            else if (e.getCode() == KeyCode.ESCAPE && isFullscreen) { exitFullscreen(); e.consume(); }
            else if (e.getCode() == KeyCode.LEFT) { seek(-10); e.consume(); }
            else if (e.getCode() == KeyCode.RIGHT) { seek(10); e.consume(); }
        });
    }

    private void buildControls() {
        controlsBar = new HBox(8); controlsBar.getStyleClass().add("player-controls-bar"); controlsBar.setAlignment(Pos.CENTER_LEFT);
        playPauseBtn = new Button("▶");
        playPauseBtn.getStyleClass().add("player-btn");
        playPauseBtn.setOnAction(e -> {
            e.consume();
            togglePlayPause();
        });
        muteBtn = new Button("🔊"); muteBtn.getStyleClass().add("player-btn"); muteBtn.setOnAction(e -> toggleMute());
        volumeSlider = new Slider(0, 1, 0.7); volumeSlider.getStyleClass().add("player-volume"); volumeSlider.setPrefWidth(80); volumeSlider.setMaxWidth(80);
        volumeSlider.valueProperty().addListener((o, ov, nv) -> { if (mediaPlayer != null) { mediaPlayer.setVolume(nv.doubleValue()); updateMuteIcon(nv.doubleValue()); } });
        currentTimeLabel = new Label("0:00"); currentTimeLabel.getStyleClass().add("player-time");
        timelineSlider = new Slider(0, 100, 0); timelineSlider.getStyleClass().add("player-timeline"); HBox.setHgrow(timelineSlider, Priority.ALWAYS);
        timelineSlider.setOnMousePressed(e -> isSeeking = true);
        timelineSlider.setOnMouseReleased(e -> { isSeeking = false; if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) mediaPlayer.seek(Duration.seconds(timelineSlider.getValue())); });
        durationLabel = new Label("0:00"); durationLabel.getStyleClass().add("player-time");
        speedBtn = new Button("1x"); speedBtn.getStyleClass().add("speed-btn"); speedBtn.setOnAction(e -> cycleSpeed());
        fullscreenBtn = new Button("⛶"); fullscreenBtn.getStyleClass().add("player-btn"); fullscreenBtn.setOnAction(e -> toggleFullscreen());
        controlsBar.getChildren().addAll(playPauseBtn, muteBtn, volumeSlider, currentTimeLabel, timelineSlider, durationLabel, speedBtn, fullscreenBtn);
    }

    // ==================== PUBLIC API ====================

    public void setOnWatchProgress(Consumer<WatchProgress> listener) {
        this.watchProgressListener = listener;
    }

    public void setOnPlaybackReady(Runnable handler) {
        this.onPlaybackReady = handler;
    }

    public void setOnPlaybackError(Consumer<String> handler) {
        this.onPlaybackError = handler;
    }

    public double getMaxWatchedPercent() {
        if (mediaPlayer == null || mediaPlayer.getTotalDuration() == null) return 0;
        double total = mediaPlayer.getTotalDuration().toSeconds();
        return total > 0 ? (maxWatchedSeconds / total) * 100.0 : 0;
    }

    public void loadVideo(String url) {
        dispose(); this.currentUrl = url;
        maxWatchedSeconds = 0;
        if (url == null || url.isBlank()) { showPlaceholder("No video uploaded yet", "Upload a video to preview it here"); return; }
        try {
            placeholder.setVisible(false); bufferSpinner.setVisible(true); controlsOverlay.setVisible(false);
            Media media = new Media(url);
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.setOnReady(() -> Platform.runLater(() -> {
                bufferSpinner.setVisible(false); controlsOverlay.setVisible(true);
                Duration total = mediaPlayer.getTotalDuration();
                timelineSlider.setMax(total.toSeconds()); durationLabel.setText(formatTime(total));
                if (onPlaybackReady != null) onPlaybackReady.run();
            }));
            mediaPlayer.setOnPlaying(() -> Platform.runLater(() -> { isPlaying = true; playPauseBtn.setText("⏸"); bufferSpinner.setVisible(false); }));
            mediaPlayer.setOnPaused(() -> Platform.runLater(() -> { isPlaying = false; playPauseBtn.setText("▶"); }));
            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> { isPlaying = false; playPauseBtn.setText("▶"); mediaPlayer.seek(Duration.ZERO); showControls(); }));
            mediaPlayer.setOnStalled(() -> Platform.runLater(() -> bufferSpinner.setVisible(true)));
            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                bufferSpinner.setVisible(false);
                String detail = mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : null;
                if (onPlaybackError != null) {
                    onPlaybackError.accept(detail != null ? detail : "Playback failed");
                } else {
                    showPlaceholder("⚠️ Playback Error", "Only MP4 (H.264) is supported.");
                }
            }));
            mediaPlayer.currentTimeProperty().addListener((o, ov, nv) -> {
                if (nv == null) return;
                double current = nv.toSeconds();
                if (!isSeeking && current > maxWatchedSeconds) {
                    maxWatchedSeconds = current;
                }
                if (!isSeeking) {
                    Platform.runLater(() -> {
                        timelineSlider.setValue(current);
                        currentTimeLabel.setText(formatTime(nv));
                    });
                }
                notifyWatchProgress();
            });
        } catch (Exception e) { bufferSpinner.setVisible(false); showPlaceholder("⚠️ Cannot Load Video", e.getMessage()); }
    }

    /** Pause playback (keeps media loaded). */
    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            Platform.runLater(() -> playPauseBtn.setText("▶"));
        }
    }

    /** Stop playback and reset position (keeps media loaded). */
    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
            Platform.runLater(() -> {
                playPauseBtn.setText("▶");
                currentTimeLabel.setText("0:00");
                timelineSlider.setValue(0);
            });
        }
    }

    public boolean isPlaying() { return isPlaying; }

    private void notifyWatchProgress() {
        if (watchProgressListener == null || mediaPlayer == null) return;
        Duration total = mediaPlayer.getTotalDuration();
        if (total == null || total.toSeconds() <= 0) return;
        long now = System.currentTimeMillis();
        if (now - lastProgressNotifyMs < 2000) return;
        lastProgressNotifyMs = now;
        double totalSec = total.toSeconds();
        double percent = Math.min(100.0, (maxWatchedSeconds / totalSec) * 100.0);
        WatchProgress wp = new WatchProgress(maxWatchedSeconds, totalSec, percent);
        Platform.runLater(() -> watchProgressListener.accept(wp));
    }

    public void dispose() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
            mediaView.setMediaPlayer(null);
        }
        isPlaying = false;
        playPauseBtn.setText("▶");
        currentTimeLabel.setText("0:00");
        durationLabel.setText("0:00");
        timelineSlider.setValue(0);
        if (isFullscreen) exitFullscreen();
    }

    public void showPlaceholder(String title, String subtitle) {
        placeholder.getChildren().clear();
        placeholder.getChildren().addAll(
            new Label("🎬") {{ getStyleClass().add("video-placeholder-icon"); }},
            new Label(title) {{ getStyleClass().add("video-placeholder-text"); }},
            new Label(subtitle) {{ setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-text-alignment: center;"); setWrapText(true); }}
        );
        placeholder.setVisible(true); controlsOverlay.setVisible(false);
    }

    // ==================== FULLSCREEN ====================

    public void toggleFullscreen() {
        if (isFullscreen) exitFullscreen();
        else enterFullscreen();
    }

    private void enterFullscreen() {
        if (mediaPlayer == null || isFullscreen) return;

        // Save state
        double currentTime = mediaPlayer.getCurrentTime().toSeconds();
        double volume = volumeSlider.getValue();
        boolean wasPlaying = isPlaying;

        // Remove mediaView from normal container
        videoContainer.getChildren().remove(mediaView);
        videoContainer.getChildren().remove(controlsOverlay);
        videoContainer.getChildren().remove(bufferSpinner);

        // Create fullscreen stage
        StackPane fsRoot = new StackPane();
        fsRoot.setStyle("-fx-background-color: #000000;");
        fsRoot.getStyleClass().add("player-fullscreen");

        // Bind mediaView to full screen
        var screenBounds = Screen.getPrimary().getVisualBounds();
        mediaView.setFitWidth(screenBounds.getWidth());
        mediaView.setFitHeight(screenBounds.getHeight() - 80);

        // Fullscreen controls overlay
        VBox fsControls = new VBox();
        fsControls.setAlignment(Pos.BOTTOM_CENTER);
        fsControls.setPadding(new Insets(0, 24, 24, 24));
        fsControls.setPickOnBounds(false);
        fsControls.getChildren().add(controlsBar);
        controlsBar.getStyleClass().add("player-controls-fullscreen");

        fullscreenBtn.setText("⛶"); // exit icon

        fsRoot.getChildren().addAll(mediaView, bufferSpinner, fsControls);
        StackPane.setAlignment(fsControls, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(bufferSpinner, Pos.CENTER);

        // Mouse move: show controls, hide after inactivity
        fsRoot.setOnMouseMoved(e -> {
            showControlsFS(fsControls);
            resetHideTimer(fsControls);
        });
        fsRoot.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) exitFullscreen();
            else { togglePlayPause(); requestFocus(); }
        });

        Scene fsScene = new Scene(fsRoot, screenBounds.getWidth(), screenBounds.getHeight());
        // Load stylesheets
        try {
            fsScene.getStylesheets().addAll(
                getClass().getResource("/com/stla/css/player.css").toExternalForm(),
                getClass().getResource("/com/stla/css/app.css").toExternalForm()
            );
        } catch (Exception ignored) {}

        // ESC key
        fsScene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) exitFullscreen();
            else if (e.getCode() == KeyCode.SPACE) togglePlayPause();
            else if (e.getCode() == KeyCode.M) toggleMute();
            else if (e.getCode() == KeyCode.LEFT) seek(-10);
            else if (e.getCode() == KeyCode.RIGHT) seek(10);
            else if (e.getCode() == KeyCode.F) exitFullscreen();
        });

        fullscreenStage = new Stage();
        fullscreenStage.initStyle(StageStyle.UNDECORATED);
        fullscreenStage.setScene(fsScene);
        fullscreenStage.setFullScreen(true);
        fullscreenStage.setFullScreenExitHint("Press ESC to exit fullscreen");
        fullscreenStage.setOnCloseRequest(e -> exitFullscreen());
        fullscreenStage.show();

        isFullscreen = true;

        // Restore playback state
        mediaPlayer.seek(Duration.seconds(currentTime));
        volumeSlider.setValue(volume);
        if (wasPlaying && !isPlaying) mediaPlayer.play();

        // Start hide timer
        resetHideTimer(fsControls);
    }

    private void exitFullscreen() {
        if (!isFullscreen || fullscreenStage == null) return;

        // Save state
        double currentTime = mediaPlayer != null ? mediaPlayer.getCurrentTime().toSeconds() : 0;
        double volume = volumeSlider.getValue();
        boolean wasPlaying = isPlaying;

        // Get the fullscreen root and remove items
        StackPane fsRoot = (StackPane) fullscreenStage.getScene().getRoot();
        fsRoot.getChildren().clear();

        // Close fullscreen stage
        fullscreenStage.close();
        fullscreenStage = null;
        isFullscreen = false;

        // Cancel hide timer
        if (hideTimer != null) { hideTimer.stop(); hideTimer = null; }

        // Restore controlsBar style
        controlsBar.getStyleClass().remove("player-controls-fullscreen");
        fullscreenBtn.setText("⛶");

        // Restore MediaView to normal container
        mediaView.setFitWidth(videoContainer.getWidth());
        mediaView.setFitHeight(videoContainer.getHeight() - 60);
        videoContainer.widthProperty().addListener((o, ov, nv) -> { if (nv.doubleValue() > 0 && !isFullscreen) mediaView.setFitWidth(nv.doubleValue()); });
        videoContainer.heightProperty().addListener((o, ov, nv) -> { if (nv.doubleValue() > 0 && !isFullscreen) mediaView.setFitHeight(nv.doubleValue() - 60); });

        controlsOverlay.getChildren().clear();
        controlsOverlay.getChildren().add(controlsBar);

        videoContainer.getChildren().addAll(mediaView, bufferSpinner, controlsOverlay);
        StackPane.setAlignment(bufferSpinner, Pos.CENTER);
        StackPane.setAlignment(controlsOverlay, Pos.BOTTOM_CENTER);

        controlsOverlay.setVisible(true);
        controlsOverlay.setOpacity(1);

        // Restore playback
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(currentTime));
            volumeSlider.setValue(volume);
            if (wasPlaying && !isPlaying) mediaPlayer.play();
        }
    }

    private void showControlsFS(VBox fsControls) {
        fsControls.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), fsControls);
        ft.setToValue(1); ft.play();
    }

    private void resetHideTimer(VBox fsControls) {
        if (hideTimer != null) hideTimer.stop();
        hideTimer = new PauseTransition(Duration.seconds(3));
        hideTimer.setOnFinished(e -> {
            if (isPlaying && isFullscreen) {
                FadeTransition fade = new FadeTransition(Duration.millis(500), fsControls);
                fade.setToValue(0); fade.setOnFinished(ev -> fsControls.setVisible(false)); fade.play();
            }
        });
        hideTimer.play();
    }

    // ==================== CONTROLS ====================

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseBtn.setText("▶");
        } else {
            mediaPlayer.play();
            isPlaying = true;
            playPauseBtn.setText("⏸");
        }
    }

    private boolean isClickOnControls(MouseEvent e) {
        javafx.scene.Node node = e.getTarget() instanceof javafx.scene.Node n ? n : null;
        while (node != null) {
            if (node == controlsBar || node == controlsOverlay || node == playPauseBtn
                    || node == muteBtn || node == volumeSlider || node == timelineSlider
                    || node == speedBtn || node == fullscreenBtn) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        isMuted = !isMuted;
        if (isMuted) { previousVolume = volumeSlider.getValue(); volumeSlider.setValue(0); mediaPlayer.setVolume(0); muteBtn.setText("🔇"); }
        else { volumeSlider.setValue(previousVolume); mediaPlayer.setVolume(previousVolume); muteBtn.setText("🔊"); }
    }

    private void updateMuteIcon(double vol) { muteBtn.setText(vol == 0 ? "🔇" : vol < 0.5 ? "🔉" : "🔊"); }

    private void seek(double seconds) {
        if (mediaPlayer == null) return;
        Duration target = mediaPlayer.getCurrentTime().add(Duration.seconds(seconds));
        if (target.lessThan(Duration.ZERO)) target = Duration.ZERO;
        if (target.greaterThan(mediaPlayer.getTotalDuration())) target = mediaPlayer.getTotalDuration();
        mediaPlayer.seek(target);
    }

    private void cycleSpeed() {
        int idx = 0;
        for (int i = 0; i < SPEEDS.length; i++) if (Math.abs(SPEEDS[i] - currentSpeed) < 0.01) { idx = i; break; }
        idx = (idx + 1) % SPEEDS.length; currentSpeed = SPEEDS[idx];
        speedBtn.setText(currentSpeed == 1.0 ? "1x" : currentSpeed + "x");
        if (mediaPlayer != null) mediaPlayer.setRate(currentSpeed);
    }

    private void handleVideoClick(MouseEvent e) {
        if (e.getClickCount() == 2) toggleFullscreen();
        else togglePlayPause();
        requestFocus();
    }

    private void showControls() { controlsOverlay.setVisible(true); controlsOverlay.setOpacity(1); }

    private void hideControlsDelayed() {
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> {
            if (isPlaying && !isFullscreen) {
                FadeTransition fade = new FadeTransition(Duration.millis(400), controlsOverlay);
                fade.setFromValue(1); fade.setToValue(0); fade.setOnFinished(ev -> controlsOverlay.setVisible(false)); fade.play();
            }
        });
        delay.play();
    }

    private String formatTime(Duration d) {
        if (d == null || d.isUnknown() || d.isIndefinite()) return "0:00";
        int ts = (int) d.toSeconds(), h = ts / 3600, m = (ts % 3600) / 60, s = ts % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }
}
