package com.stla.ui.student;

import com.stla.core.navigation.StudentNavigationContext;
import com.stla.core.session.SessionManager;
import com.stla.data.repositories.CourseRepositoryImpl;
import com.stla.data.repositories.LessonProgressRepository;
import com.stla.domain.models.CourseLesson;
import com.stla.domain.models.CourseSection;
import com.stla.domain.models.LessonResource;
import com.stla.domain.models.StudentQuizSummary;
import com.stla.patterns.proxy.CourseAccessProxy;
import com.stla.services.EnrollmentService;
import com.stla.services.LearningProgressService;
import com.stla.services.LessonService;
import com.stla.services.QuizService;
import com.stla.services.ResourceDownloadService;
import com.stla.services.ResourceService;
import com.stla.ui.components.StudentQuizUiHelper;
import com.stla.ui.components.StudentResourceFileCard;
import com.stla.ui.components.ToastNotification;
import com.stla.ui.components.VideoPlayerCard;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class StudentCoursePlayerController {

    @FXML private BorderPane rootPane;
    @FXML private Button backButton;
    @FXML private Label courseTitleLabel;
    @FXML private Label lessonTitleLabel;
    @FXML private ProgressBar courseProgressBar;
    @FXML private Label lessonsCountLabel;
    @FXML private VBox lessonsList;
    @FXML private StackPane playerArea;
    @FXML private VBox lockedOverlay;
    @FXML private Label lockMessageLabel;
    @FXML private VBox emptyStateBox;
    @FXML private Label emptyStateLabel;
    @FXML private HBox lessonActionsBox;
    @FXML private Label watchProgressLabel;
    @FXML private Label completedBadge;
    @FXML private Button completeLessonBtn;
    @FXML private Button nextLessonBtn;
    @FXML private VBox resourcesPanel;
    @FXML private VBox resourcesList;

    private final LessonService lessonService = new LessonService();
    private final ResourceService resourceService = new ResourceService();
    private final ResourceDownloadService downloadService = new ResourceDownloadService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final CourseAccessProxy accessProxy = new CourseAccessProxy();
    private final QuizService quizService = new QuizService();
    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();
    private final LearningProgressService progressService = new LearningProgressService();
    private final LessonProgressRepository lessonProgressRepo = new LessonProgressRepository();

    private String courseId;
    private String courseTitle;
    private List<CourseSection> sections = new ArrayList<>();
    private List<CourseLesson> flatLessons = new ArrayList<>();
    private CourseLesson selectedLesson;
    private VideoPlayerCard videoPlayer;
    private Set<String> completedLessonIds = new HashSet<>();
    private Consumer<Void> onBack;
    private Consumer<String> onStartQuiz;
    private final Runnable stopVideoHandler = this::shutdown;

    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setCourseTitle(String title) { this.courseTitle = title; }
    public void setOnBack(Consumer<Void> onBack) { this.onBack = onBack; }
    public void setOnStartQuiz(Consumer<String> onStartQuiz) { this.onStartQuiz = onStartQuiz; }

    @FXML
    public void initialize() {
        lockMessageLabel.setText(accessProxy.getLockMessage());
        videoPlayer = new VideoPlayerCard();
        videoPlayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(videoPlayer, Pos.CENTER);
        playerArea.getChildren().add(0, videoPlayer);

        videoPlayer.setOnWatchProgress(wp -> {
            if (selectedLesson == null) return;
            String studentId = SessionManager.getInstance().getCurrentStudent().getId();
            progressService.recordWatchProgress(studentId, selectedLesson.getId(),
                    (int) wp.currentSeconds(), (int) wp.totalSeconds());
            Platform.runLater(() -> {
                watchProgressLabel.setText(String.format("Watched: %.0f%%", wp.percentWatched()));
                refreshLessonActions(studentId);
            });
        });
    }

    public void start() {
        if (courseId == null) {
            courseId = StudentNavigationContext.getSelectedCourseId();
        }
        if (courseId == null) {
            showEmptyState("No course selected");
            return;
        }
        if (courseTitle == null || courseTitle.isBlank()) {
            courseRepo.findById(courseId).ifPresent(c -> courseTitle = c.getTitle());
        }
        if (courseTitle != null) {
            courseTitleLabel.setText(courseTitle);
        }
        StudentNavigationContext.registerVideoStopHandler(stopVideoHandler);
        loadCourseContent();
    }

    public void shutdown() {
        StudentNavigationContext.unregisterVideoStopHandler(stopVideoHandler);
        if (videoPlayer != null) {
            videoPlayer.dispose();
        }
    }

    private void loadCourseContent() {
        String studentId = SessionManager.getInstance().getCurrentStudent().getId();
        Task<List<CourseSection>> task = new Task<>() {
            @Override protected List<CourseSection> call() {
                completedLessonIds = lessonProgressRepo.findCompletedLessonIds(studentId, courseId);
                return lessonService.getSections(courseId);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            sections = task.getValue() != null ? task.getValue() : List.of();
            rebuildFlatLessons();
            refreshCourseHeader(studentId);
            buildLessonSidebar(studentId);
            selectFirstPlayableLesson(studentId);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showEmptyState("Failed to load lessons")));
        new Thread(task).start();
    }

    private void rebuildFlatLessons() {
        flatLessons.clear();
        String studentId = SessionManager.getInstance().getCurrentStudent().getId();
        boolean enrolled = enrollmentService.isEnrolled(studentId, courseId);
        for (CourseSection section : sections) {
            if (section.getLessons() == null) continue;
            for (CourseLesson lesson : section.getLessons()) {
                if (!enrolled && !lesson.isPreview()) continue;
                flatLessons.add(lesson);
            }
        }
    }

    private void refreshCourseHeader(String studentId) {
        var snapshot = progressService.getCourseProgress(studentId, courseId);
        if (snapshot != null) {
            courseProgressBar.setProgress(snapshot.progressPercent() / 100.0);
            lessonsCountLabel.setText(snapshot.lessonsCompleted() + " / " + snapshot.totalLessons() + " lessons");
        } else {
            int total = progressService.countPublishedLessons(courseId);
            courseProgressBar.setProgress(0);
            lessonsCountLabel.setText("0 / " + total + " lessons");
        }
    }

    private void buildLessonSidebar(String studentId) {
        lessonsList.getChildren().clear();
        boolean enrolled = enrollmentService.isEnrolled(studentId, courseId);

        if (!enrolled) {
            Button enrollCta = new Button("Enroll to Access Full Course");
            enrollCta.getStyleClass().addAll("btn-accent", "btn-sm");
            enrollCta.setMaxWidth(Double.MAX_VALUE);
            enrollCta.setOnAction(e -> StudentNavigationContext.goToCheckout(courseId));
            lessonsList.getChildren().add(enrollCta);
        }

        int sectionIndex = 0;
        boolean hasLessons = false;
        for (CourseSection section : sections) {
            if (section.getLessons() == null || section.getLessons().isEmpty()) continue;
            sectionIndex++;
            hasLessons = true;

            Label sectionHeader = new Label("Section " + sectionIndex + ": " + section.getTitle());
            sectionHeader.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 4 4 4;");
            lessonsList.getChildren().add(sectionHeader);

            for (CourseLesson lesson : section.getLessons()) {
                if (!enrolled && !lesson.isPreview()) continue;
                lessonsList.getChildren().add(buildLessonRow(lesson, studentId));
            }
        }

        if (!hasLessons) {
            lessonsList.getChildren().add(new Label("No lessons published yet") {{
                setStyle("-fx-text-fill: #64748b; -fx-padding: 12;");
            }});
        }
        buildQuizSection(studentId);
    }

    private void buildQuizSection(String studentId) {
        Label quizHeader = new Label("🎯 Quizzes");
        quizHeader.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 16 4 8 4;");
        lessonsList.getChildren().add(quizHeader);

        new Thread(() -> {
            var summaries = quizService.getCourseQuizzes(courseId, studentId);
            Platform.runLater(() -> {
                if (summaries.isEmpty()) {
                    lessonsList.getChildren().add(new Label("No quizzes yet") {{
                        setStyle("-fx-text-fill: #64748b; -fx-padding: 8 12;");
                    }});
                    return;
                }
                for (StudentQuizSummary summary : summaries) {
                    VBox card = StudentQuizUiHelper.buildQuizCard(summary, quizId -> {
                        if (onStartQuiz != null) onStartQuiz.accept(quizId);
                    });
                    card.setMaxWidth(Double.MAX_VALUE);
                    lessonsList.getChildren().add(card);
                }
            });
        }).start();
    }

    private HBox buildLessonRow(CourseLesson lesson, String studentId) {
        boolean canAccess = accessProxy.canAccessLesson(studentId, courseId, lesson);
        boolean active = selectedLesson != null && selectedLesson.getId().equals(lesson.getId());
        boolean done = completedLessonIds.contains(lesson.getId());

        HBox row = new HBox(10);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("sidebar-item");
        if (active) row.setStyle("-fx-background-color: #334155; -fx-background-radius: 8;");

        String iconText = done ? "✓" : (canAccess ? "▶" : "🔒");
        Label icon = new Label(iconText);
        icon.setStyle("-fx-text-fill: " + (done ? "#34d399" : (canAccess ? "#a78bfa" : "#64748b")) + ";");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(lesson.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: " + (active ? "bold" : "normal") + ";");
        String metaText = (lesson.isPreview() ? "Preview" : (canAccess ? "Video lesson" : "Locked"))
                + " • " + formatDuration(lesson.getDurationSeconds())
                + (done ? " • Done" : "");
        Label meta = new Label(metaText);
        meta.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        info.getChildren().addAll(title, meta);
        row.getChildren().addAll(icon, info);

        if (!canAccess) row.setOpacity(0.75);
        row.setOnMouseClicked(e -> selectLesson(lesson, studentId));
        return row;
    }

    private void selectFirstPlayableLesson(String studentId) {
        for (CourseLesson lesson : flatLessons) {
            if (accessProxy.canAccessLesson(studentId, courseId, lesson)) {
                selectLesson(lesson, studentId);
                return;
            }
        }
        showEmptyState("No playable lessons yet");
    }

    private void selectLesson(CourseLesson lesson, String studentId) {
        if (videoPlayer != null && videoPlayer.isPlaying()) {
            videoPlayer.pause();
        }
        selectedLesson = lesson;
        lessonTitleLabel.setText(lesson.getTitle());
        buildLessonSidebar(studentId);

        boolean canAccess = accessProxy.canAccessLesson(studentId, courseId, lesson);
        lockedOverlay.setVisible(!canAccess);
        lockedOverlay.setManaged(!canAccess);
        emptyStateBox.setVisible(false);
        emptyStateBox.setManaged(false);
        lessonActionsBox.setVisible(canAccess);
        lessonActionsBox.setManaged(canAccess);
        videoPlayer.setVisible(canAccess);

        if (!canAccess) {
            videoPlayer.dispose();
            hideResourcesPanel();
            return;
        }

        loadLessonResources(lesson);

        String url = lesson.getVideoUrl();
        if (url != null && !url.isBlank()) {
            videoPlayer.loadVideo(url.trim());
            videoPlayer.setVisible(true);
            rootPane.requestFocus();
        } else {
            videoPlayer.showPlaceholder("No video for this lesson", "The instructor has not uploaded a video yet.");
            videoPlayer.setVisible(true);
        }
        refreshLessonActions(studentId);
    }

    private void refreshLessonActions(String studentId) {
        if (selectedLesson == null) return;
        boolean done = completedLessonIds.contains(selectedLesson.getId());
        boolean canComplete = progressService.canCompleteLesson(studentId, selectedLesson);

        completedBadge.setVisible(done);
        completedBadge.setManaged(done);
        completeLessonBtn.setVisible(!done && canComplete);
        completeLessonBtn.setManaged(!done && canComplete);

        int idx = indexOfLesson(selectedLesson);
        boolean hasNext = idx >= 0 && idx < flatLessons.size() - 1;
        nextLessonBtn.setVisible(hasNext && done);
        nextLessonBtn.setManaged(hasNext && done);

        if (!done && !canComplete) {
            watchProgressLabel.setText("Watch at least 90% to unlock completion");
        }
    }

    @FXML
    private void handleCompleteLesson() {
        if (selectedLesson == null) return;
        String studentId = SessionManager.getInstance().getCurrentStudent().getId();
        progressService.markLessonCompleted(studentId, selectedLesson);
        completedLessonIds.add(selectedLesson.getId());
        refreshCourseHeader(studentId);
        buildLessonSidebar(studentId);
        refreshLessonActions(studentId);
        showToast("Lesson completed! 🎉", ToastNotification.Type.SUCCESS);
    }

    @FXML
    private void handleNextLesson() {
        if (selectedLesson == null) return;
        int idx = indexOfLesson(selectedLesson);
        if (idx < 0 || idx >= flatLessons.size() - 1) return;
        String studentId = SessionManager.getInstance().getCurrentStudent().getId();
        selectLesson(flatLessons.get(idx + 1), studentId);
    }

    private int indexOfLesson(CourseLesson lesson) {
        for (int i = 0; i < flatLessons.size(); i++) {
            if (flatLessons.get(i).getId().equals(lesson.getId())) return i;
        }
        return -1;
    }

    private void loadLessonResources(CourseLesson lesson) {
        resourcesList.getChildren().clear();
        resourcesPanel.setVisible(false);
        resourcesPanel.setManaged(false);

        new Thread(() -> {
            List<LessonResource> resources = resourceService.getResources(lesson.getId());
            Platform.runLater(() -> buildResourcesList(resources));
        }).start();
    }

    private void buildResourcesList(List<LessonResource> resources) {
        resourcesList.getChildren().clear();
        if (resources == null || resources.isEmpty()) {
            hideResourcesPanel();
            return;
        }

        resourcesPanel.setVisible(true);
        resourcesPanel.setManaged(true);

        for (LessonResource resource : resources) {
            StudentResourceFileCard card = new StudentResourceFileCard(resource);
            card.setOnDownload(this::downloadResource);
            resourcesList.getChildren().add(card);
        }
    }

    private void hideResourcesPanel() {
        resourcesPanel.setVisible(false);
        resourcesPanel.setManaged(false);
        resourcesList.getChildren().clear();
    }

    private void downloadResource(LessonResource resource) {
        StudentResourceFileCard card = findResourceCard(resource.getId());
        if (card != null) card.setDownloading(true);

        String title = courseTitle != null ? courseTitle : "course";
        new Thread(() -> {
            try {
                var path = downloadService.download(resource, title);
                Platform.runLater(() -> {
                    if (card != null) card.setDownloading(false);
                    showToast("Downloaded to " + path.getParent(), ToastNotification.Type.SUCCESS);
                    openDownloadFolder(path.getParent());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (card != null) card.setDownloading(false);
                    showToast("Download failed: " + ex.getMessage(), ToastNotification.Type.ERROR);
                });
            }
        }).start();
    }

    private StudentResourceFileCard findResourceCard(String resourceId) {
        for (var node : resourcesList.getChildren()) {
            if (node instanceof StudentResourceFileCard card
                    && card.getResource().getId().equals(resourceId)) {
                return card;
            }
        }
        return null;
    }

    private void openDownloadFolder(java.nio.file.Path folder) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(folder.toFile());
            }
        } catch (Exception ignored) {}
    }

    private void showEmptyState(String message) {
        emptyStateLabel.setText(message);
        emptyStateBox.setVisible(true);
        emptyStateBox.setManaged(true);
        lockedOverlay.setVisible(false);
        lockedOverlay.setManaged(false);
        lessonActionsBox.setVisible(false);
        lessonActionsBox.setManaged(false);
        videoPlayer.setVisible(false);
        hideResourcesPanel();
    }

    private void showToast(String msg, ToastNotification.Type type) {
        try {
            if (rootPane.getScene() != null) {
                ToastNotification.show((Stage) rootPane.getScene().getWindow(), msg, type);
            }
        } catch (Exception ignored) {}
    }

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "—";
        int m = seconds / 60;
        int s = seconds % 60;
        return m > 0 ? m + " min" : s + " sec";
    }

    @FXML
    private void handleBack() {
        shutdown();
        if (onBack != null) onBack.accept(null);
    }
}
