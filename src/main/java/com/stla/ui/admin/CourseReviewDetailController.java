package com.stla.ui.admin;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.Course;
import com.stla.domain.models.CourseLesson;
import com.stla.domain.models.CourseSection;
import com.stla.patterns.facade.CoursePublishFacade;
import com.stla.services.LessonService;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.CourseThumbnailLoader;
import com.stla.ui.components.IntroVideoPane;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Isolated admin page for reviewing one pending course (details, intro video, curriculum).
 */
public class CourseReviewDetailController {

    @FXML private Label pageTitleLabel;
    @FXML private VBox detailContainer;

    private final LessonService lessonService = new LessonService();
    private final CoursePublishFacade facade = new CoursePublishFacade();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private Course course;
    private IntroVideoPane introVideoPane;
    private Runnable onBack;
    private Runnable onReviewComplete;

    public void init(Course course, Runnable onBack, Runnable onReviewComplete) {
        this.course = course;
        this.onBack = onBack;
        this.onReviewComplete = onReviewComplete;
        pageTitleLabel.setText(course.getTitle());
        loadContent();
    }

    @FXML
    private void handleBack() {
        if (introVideoPane != null) {
            introVideoPane.dispose();
        }
        if (onBack != null) onBack.run();
    }

    private void loadContent() {
        detailContainer.getChildren().clear();
        detailContainer.getChildren().add(ComponentFactory.createLoadingState());

        Task<List<CourseSection>> task = new Task<>() {
            @Override protected List<CourseSection> call() {
                return lessonService.getSections(course.getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> buildDetail(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            detailContainer.getChildren().clear();
            detailContainer.getChildren().add(ComponentFactory.createEmptyState("⚠️", "Failed to load course"));
        }));
        new Thread(task).start();
    }

    private void buildDetail(List<CourseSection> sections) {
        detailContainer.getChildren().clear();
        detailContainer.getChildren().addAll(
            buildHeader(sections),
            new Separator(),
            buildIntroSection(),
            new Separator(),
            buildDetailsGrid(),
            buildCurriculum(sections),
            buildAddons(),
            new Separator(),
            buildActions()
        );
    }

    private VBox buildHeader(List<CourseSection> sections) {
        HBox header = new HBox(16);
        header.setAlignment(Pos.TOP_LEFT);

        StackPane thumbBox = new StackPane();
        thumbBox.setPrefSize(220, 130);
        thumbBox.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 12;");
        Label ph = new Label("📚");
        ph.setStyle("-fx-font-size: 36px;");
        ImageView iv = new ImageView();
        iv.setFitWidth(220);
        iv.setFitHeight(130);
        iv.setPreserveRatio(true);
        thumbBox.getChildren().addAll(ph, iv);
        CourseThumbnailLoader.loadWithPlaceholder(course.getThumbnailUrl(), iv, ph);

        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(course.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label subtitle = new Label(nullToEmpty(course.getSubtitle()));
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #6B7280;");
        subtitle.setVisible(!subtitle.getText().isBlank());
        subtitle.setManaged(!subtitle.getText().isBlank());

        HBox badges = new HBox(8);
        badges.getChildren().addAll(
            ComponentFactory.createStatusBadge("PENDING", "pending"),
            ComponentFactory.createStatusBadge(
                course.getLevel() != null ? course.getLevel().getValue() : "—", "info")
        );

        int lessons = countLessons(sections);
        GridPane meta = new GridPane();
        meta.setHgap(20);
        meta.setVgap(6);
        int row = 0;
        addMeta(meta, row++, "Instructor", dash(course.getInstructorName()));
        addMeta(meta, row++, "Category", dash(course.getCategoryName()));
        addMeta(meta, row++, "Price", "$" + (course.getPrice() != null ? course.getPrice() : "0"));
        addMeta(meta, row++, "Duration", course.getEstimatedHours() != null ? course.getEstimatedHours() + " hours" : "—");
        addMeta(meta, row++, "Language", dash(course.getLanguage()));
        addMeta(meta, row++, "Curriculum", sections.size() + " sections · " + lessons + " lessons");
        addMeta(meta, row++, "Submitted", course.getCreatedAt() != null ? course.getCreatedAt().format(dateFmt) : "—");

        info.getChildren().addAll(title, subtitle, badges, meta);
        header.getChildren().addAll(thumbBox, info);

        VBox wrap = new VBox(header);
        wrap.getStyleClass().add("card");
        wrap.setPadding(new Insets(20));
        return wrap;
    }

    private VBox buildIntroSection() {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));

        Label heading = new Label("🎬 Intro Video");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        introVideoPane = new IntroVideoPane();
        VBox.setVgrow(introVideoPane, Priority.ALWAYS);
        introVideoPane.load(course.getIntroVideoUrl());

        box.getChildren().addAll(heading, introVideoPane);
        return box;
    }

