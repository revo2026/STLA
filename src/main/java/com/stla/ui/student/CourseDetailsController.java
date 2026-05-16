package com.stla.ui.student;

import com.stla.core.navigation.StudentNavigationContext;
import com.stla.core.session.SessionManager;
import com.stla.domain.models.Course;
import com.stla.domain.models.CourseDetailsView;
import com.stla.domain.models.CourseLesson;
import com.stla.domain.models.CourseReview;
import com.stla.domain.models.StudentQuizSummary;
import com.stla.services.CourseDetailsService;
import com.stla.services.QuizService;
import com.stla.ui.components.AnimationUtils;
import com.stla.ui.components.CourseThumbnailLoader;
import com.stla.ui.components.StudentQuizUiHelper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.math.BigDecimal;
import java.util.function.Consumer;

public class CourseDetailsController {

    @FXML private VBox rootBox;
    @FXML private Label courseTitleLabel;
    @FXML private Label instructorLabel;
    @FXML private Label ratingLabel;
    @FXML private Label studentsLabel;
    @FXML private Label durationLabel;
    @FXML private Label sectionsLabel;
    @FXML private Label lessonsLabel;
    @FXML private Label priceLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextArea descriptionArea;
    @FXML private VBox quizzesBox;
    @FXML private VBox previewLessonsBox;
    @FXML private VBox reviewsBox;
    @FXML private VBox includesBox;
    @FXML private Button enrollButton;
    @FXML private Label certificateBadge;
    @FXML private StackPane heroStack;
    @FXML private StackPane heroThumbnailPane;
    @FXML private VBox heroThumbPlaceholder;
    @FXML private ImageView heroThumbnailView;

    private final CourseDetailsService detailsService = new CourseDetailsService();
    private final QuizService quizService = new QuizService();
    private Consumer<String> onEnrollNow;
    private Consumer<String> onContinueLearning;
    private Consumer<String> onStartQuiz;
    private String courseId;
    private boolean enrolled;

    public void setCourseId(String courseId) {
        this.courseId = courseId;
        StudentNavigationContext.setSelectedCourseId(courseId);
        if (rootBox != null && courseTitleLabel.getText().equals("Course Title")) {
            loadDetails();
        }
    }
    public void setOnEnrollNow(Consumer<String> handler) { this.onEnrollNow = handler; }
    public void setOnContinueLearning(Consumer<String> handler) { this.onContinueLearning = handler; }
    public void setOnStartQuiz(Consumer<String> handler) { this.onStartQuiz = handler; }

    @FXML
    public void initialize() {
        if (heroThumbnailView != null && heroThumbnailPane != null) {
            heroThumbnailView.fitWidthProperty().bind(heroThumbnailPane.widthProperty());
            heroThumbnailView.fitHeightProperty().bind(heroThumbnailPane.heightProperty());
        }
        if (courseId == null) courseId = StudentNavigationContext.getSelectedCourseId();
        if (courseId == null) return;
        loadDetails();
    }

