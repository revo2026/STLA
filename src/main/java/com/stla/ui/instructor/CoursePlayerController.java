package com.stla.ui.instructor;

import com.stla.domain.models.*;
import com.stla.services.LessonService;
import com.stla.services.QuizService;
import com.stla.services.ResourceService;
import com.stla.ui.components.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Professional course player/editor controller.
 * Split layout: sidebar with section accordion | main area with video player + tabs.
 */
public class CoursePlayerController {

    @javafx.fxml.FXML private BorderPane rootPane;
    @javafx.fxml.FXML private VBox sidebarPane, tabContent;
    @javafx.fxml.FXML private VBox sectionsContainer;
    @javafx.fxml.FXML private VBox playerHeader;
    @javafx.fxml.FXML private StackPane videoPlayerContainer;
    @javafx.fxml.FXML private HBox tabBar, actionButtonsRow;
    @javafx.fxml.FXML private Label sidebarCourseTitle, sidebarCourseMeta;
    @javafx.fxml.FXML private Label breadcrumbLabel, lessonTitleLabel, lastUpdatedLabel, previewBadgeLabel;
    @javafx.fxml.FXML private Button backBtn, tabInfo, tabResources, tabQuiz;

    private final LessonService lessonService = new LessonService();
    private final ResourceService resourceService = new ResourceService();
    private final QuizService quizService = new QuizService();

    private Course course;
    private List<CourseSection> sections = new ArrayList<>();
    private CourseLesson selectedLesson;
    private VideoPlayerCard videoPlayer;
    private String activeTab = "info";
    private final Map<String, Boolean> expandedSections = new HashMap<>();

    @javafx.fxml.FXML
    public void initialize() {
        videoPlayer = new VideoPlayerCard();
        videoPlayerContainer.getChildren().add(videoPlayer);
    }

    public void setCourse(Course course) {
        this.course = course;
        sidebarCourseTitle.setText(course.getTitle());
        sidebarCourseMeta.setText(course.getEnrollmentCount() + " students enrolled");
        loadSections();
    }

    // ==================== DATA LOADING ====================

