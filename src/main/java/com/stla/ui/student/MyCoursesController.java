package com.stla.ui.student;

import com.stla.core.session.SessionManager;
import com.stla.domain.enums.EnrollmentStatus;
import com.stla.domain.models.Enrollment;
import com.stla.services.CourseService;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.CourseThumbnailLoader;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyCoursesController {

    @javafx.fxml.FXML private VBox coursesContainer;

    private final CourseService courseService = new CourseService();
    private java.util.function.Consumer<String> onContinue;
    private java.util.function.Consumer<Enrollment> onRateInstructor;

    public void setOnContinue(java.util.function.Consumer<String> onContinue) {
        this.onContinue = onContinue;
    }

    public void setOnRateInstructor(java.util.function.Consumer<Enrollment> onRateInstructor) {
        this.onRateInstructor = onRateInstructor;
    }

    @javafx.fxml.FXML
    public void initialize() {
        loadEnrollments();
    }

    private void loadEnrollments() {
        coursesContainer.getChildren().clear();
        coursesContainer.getChildren().add(ComponentFactory.createLoadingState());

        Task<List<Enrollment>> task = new Task<>() {
            @Override protected List<Enrollment> call() {
                return courseService.getStudentEnrollments(SessionManager.getInstance().getCurrentStudent().getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> buildList(task.getValue())));
        new Thread(task).start();
    }

    private void buildList(List<Enrollment> enrollments) {
        coursesContainer.getChildren().clear();
        if (enrollments == null || enrollments.isEmpty()) {
            coursesContainer.getChildren().add(ComponentFactory.createEmptyState("📚", "You haven't enrolled in any courses yet"));
            return;
        }
        for (Enrollment en : enrollments) {
            coursesContainer.getChildren().add(buildCourseCard(en));
        }
    }

    private HBox buildCourseCard(Enrollment en) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane thumbPane = buildThumbnail(en.getCourseThumbnailUrl());
        thumbPane.setPrefSize(100, 72);
        thumbPane.setMinSize(100, 72);
        thumbPane.setMaxSize(100, 72);

        VBox info = new VBox(8);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(en.getCourseTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1F2937;");
        title.setWrapText(true);
        title.setMaxWidth(420);
        titleRow.getChildren().add(title);
        if (en.getProgressPercent() >= 100 || en.getStatus() == EnrollmentStatus.COMPLETED) {
            Label badge = new Label("Completed");
            badge.getStyleClass().add("course-completed-badge");
            titleRow.getChildren().add(badge);
        }
        info.getChildren().add(titleRow);

        if (en.getInstructorName() != null && !en.getInstructorName().isBlank()) {
            Label instructor = new Label("👨‍🏫 " + en.getInstructorName());
            instructor.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            info.getChildren().add(instructor);
        }

        ProgressBar bar = new ProgressBar(en.getProgressPercent() / 100.0);
        bar.getStyleClass().add("course-progress-bar");
        bar.setPrefWidth(280);
        bar.setMaxWidth(400);

        String lessonsText = en.getTotalLessons() > 0
                ? en.getLessonsCompleted() + " / " + en.getTotalLessons() + " lessons completed"
                : "Progress: " + (int) en.getProgressPercent() + "%";
        Label progressMeta = new Label(lessonsText);
        progressMeta.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
        info.getChildren().addAll(bar, progressMeta);

        if (en.getLastLessonTitle() != null && !en.getLastLessonTitle().isBlank()) {
            Label last = new Label("Last: " + en.getLastLessonTitle());
            last.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            info.getChildren().add(last);
        } else if (en.getLastAccessedAt() != null) {
            Label last = new Label("Last accessed: " + en.getLastAccessedAt()
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
            last.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            info.getChildren().add(last);
        }

        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMinWidth(160);

        boolean completed = en.getProgressPercent() >= 100 || en.getStatus() == EnrollmentStatus.COMPLETED;
        Button cont = new Button(completed ? "Revisit Course →" : "Continue Learning →");
        cont.getStyleClass().addAll("btn-primary", "btn-sm");
        cont.setMaxWidth(Double.MAX_VALUE);
        String courseId = en.getCourseId();
        cont.setOnAction(e -> { if (onContinue != null) onContinue.accept(courseId); });

        if (en.isHasReview()) {
            Label reviewed = new Label("✓ You reviewed this course");
            reviewed.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-alignment: center-right;");
            Button editReviewBtn = new Button("Edit review");
            editReviewBtn.getStyleClass().addAll("btn-outline", "btn-sm");
            editReviewBtn.setMaxWidth(Double.MAX_VALUE);
            editReviewBtn.setOnAction(e -> openRatePage(en));
            actions.getChildren().addAll(cont, reviewed, editReviewBtn);
        } else {
            Button rateBtn = new Button("⭐ Rate Instructor");
            rateBtn.getStyleClass().addAll("btn-outline", "btn-sm");
            rateBtn.setMaxWidth(Double.MAX_VALUE);
            rateBtn.setOnAction(e -> openRatePage(en));
            actions.getChildren().addAll(cont, rateBtn);
        }

        card.getChildren().addAll(thumbPane, info, actions);
        return card;
    }

    private StackPane buildThumbnail(String thumbnailUrl) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("course-card-thumb");
        pane.setStyle("-fx-background-radius: 10;");

        Label fallback = new Label("📘");
        fallback.setStyle("-fx-font-size: 28px;");
        VBox placeholderBox = new VBox(fallback);
        placeholderBox.setAlignment(Pos.CENTER);
        placeholderBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        placeholderBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #0E4A54, #0F3D45); -fx-background-radius: 10;");

        ImageView imageView = new ImageView();
        imageView.fitWidthProperty().bind(pane.widthProperty());
        imageView.fitHeightProperty().bind(pane.heightProperty());
        imageView.setPreserveRatio(true);
        imageView.setVisible(false);

        pane.getChildren().addAll(placeholderBox, imageView);
        CourseThumbnailLoader.loadWithPlaceholder(thumbnailUrl, imageView, placeholderBox);
        return pane;
    }

    private void openRatePage(Enrollment en) {
        if (onRateInstructor != null) {
            onRateInstructor.accept(en);
        }
    }
}