    private void loadDetails() {
        String studentId = SessionManager.getInstance().getCurrentStudent() != null
                ? SessionManager.getInstance().getCurrentStudent().getId() : null;

        Task<CourseDetailsView> task = new Task<>() {
            @Override protected CourseDetailsView call() {
                return detailsService.getCourseDetails(courseId, studentId).orElse(null);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (task.getValue() != null) populate(task.getValue());
        }));
        new Thread(task).start();
    }

    private void populate(CourseDetailsView view) {
        Course c = view.getCourse();
        courseTitleLabel.setText(c.getTitle());
        subtitleLabel.setText(c.getSubtitle() != null ? c.getSubtitle() : "");
        instructorLabel.setText("👨‍🏫 " + (view.getInstructorName() != null ? view.getInstructorName() : "Instructor"));
        ratingLabel.setText("⭐ " + (c.getRatingAvg() != null ? c.getRatingAvg() : "0.0") + " (" + c.getRatingCount() + " reviews)");
        studentsLabel.setText("👥 " + c.getEnrollmentCount() + " students");
        durationLabel.setText("⏱ " + view.getTotalDurationMinutes() + " min");
        sectionsLabel.setText("📂 " + view.getSectionsCount() + " sections");
        lessonsLabel.setText("🎬 " + view.getLessonsCount() + " lessons");
        BigDecimal price = view.getEffectivePrice();
        priceLabel.setText(price.compareTo(BigDecimal.ZERO) == 0 ? "Free" : "$" + price);
        descriptionArea.setText(c.getDescription() != null ? c.getDescription() : "");

        certificateBadge.setVisible(c.isHasCertificate());
        certificateBadge.setManaged(c.isHasCertificate());

        CourseThumbnailLoader.loadWithPlaceholder(c.getThumbnailUrl(), heroThumbnailView, heroThumbPlaceholder);

        enrolled = view.isEnrolled();
        buildIncludes(c);
        buildQuizzes(courseId, studentIdFromView(view));
        buildPreviewLessons(view.getPreviewLessons());
        buildReviews(view.getReviews());

        if (view.isEnrolled()) {
            enrollButton.setText("Continue Learning →");
            enrollButton.getStyleClass().removeAll("btn-accent");
            if (!enrollButton.getStyleClass().contains("btn-primary")) {
                enrollButton.getStyleClass().add("btn-primary");
            }
        } else {
            enrollButton.setText("Enroll Now — " + priceLabel.getText());
        }
        AnimationUtils.fadeIn(rootBox, 350);
    }

    private String studentIdFromView(CourseDetailsView view) {
        return SessionManager.getInstance().getCurrentStudent() != null
                ? SessionManager.getInstance().getCurrentStudent().getId() : null;
    }

    private void buildQuizzes(String courseId, String studentId) {
        quizzesBox.getChildren().clear();
        new Thread(() -> {
            var summaries = quizService.getCourseQuizzes(courseId, studentId);
            Platform.runLater(() -> {
                if (summaries.isEmpty()) {
                    quizzesBox.getChildren().add(new Label("No quizzes for this course yet") {{
                        getStyleClass().add("text-secondary");
                    }});
                    return;
                }
                for (StudentQuizSummary s : summaries) {
                    quizzesBox.getChildren().add(StudentQuizUiHelper.buildQuizCard(s, quizId -> {
                        if (onStartQuiz != null) onStartQuiz.accept(quizId);
                    }, studentId));
                }
            });
        }).start();
    }

    private void buildIncludes(Course c) {
        includesBox.getChildren().clear();
        addInclude("🎬", "On-demand video lessons");
        if (c.isHasResources()) addInclude("📁", "Downloadable resources");
        if (c.isHasQuiz()) addInclude("🎯", "Quizzes & assessments");
        if (c.isHasCertificate()) addInclude("🏆", "Certificate of completion");
        addInclude("♾", "Lifetime access");
    }

    private void addInclude(String icon, String text) {
        HBox row = new HBox(10, new Label(icon), new Label(text));
        row.setAlignment(Pos.CENTER_LEFT);
        includesBox.getChildren().add(row);
    }

    private void buildPreviewLessons(java.util.List<CourseLesson> lessons) {
        previewLessonsBox.getChildren().clear();
        if (lessons == null || lessons.isEmpty()) {
            previewLessonsBox.getChildren().add(new Label("No preview lessons available") {{
                getStyleClass().add("text-secondary");
            }});
            return;
        }
        for (CourseLesson l : lessons) {
            HBox row = new HBox(12);
            row.getStyleClass().add("card");
            row.setPadding(new Insets(12));
            row.setAlignment(Pos.CENTER_LEFT);
            Label play = new Label("▶");
            play.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 18px;");
            VBox info = new VBox(2, new Label(l.getTitle()) {{ setStyle("-fx-font-weight: bold;"); }},
                    new Label("Preview • " + formatDuration(l.getDurationSeconds())) {{ getStyleClass().add("text-secondary"); }});
            HBox.setHgrow(info, Priority.ALWAYS);
            Label badge = new Label("FREE PREVIEW");
            badge.getStyleClass().addAll("badge", "badge-student");
            row.getChildren().addAll(play, info, badge);
            previewLessonsBox.getChildren().add(row);
        }
    }

    private void buildReviews(java.util.List<CourseReview> reviews) {
        reviewsBox.getChildren().clear();
        if (reviews == null || reviews.isEmpty()) {
            reviewsBox.getChildren().add(new Label("No reviews yet") {{ getStyleClass().add("text-secondary"); }});
            return;
        }
        for (CourseReview r : reviews) {
            VBox card = new VBox(6);
            card.getStyleClass().add("card");
            card.setPadding(new Insets(14));
            card.getChildren().addAll(
                    new HBox(8, new Label("⭐ " + r.getRating()), new Label(r.getStudentName()) {{ getStyleClass().add("text-secondary"); }}),
                    new Label(r.getComment() != null ? r.getComment() : "") {{ setWrapText(true); }}
            );
            reviewsBox.getChildren().add(card);
        }
    }

    private String formatDuration(int seconds) {
        int m = Math.max(1, seconds / 60);
        return m + " min";
    }

    @FXML
    private void handleEnroll() {
        if (courseId == null) return;
        StudentNavigationContext.setSelectedCourseId(courseId);
        if (enrollButton.getText().startsWith("Continue") && onContinueLearning != null) {
            onContinueLearning.accept(courseId);
        } else if (onEnrollNow != null) {
            onEnrollNow.accept(courseId);
        } else {
            StudentNavigationContext.goToCheckout(courseId);
        }
    }
}