    private void loadSections() {
        if (course == null) return;
        Task<List<CourseSection>> task = new Task<>() {
            @Override protected List<CourseSection> call() {
                return lessonService.getSections(course.getId());
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    sections = getValue();
                    buildSidebar();
                    if (selectedLesson == null && !sections.isEmpty()) {
                        for (CourseSection s : sections) {
                            if (s.getLessons() != null && !s.getLessons().isEmpty()) {
                                selectLesson(s.getLessons().get(0), s);
                                break;
                            }
                        }
                    }
                    if (selectedLesson == null) showEmptyState();
                });
            }
        };
        new Thread(task).start();
    }

    // ==================== SIDEBAR ====================

    private void buildSidebar() {
        sectionsContainer.getChildren().clear();
        if (sections == null || sections.isEmpty()) {
            sectionsContainer.getChildren().add(
                ComponentFactory.createEmptyState("📂", "No sections yet.\nAdd a section to get started.")
            );
            return;
        }
        for (int i = 0; i < sections.size(); i++) {
            sectionsContainer.getChildren().add(buildSectionAccordion(sections.get(i), i + 1));
        }
        AnimationUtils.staggerFadeIn(sectionsContainer, 60);
    }

    private VBox buildSectionAccordion(CourseSection section, int index) {
        VBox card = new VBox(0);
        card.getStyleClass().add("section-accordion-card");
        boolean expanded = expandedSections.getOrDefault(section.getId(), true);

        // Header
        HBox header = new HBox(8);
        header.getStyleClass().add("section-accordion-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label chevron = new Label(expanded ? "▼" : "▶");
        chevron.getStyleClass().add("section-accordion-chevron");

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label title = new Label("Section " + index + ": " + section.getTitle());
        title.getStyleClass().add("section-accordion-title");

        int lessonCount = section.getLessons() != null ? section.getLessons().size() : 0;
        int totalDuration = section.getLessons() != null ?
            section.getLessons().stream().mapToInt(CourseLesson::getDurationSeconds).sum() : 0;
        Label meta = new Label(lessonCount + " lessons • " + formatDuration(totalDuration));
        meta.getStyleClass().add("section-accordion-meta");
        titleBox.getChildren().addAll(title, meta);

        // Section actions
        Button addLessonBtn = new Button("➕");
        addLessonBtn.getStyleClass().add("section-action-btn");
        addLessonBtn.setTooltip(new Tooltip("Add Lesson"));
        addLessonBtn.setOnAction(e -> { addLesson(section); e.consume(); });

        Button editBtn = new Button("✏");
        editBtn.getStyleClass().add("section-action-btn");
        editBtn.setTooltip(new Tooltip("Rename Section"));
        editBtn.setOnAction(e -> { editSection(section); e.consume(); });

        Button delBtn = new Button("🗑");
        delBtn.getStyleClass().add("section-action-btn");
        delBtn.setStyle("-fx-text-fill: #EF4444;");
        delBtn.setTooltip(new Tooltip("Delete Section"));
        delBtn.setOnAction(e -> { deleteSection(section); e.consume(); });

        header.getChildren().addAll(chevron, titleBox, addLessonBtn, editBtn, delBtn);

        // Body (lessons)
        VBox body = new VBox(0);
        body.getStyleClass().add("section-accordion-body");
        body.setVisible(expanded);
        body.setManaged(expanded);

        if (section.getLessons() != null) {
            for (CourseLesson lesson : section.getLessons()) {
                LessonCard lc = new LessonCard(lesson);
                lc.setOnSelect(l -> selectLesson(l, section));
                lc.setOnEdit(this::editLesson);
                lc.setOnDelete(this::deleteLesson);
                lc.setOnDuplicate(this::duplicateLesson);
                lc.setOnMoveUp(l -> moveLesson(l, section, -1));
                lc.setOnMoveDown(l -> moveLesson(l, section, 1));
                if (selectedLesson != null && lesson.getId().equals(selectedLesson.getId())) {
                    lc.setActive(true);
                }
                body.getChildren().add(lc);
            }
        }

        // Toggle expand
        header.setOnMouseClicked(e -> {
            boolean exp = !body.isVisible();
            body.setVisible(exp);
            body.setManaged(exp);
            chevron.setText(exp ? "▼" : "▶");
            expandedSections.put(section.getId(), exp);
        });

        card.getChildren().addAll(header, body);
        return card;
    }

    // ==================== LESSON SELECTION ====================

    private void selectLesson(CourseLesson lesson, CourseSection section) {
        this.selectedLesson = lesson;
        updatePlayerHeader(lesson, section);
        loadVideo(lesson);
        showTabContent(activeTab);
        buildSidebar(); // refresh active state
    }

    private void updatePlayerHeader(CourseLesson lesson, CourseSection section) {
        breadcrumbLabel.setText(course.getTitle() + "  ›  " + section.getTitle());
        lessonTitleLabel.setText(lesson.getTitle());
        if (lesson.getUpdatedAt() != null) {
            lastUpdatedLabel.setText("Updated " + lesson.getUpdatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        } else if (lesson.getCreatedAt() != null) {
            lastUpdatedLabel.setText("Created " + lesson.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        } else {
            lastUpdatedLabel.setText("");
        }
        previewBadgeLabel.setVisible(lesson.isPreview());
        previewBadgeLabel.setManaged(lesson.isPreview());
        if (lesson.isPreview()) previewBadgeLabel.setText("👁 Preview");
        actionButtonsRow.setVisible(true);
        actionButtonsRow.setManaged(true);
    }

    private void loadVideo(CourseLesson lesson) {
        String url = lesson.getVideoUrl();
        if (url != null && !url.isBlank()) {
            videoPlayer.loadVideo(url);
        } else {
            videoPlayer.showPlaceholder("No video uploaded", "Click 'Replace Video' to upload one");
        }
    }

    private void showEmptyState() {
        lessonTitleLabel.setText("No lessons yet");
        breadcrumbLabel.setText(course.getTitle());
        lastUpdatedLabel.setText("");
        actionButtonsRow.setVisible(false);
        actionButtonsRow.setManaged(false);
        videoPlayer.showPlaceholder("📚 Get Started", "Add a section and lessons to build your course");
        tabContent.getChildren().clear();
    }

    // ==================== TABS ====================

    @javafx.fxml.FXML private void handleTabInfo() { switchTab("info"); }
    @javafx.fxml.FXML private void handleTabResources() { switchTab("resources"); }
    @javafx.fxml.FXML private void handleTabQuiz() { switchTab("quiz"); }

    private void switchTab(String tab) {
        activeTab = tab;
        tabInfo.getStyleClass().remove("studio-tab-btn-active");
        tabResources.getStyleClass().remove("studio-tab-btn-active");
        tabQuiz.getStyleClass().remove("studio-tab-btn-active");
        switch (tab) {
            case "info" -> tabInfo.getStyleClass().add("studio-tab-btn-active");
            case "resources" -> tabResources.getStyleClass().add("studio-tab-btn-active");
            case "quiz" -> tabQuiz.getStyleClass().add("studio-tab-btn-active");
        }
        showTabContent(tab);
    }

    private void showTabContent(String tab) {
        tabContent.getChildren().clear();
        if (selectedLesson == null) return;
        switch (tab) {
            case "info" -> buildInfoTab();
            case "resources" -> buildResourcesTab();
            case "quiz" -> buildQuizTab();
        }
    }

    private void buildInfoTab() {
        Label titleLbl = new Label("Lesson Title");
        titleLbl.getStyleClass().add("form-label");
        TextField titleField = new TextField(selectedLesson.getTitle());
        titleField.getStyleClass().add("text-field");
        titleField.textProperty().addListener((o, ov, nv) -> selectedLesson.setTitle(nv));

        Label descLbl = new Label("Description");
        descLbl.getStyleClass().add("form-label");
        TextArea descField = new TextArea(selectedLesson.getDescription() != null ? selectedLesson.getDescription() : "");
        descField.getStyleClass().add("text-area");
        descField.setPrefRowCount(3);
        descField.textProperty().addListener((o, ov, nv) -> selectedLesson.setDescription(nv));

        HBox toggles = new HBox(24);
        toggles.setAlignment(Pos.CENTER_LEFT);
        CheckBox previewCb = new CheckBox("Preview Lesson");
        previewCb.setSelected(selectedLesson.isPreview());
        previewCb.selectedProperty().addListener((o, ov, nv) -> selectedLesson.setPreview(nv));
        CheckBox publishedCb = new CheckBox("Published");
        publishedCb.setSelected(selectedLesson.isPublished());
        publishedCb.selectedProperty().addListener((o, ov, nv) -> selectedLesson.setPublished(nv));
        toggles.getChildren().addAll(previewCb, publishedCb);

        HBox durationRow = new HBox(12);
        durationRow.setAlignment(Pos.CENTER_LEFT);
        Label durLbl = new Label("Duration (seconds):");
        durLbl.getStyleClass().add("form-label");
        Spinner<Integer> durSpinner = new Spinner<>(0, 36000, selectedLesson.getDurationSeconds(), 30);
        durSpinner.setEditable(true);
        durSpinner.setPrefWidth(120);
        durSpinner.valueProperty().addListener((o, ov, nv) -> selectedLesson.setDurationSeconds(nv));
        durationRow.getChildren().addAll(durLbl, durSpinner);

        tabContent.getChildren().addAll(titleLbl, titleField, descLbl, descField, toggles, durationRow);
    }

    private void buildResourcesTab() {
        new Thread(() -> {
            List<LessonResource> resources = resourceService.getResources(selectedLesson.getId());
            Platform.runLater(() -> {
                tabContent.getChildren().clear();
                Label heading = new Label("📁 Lesson Resources (" + resources.size() + ")");
                heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
                tabContent.getChildren().add(heading);

                for (LessonResource r : resources) {
                    ResourceFileCard card = new ResourceFileCard(r);
                    card.setOnRemove(res -> {
                        if (ComponentFactory.showConfirmDialog("Delete Resource", "Remove '" + res.getTitle() + "'?")) {
                            new Thread(() -> {
                                resourceService.deleteResource(res.getId());
                                Platform.runLater(() -> buildResourcesTab());
                            }).start();
                        }
                    });
                    tabContent.getChildren().add(card);
                }

                UploadDropZone dropZone = new UploadDropZone(
                    "Drop resource files here",
                    "PDF, DOCX, ZIP, Images — Max 50MB",
                    Set.of(".pdf", ".docx", ".doc", ".zip", ".png", ".jpg", ".jpeg", ".webp"),
                    50L * 1024 * 1024,
                    file -> uploadResource(file)
                );
                tabContent.getChildren().add(dropZone);
            });
        }).start();
    }

    private void buildQuizTab() {
        new Thread(() -> {
            Quiz quiz = lessonService.getLessonQuiz(selectedLesson.getId());
            Platform.runLater(() -> {
                tabContent.getChildren().clear();
                if (quiz != null) {
                    VBox quizCard = new VBox(8);
                    quizCard.getStyleClass().add("card");
                    quizCard.setPadding(new Insets(16));
                    Label qTitle = new Label("📝 " + quiz.getTitle());
                    qTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                    Label qDesc = new Label(quiz.getDescription() != null ? quiz.getDescription() : "");
                    qDesc.getStyleClass().add("text-secondary");
                    qDesc.setWrapText(true);

                    HBox badges = new HBox(8);
                    badges.setAlignment(Pos.CENTER_LEFT);
                    Label scoreBadge = ComponentFactory.createStatusBadge("Pass: " + quiz.getPassingScore() + "%", "info");
                    Label attemptsBadge = ComponentFactory.createStatusBadge("Attempts: " + quiz.getAttemptsAllowed(), "info");
                    Label statusBadge = ComponentFactory.createStatusBadge(quiz.isPublished() ? "Published" : "Draft",
                        quiz.isPublished() ? "success" : "warning");
                    badges.getChildren().addAll(scoreBadge, attemptsBadge, statusBadge);

                    HBox qActions = new HBox(8);
                    qActions.setPadding(new Insets(8, 0, 0, 0));
                    Button editQ = new Button("✏ Edit Quiz");
                    editQ.getStyleClass().addAll("btn-outline", "btn-sm");
                    editQ.setOnAction(e -> openQuizBuilder(quiz));
                    Button removeQ = new Button("🗑 Remove Quiz");
                    removeQ.getStyleClass().addAll("btn-outline", "btn-sm");
                    removeQ.setStyle("-fx-text-fill: #EF4444;");
                    removeQ.setOnAction(e -> {
                        if (ComponentFactory.showConfirmDialog("Remove Quiz", "Delete quiz '" + quiz.getTitle() + "'?")) {
                            new Thread(() -> {
                                quizService.deleteQuiz(quiz.getId());
                                Platform.runLater(this::buildQuizTab);
                            }).start();
                            showToast("Quiz removed", ToastNotification.Type.SUCCESS);
                        }
                    });
                    qActions.getChildren().addAll(editQ, removeQ);
                    quizCard.getChildren().addAll(qTitle, qDesc, badges, qActions);
                    tabContent.getChildren().add(quizCard);
                } else {
                    VBox empty = ComponentFactory.createEmptyState("📝", "No quiz attached to this lesson");
                    Button createQ = new Button("➕ Create Quiz");
                    createQ.getStyleClass().addAll("btn-primary", "btn-sm");
                    createQ.setOnAction(e -> createQuizForLesson());
                    HBox btnRow = new HBox(createQ);
                    btnRow.setAlignment(Pos.CENTER);
                    btnRow.setPadding(new Insets(8, 0, 0, 0));
                    tabContent.getChildren().addAll(empty, btnRow);
                }
            });
        }).start();
    }

    /** Create a new quiz attached to the selected lesson and open the builder */
    private void createQuizForLesson() {
        if (selectedLesson == null || course == null) return;
        Quiz newQuiz = new Quiz();
        newQuiz.setCourseId(course.getId());
        newQuiz.setLessonId(selectedLesson.getId());
        newQuiz.setTitle(selectedLesson.getTitle() + " Quiz");
        newQuiz.setPassingScore(70);
        newQuiz.setAttemptsAllowed(3);
        newQuiz.setShowAnswersAfterSubmit(true);

        new Thread(() -> {
            String quizId = quizService.createQuiz(newQuiz);
            if (quizId != null) {
                newQuiz.setId(quizId);
                Platform.runLater(() -> {
                    showToast("Quiz created!", ToastNotification.Type.SUCCESS);
                    openQuizBuilder(newQuiz);
                });
            } else {
                Platform.runLater(() -> showToast("Failed to create quiz", ToastNotification.Type.ERROR));
            }
        }).start();
    }

    /** Open the quiz builder as a full isolated page */
    private void openQuizBuilder(Quiz quiz) {
        try {
            videoPlayer.dispose();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/stla/views/instructor/quiz-builder.fxml"));
            javafx.scene.Parent builder = loader.load();
            QuizBuilderController ctrl = loader.getController();
            ctrl.setCourse(course);
            ctrl.loadQuiz(quiz);
            rootPane.getScene().setRoot(builder);
        } catch (Exception e) {
            System.err.println("Error loading quiz builder: " + e.getMessage());
            e.printStackTrace();
            showToast("Error opening quiz builder", ToastNotification.Type.ERROR);
        }
    }

    // ==================== SECTION ACTIONS ====================

    @javafx.fxml.FXML
    private void handleAddSection() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Section");
        dialog.setHeaderText("Enter section title:");
        dialog.showAndWait().ifPresent(title -> {
            if (title.isBlank()) return;
            CourseSection s = new CourseSection();
            s.setCourseId(course.getId());
            s.setTitle(title.trim());
            new Thread(() -> { lessonService.addSection(s); Platform.runLater(this::loadSections); }).start();
        });
    }

    private void editSection(CourseSection section) {
        TextInputDialog dialog = new TextInputDialog(section.getTitle());
        dialog.setTitle("Rename Section");
        dialog.setHeaderText("New title:");
        dialog.showAndWait().ifPresent(title -> {
            if (title.isBlank()) return;
            section.setTitle(title.trim());
            new Thread(() -> { lessonService.updateSection(section); Platform.runLater(this::loadSections); }).start();
        });
    }

    private void deleteSection(CourseSection section) {
        if (ComponentFactory.showConfirmDialog("Delete Section", "Delete '" + section.getTitle() + "' and all its lessons?")) {
            new Thread(() -> { lessonService.deleteSection(section.getId()); Platform.runLater(() -> { selectedLesson = null; loadSections(); }); }).start();
        }
    }

    // ==================== LESSON ACTIONS ====================

    private void addLesson(CourseSection section) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Lesson");
        dialog.setHeaderText("Enter lesson title:");
        dialog.showAndWait().ifPresent(title -> {
            if (title.isBlank()) return;
            CourseLesson l = new CourseLesson();
            l.setCourseId(course.getId());
            l.setSectionId(section.getId());
            l.setTitle(title.trim());
            new Thread(() -> { lessonService.addLesson(l); Platform.runLater(this::loadSections); }).start();
        });
    }

    private void editLesson(CourseLesson lesson) {
        TextInputDialog dialog = new TextInputDialog(lesson.getTitle());
        dialog.setTitle("Edit Lesson");
        dialog.setHeaderText("Rename lesson:");
        dialog.showAndWait().ifPresent(title -> {
            if (title.isBlank()) return;
            lesson.setTitle(title.trim());
            new Thread(() -> { lessonService.updateLesson(lesson); Platform.runLater(this::loadSections); }).start();
        });
    }

    private void deleteLesson(CourseLesson lesson) {
        if (ComponentFactory.showConfirmDialog("Delete Lesson", "Delete '" + lesson.getTitle() + "'?")) {
            new Thread(() -> {
                lessonService.deleteLesson(lesson.getId());
                Platform.runLater(() -> {
                    if (selectedLesson != null && selectedLesson.getId().equals(lesson.getId())) selectedLesson = null;
                    loadSections();
                });
            }).start();
        }
    }

    private void duplicateLesson(CourseLesson lesson) {
        new Thread(() -> {
            lessonService.duplicateLesson(lesson);
            Platform.runLater(this::loadSections);
        }).start();
        showToast("Lesson duplicated", ToastNotification.Type.SUCCESS);
    }

    private void moveLesson(CourseLesson lesson, CourseSection section, int direction) {
        if (section.getLessons() == null) return;
        List<CourseLesson> lessons = new ArrayList<>(section.getLessons());
        int idx = -1;
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(lesson.getId())) { idx = i; break; }
        }
        if (idx < 0) return;
        int newIdx = idx + direction;
        if (newIdx < 0 || newIdx >= lessons.size()) return;
        Collections.swap(lessons, idx, newIdx);
        new Thread(() -> { lessonService.reorderLessons(lessons); Platform.runLater(this::loadSections); }).start();
    }

    // ==================== PLAYER ACTIONS ====================

    @javafx.fxml.FXML
    private void handleEditLesson() {
        if (selectedLesson != null) editLesson(selectedLesson);
    }

    @javafx.fxml.FXML
    private void handleReplaceVideo() {
        if (selectedLesson == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Video");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov", "*.mkv", "*.webm"));
        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file == null) return;
        uploadVideo(file);
    }

    @javafx.fxml.FXML
    private void handleDeleteLesson() {
        if (selectedLesson != null) deleteLesson(selectedLesson);
    }

    @javafx.fxml.FXML
    private void handleSaveChanges() {
        if (selectedLesson == null) return;
        new Thread(() -> {
            lessonService.updateLesson(selectedLesson);
            Platform.runLater(() -> {
                showToast("Changes saved", ToastNotification.Type.SUCCESS);
                loadSections();
            });
        }).start();
    }

    @javafx.fxml.FXML
    private void handleBack() {
        videoPlayer.dispose();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/stla/views/instructor/instructor-dashboard.fxml"));
            javafx.scene.Parent dashboard = loader.load();
            rootPane.getScene().setRoot(dashboard);
        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
        }
    }

    // ==================== UPLOAD ====================

    private void uploadVideo(File file) {
        // Show progress dialog
        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Uploading Video");
        progress.setHeaderText("Uploading: " + file.getName());
        progress.setContentText("Please wait...");
        ProgressBar pb = new ProgressBar(-1);
        pb.setPrefWidth(300);
        progress.getDialogPane().setContent(pb);
        progress.show();

        new Thread(() -> {
            String url = lessonService.uploadVideo(file, course.getId(), getSectionId(), selectedLesson.getId());
            if (url != null) {
                selectedLesson.setVideoUrl(url);
                lessonService.updateLesson(selectedLesson);
            }
            Platform.runLater(() -> {
                progress.close();
                if (url != null) {
                    showToast("Video uploaded successfully!", ToastNotification.Type.SUCCESS);
                    loadVideo(selectedLesson);
                    loadSections();
                } else {
                    showToast("Video upload failed", ToastNotification.Type.ERROR);
                }
            });
        }).start();
    }

    private void uploadResource(File file) {
        String title = file.getName();
        new Thread(() -> {
            String id = resourceService.uploadAndSaveResource(file, selectedLesson.getId(), title);
            Platform.runLater(() -> {
                if (id != null) {
                    showToast("Resource uploaded!", ToastNotification.Type.SUCCESS);
                    buildResourcesTab();
                } else {
                    showToast("Resource upload failed", ToastNotification.Type.ERROR);
                }
            });
        }).start();
    }

    // ==================== HELPERS ====================

    private String getSectionId() {
        return selectedLesson != null && selectedLesson.getSectionId() != null ? selectedLesson.getSectionId() : "default";
    }

    private String formatDuration(int totalSeconds) {
        int h = totalSeconds / 3600, m = (totalSeconds % 3600) / 60;
        if (h > 0) return h + "h " + m + "m";
        return m + " min";
    }

    private void showToast(String msg, ToastNotification.Type type) {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            ToastNotification.show(stage, msg, type);
        } catch (Exception ignored) {}
    }
}
