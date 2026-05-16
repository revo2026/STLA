package com.stla.ui.instructor;

import com.stla.domain.models.Course;
import com.stla.domain.models.CourseLesson;
import com.stla.domain.models.CourseSection;
import com.stla.services.LessonService;
import com.stla.services.SupabaseStorageAdapter;
import com.stla.ui.components.AnimationUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Course content management: Sections, Lessons, Videos, Resources.
 * Loaded after a course is approved.
 */
public class CourseSectionsController {

    @javafx.fxml.FXML private Label courseTitle, courseStatus;
    @javafx.fxml.FXML private VBox sectionsContainer;
    @javafx.fxml.FXML private Button btnAddSection, btnAddQuiz;

    private final LessonService lessonService = new LessonService();
    private final SupabaseStorageAdapter storageAdapter = new SupabaseStorageAdapter();
    private Course course;

    public void setCourse(Course course) {
        this.course = course;
        courseTitle.setText("📚 " + course.getTitle());
        courseStatus.setText("Status: " + course.getStatus().getValue().toUpperCase());
        loadSections();
    }

    @javafx.fxml.FXML
    public void initialize() {}

    private void loadSections() {
        if (course == null) return;
        Task<List<CourseSection>> task = new Task<>() {
            @Override protected List<CourseSection> call() {
                return lessonService.getSections(course.getId());
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> buildSectionsUI(getValue()));
            }
        };
        new Thread(task).start();
    }

    private void buildSectionsUI(List<CourseSection> sections) {
        sectionsContainer.getChildren().clear();
        if (sections == null || sections.isEmpty()) {
            sectionsContainer.getChildren().add(
                new Label("No sections yet. Click '➕ Add Section' to start building your course content.")
            );
            return;
        }

        for (int i = 0; i < sections.size(); i++) {
            CourseSection section = sections.get(i);
            VBox sectionCard = buildSectionCard(section, i + 1);
            sectionsContainer.getChildren().add(sectionCard);
        }
        AnimationUtils.staggerFadeIn(sectionsContainer, 80);
    }

    private VBox buildSectionCard(CourseSection section, int index) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-background-radius: 12;");

        // Section header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("📂 Section " + index + ": " + section.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✏");
        editBtn.getStyleClass().addAll("btn-outline", "btn-sm");
        editBtn.setOnAction(e -> editSection(section));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().addAll("btn-outline", "btn-sm");
        deleteBtn.setStyle("-fx-text-fill: #EF4444;");
        deleteBtn.setOnAction(e -> deleteSection(section));

        Button addLessonBtn = new Button("➕ Lesson");
        addLessonBtn.getStyleClass().addAll("btn-primary", "btn-sm");
        addLessonBtn.setOnAction(e -> addLesson(section));

        header.getChildren().addAll(title, spacer, addLessonBtn, editBtn, deleteBtn);
        card.getChildren().add(header);

        // Lessons list
        if (section.getLessons() != null && !section.getLessons().isEmpty()) {
            for (CourseLesson lesson : section.getLessons()) {
                card.getChildren().add(buildLessonRow(lesson));
            }
        } else {
            Label empty = new Label("   No lessons in this section yet.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
            card.getChildren().add(empty);
        }

        return card;
    }

    private HBox buildLessonRow(CourseLesson lesson) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 24));
        row.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8;");

        Label icon = new Label(lesson.getVideoUrl() != null && !lesson.getVideoUrl().isBlank() ? "🎬" : "📄");
        Label title = new Label(lesson.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label duration = new Label(lesson.getDurationFormatted());
        duration.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

        Label preview = new Label(lesson.isPreview() ? "👁 Preview" : "");
        preview.setStyle("-fx-text-fill: #7C3AED; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button uploadVideoBtn = new Button("🎬");
        uploadVideoBtn.getStyleClass().addAll("btn-outline", "btn-sm");
        uploadVideoBtn.setTooltip(new Tooltip("Upload Video"));
        uploadVideoBtn.setOnAction(e -> uploadVideo(lesson));

        Button editBtn = new Button("✏");
        editBtn.getStyleClass().addAll("btn-outline", "btn-sm");
        editBtn.setOnAction(e -> editLesson(lesson));

        Button delBtn = new Button("🗑");
        delBtn.getStyleClass().addAll("btn-outline", "btn-sm");
        delBtn.setOnAction(e -> deleteLesson(lesson));

        row.getChildren().addAll(icon, title, duration, preview, spacer, uploadVideoBtn, editBtn, delBtn);
        return row;
    }

    // ==================== SECTION ACTIONS ====================

    @javafx.fxml.FXML
    private void handleAddSection() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Section");
        dialog.setHeaderText("Enter section title:");
        dialog.setContentText("Title:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            if (title.isBlank()) return;
            CourseSection section = new CourseSection();
            section.setCourseId(course.getId());
            section.setTitle(title.trim());
            new Thread(() -> {
                lessonService.addSection(section);
                Platform.runLater(this::loadSections);
            }).start();
        });
    }

    private void editSection(CourseSection section) {
        TextInputDialog dialog = new TextInputDialog(section.getTitle());
        dialog.setTitle("Edit Section");
        dialog.setHeaderText("Rename section:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            if (title.isBlank()) return;
            section.setTitle(title.trim());
            new Thread(() -> {
                lessonService.updateSection(section);
                Platform.runLater(this::loadSections);
            }).start();
        });
    }

    private void deleteSection(CourseSection section) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete section '" + section.getTitle() + "' and all its lessons?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    lessonService.deleteSection(section.getId());
                    Platform.runLater(this::loadSections);
                }).start();
            }
        });
    }

    // ==================== LESSON ACTIONS ====================

    private void addLesson(CourseSection section) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Lesson");
        dialog.setHeaderText("Enter lesson title:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            if (title.isBlank()) return;
            CourseLesson lesson = new CourseLesson();
            lesson.setCourseId(course.getId());
            lesson.setSectionId(section.getId());
            lesson.setTitle(title.trim());
            new Thread(() -> {
                lessonService.addLesson(lesson);
                Platform.runLater(this::loadSections);
            }).start();
        });
    }

    private void editLesson(CourseLesson lesson) {
        TextInputDialog dialog = new TextInputDialog(lesson.getTitle());
        dialog.setTitle("Edit Lesson");
        dialog.setHeaderText("Rename lesson:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            if (title.isBlank()) return;
            lesson.setTitle(title.trim());
            new Thread(() -> {
                lessonService.updateLesson(lesson);
                Platform.runLater(this::loadSections);
            }).start();
        });
    }

    private void deleteLesson(CourseLesson lesson) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete lesson '" + lesson.getTitle() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    lessonService.deleteLesson(lesson.getId());
                    Platform.runLater(this::loadSections);
                }).start();
            }
        });
    }

    private void uploadVideo(CourseLesson lesson) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Video");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov", "*.mkv", "*.webm"));
        File file = fc.showOpenDialog(sectionsContainer.getScene().getWindow());
        if (file == null) return;

        Alert progress = new Alert(Alert.AlertType.INFORMATION, "Uploading video...");
        progress.setHeaderText(null);
        progress.show();

        new Thread(() -> {
            String url = lessonService.uploadVideo(file, lesson.getId());
            if (url != null) {
                lesson.setVideoUrl(url);
                lessonService.updateLesson(lesson);
            }
            Platform.runLater(() -> {
                progress.close();
                loadSections();
                if (url != null) {
                    new Alert(Alert.AlertType.INFORMATION, "✅ Video uploaded successfully!").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "❌ Video upload failed.").showAndWait();
                }
            });
        }).start();
    }

    // ==================== QUIZ ====================

    @javafx.fxml.FXML
    private void handleAddQuiz() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/stla/views/instructor/quiz-builder.fxml"));
            javafx.scene.Parent content = loader.load();
            QuizBuilderController ctrl = loader.getController();
            ctrl.setCourseId(course.getId());
            // Navigate up to find the ScrollPane content area and replace
            javafx.scene.Node current = sectionsContainer;
            while (current != null) {
                if (current.getParent() instanceof javafx.scene.control.ScrollPane sp) {
                    sp.setContent(content);
                    return;
                }
                current = current.getParent();
            }
            System.err.println("Could not find ScrollPane parent to load quiz builder.");
        } catch (Exception e) {
            System.err.println("Error loading quiz builder: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