    private VBox buildDetailsGrid() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));

        Label heading = new Label("📋 Course Details");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setVgap(14);
        int row = 0;
        row = addBlock(grid, row, "Description", course.getDescription());
        row = addBlock(grid, row, "What Students Will Learn", course.getWhatYouWillLearn());
        row = addBlock(grid, row, "Requirements", course.getRequirements());
        addBlock(grid, row, "Target Audience", course.getTargetAudience());

        box.getChildren().addAll(heading, grid);
        return box;
    }

    private VBox buildCurriculum(List<CourseSection> sections) {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));

        Label heading = new Label("📖 Curriculum");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        box.getChildren().add(heading);

        if (sections == null || sections.isEmpty()) {
            box.getChildren().add(new Label("No sections added yet.") {{
                setStyle("-fx-text-fill: #9CA3AF;");
            }});
            return box;
        }

        VBox list = new VBox(8);
        for (int i = 0; i < sections.size(); i++) {
            CourseSection sec = sections.get(i);
            VBox secBox = new VBox(6);
            secBox.setPadding(new Insets(10));
            secBox.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8;");
            secBox.getChildren().add(new Label("Section " + (i + 1) + ": " + sec.getTitle()) {{
                setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            }});
            if (sec.getLessons() == null || sec.getLessons().isEmpty()) {
                secBox.getChildren().add(new Label("No lessons") {{ setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;"); }});
            } else {
                for (CourseLesson lesson : sec.getLessons()) {
                    String icon = lesson.getVideoUrl() != null && !lesson.getVideoUrl().isBlank() ? "🎬" : "📄";
                    secBox.getChildren().add(new Label(icon + " " + lesson.getTitle() + " · " + lesson.getDurationFormatted()) {{
                        setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
                    }});
                }
            }
            list.getChildren().add(secBox);
        }
        box.getChildren().add(list);
        return box;
    }

    private HBox buildAddons() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(new Label("Includes:") {{ setStyle("-fx-font-weight: bold;"); }});
        if (course.isHasCertificate()) row.getChildren().add(badge("🏆 Certificate"));
        if (course.isHasQuiz()) row.getChildren().add(badge("📝 Quiz"));
        if (course.isHasMentorSupport()) row.getChildren().add(badge("👨‍🏫 Mentor"));
        if (course.isHasResources()) row.getChildren().add(badge("📦 Resources"));
        if (row.getChildren().size() == 1) {
            row.getChildren().add(new Label("None") {{ setStyle("-fx-text-fill: #9CA3AF;"); }});
        }
        return row;
    }

    private HBox buildActions() {
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        Button approve = new Button("✅ Approve Course");
        approve.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand;");
        approve.setOnAction(e -> approve());

        Button reject = new Button("❌ Reject Course");
        reject.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand;");
        reject.setOnAction(e -> reject());

        actions.getChildren().addAll(approve, reject);
        return actions;
    }

    private void approve() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Approve \"" + course.getTitle() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Approve Course");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                String adminId = SessionManager.getInstance().getCurrentAdmin() != null
                    ? SessionManager.getInstance().getCurrentAdmin().getId() : null;
                new Thread(() -> {
                    facade.approveCourse(course.getId(), adminId, "Approved by admin");
                    Platform.runLater(() -> {
                        if (onReviewComplete != null) onReviewComplete.run();
                        handleBack();
                    });
                }).start();
            }
        });
    }

    private void reject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reject Course");
        dialog.setHeaderText("Reason for rejecting \"" + course.getTitle() + "\":");
        dialog.setContentText("Reason:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            if (reason.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Rejection reason is required.").showAndWait();
                return;
            }
            String adminId = SessionManager.getInstance().getCurrentAdmin() != null
                ? SessionManager.getInstance().getCurrentAdmin().getId() : null;
            new Thread(() -> {
                facade.rejectCourse(course.getId(), adminId, reason.trim());
                Platform.runLater(() -> {
                    if (onReviewComplete != null) onReviewComplete.run();
                    handleBack();
                });
            }).start();
        });
    }

    private void addMeta(GridPane grid, int row, String key, String val) {
        grid.add(new Label(key) {{ setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7280; -fx-font-size: 12px;"); }}, 0, row);
        Label v = new Label(val);
        v.setWrapText(true);
        grid.add(v, 1, row);
    }

    private int addBlock(GridPane grid, int row, String title, String text) {
        if (text == null || text.isBlank()) return row;
        VBox block = new VBox(4);
        block.getChildren().add(new Label(title) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 13px;"); }});
        Label body = new Label(text.trim());
        body.setWrapText(true);
        body.setTextAlignment(TextAlignment.LEFT);
        body.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
        block.getChildren().add(body);
        GridPane.setHgrow(block, Priority.ALWAYS);
        grid.add(block, 0, row, 2, 1);
        return row + 1;
    }

    private int countLessons(List<CourseSection> sections) {
        if (sections == null) return 0;
        return sections.stream().mapToInt(s -> s.getLessons() != null ? s.getLessons().size() : 0).sum();
    }

    private Label badge(String t) {
        return new Label(t) {{
            setStyle("-fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        }};
    }

    private static String nullToEmpty(String s) { return s != null ? s : ""; }
    private static String dash(String s) { return s != null && !s.isBlank() ? s : "—"; }
}
