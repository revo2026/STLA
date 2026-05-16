package com.stla.ui.student;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.Enrollment;
import com.stla.domain.models.Review;
import com.stla.services.CourseService;
import com.stla.services.ReviewService;
import com.stla.ui.components.AnimationUtils;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentReviewsController {

    @FXML private StackPane reviewsRoot;
    @FXML private VBox listView;
    @FXML private VBox writeView;
    @FXML private Button refreshBtn;
    @FXML private Button writeReviewBtn;
    @FXML private VBox reviewsList;

    @FXML private Button backBtn;
    @FXML private ComboBox<CourseOption> courseCombo;
    @FXML private Label courseHintLabel;
    @FXML private HBox starRatingBox;
    @FXML private Label ratingHintLabel;
    @FXML private TextField titleField;
    @FXML private TextArea commentArea;
    @FXML private Label formErrorLabel;
    @FXML private Button submitBtn;
    @FXML private Button cancelBtn;
    @FXML private ProgressIndicator submitLoading;

    private final ReviewService reviewService = new ReviewService();
    private final CourseService courseService = new CourseService();
    private final Map<String, Enrollment> enrollmentByCourseId = new HashMap<>();
    private final List<Button> starButtons = new ArrayList<>();
    private List<Review> cachedReviews = List.of();
    private int selectedRating = 5;

    private static final String[] RATING_LABELS = {
            "", "Poor — needs improvement", "Fair — room to grow",
            "Good — solid experience", "Very good — would recommend", "Excellent — outstanding!"
    };

    @FXML
    public void initialize() {
        buildStarRatingPicker();
        setRating(5);
        loadData();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleOpenWriteView() {
        showWriteView(null);
    }

    @FXML
    private void handleBackToList() {
        hideFormError();
        listView.setVisible(true);
        listView.setManaged(true);
        writeView.setVisible(false);
        writeView.setManaged(false);
    }

    @FXML
    private void handleSubmitReview() {
        CourseOption selected = courseCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFormError("Please select a course to review.");
            return;
        }
        if (selectedRating < 1 || selectedRating > 5) {
            showFormError("Please select a rating.");
            return;
        }
        String comment = commentArea.getText();
        if (comment == null || comment.isBlank()) {
            showFormError("Please write your review comment.");
            return;
        }

        String studentId = SessionManager.getInstance().getCurrentStudent().getId();
        Enrollment en = enrollmentByCourseId.get(selected.courseId());
        String enrollmentId = en != null ? en.getId() : null;

        submitLoading.setVisible(true);
        submitLoading.setManaged(true);
        submitBtn.setDisable(true);
        cancelBtn.setDisable(true);
        hideFormError();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                reviewService.submitReview(studentId, selected.courseId(), enrollmentId,
                        selectedRating, titleField.getText(), comment);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            submitLoading.setVisible(false);
            submitLoading.setManaged(false);
            submitBtn.setDisable(false);
            cancelBtn.setDisable(false);
            titleField.clear();
            commentArea.clear();
            setRating(5);
            ToastNotification.show(getStage(), "Review published successfully!", ToastNotification.Type.SUCCESS);
            handleBackToList();
            loadData();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            submitLoading.setVisible(false);
            submitLoading.setManaged(false);
            submitBtn.setDisable(false);
            cancelBtn.setDisable(false);
            Throwable err = task.getException();
            showFormError(err != null ? err.getMessage() : "Could not submit review");
        }));
        new Thread(task).start();
    }

    private void showWriteView(String courseIdToSelect) {
        listView.setVisible(false);
        listView.setManaged(false);
        writeView.setVisible(true);
        writeView.setManaged(true);
        AnimationUtils.fadeIn(writeView, 280);

        if (courseIdToSelect != null && courseCombo.getItems() != null) {
            for (CourseOption opt : courseCombo.getItems()) {
                if (courseIdToSelect.equals(opt.courseId())) {
                    courseCombo.getSelectionModel().select(opt);
                    prefillFromExisting(opt.courseId(), cachedReviews);
                    break;
                }
            }
        } else if (courseCombo.getSelectionModel().getSelectedItem() == null
                && courseCombo.getItems() != null && !courseCombo.getItems().isEmpty()) {
            courseCombo.getSelectionModel().selectFirst();
            prefillFromExisting(courseCombo.getItems().get(0).courseId(), cachedReviews);
        }
        hideFormError();
    }

    private void buildStarRatingPicker() {
        starRatingBox.getChildren().clear();
        starButtons.clear();
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            Button btn = new Button("★");
            btn.getStyleClass().add("star-btn");
            btn.setOnAction(e -> setRating(star));
            btn.setOnMouseEntered(e -> highlightStars(star));
            btn.setOnMouseExited(e -> highlightStars(selectedRating));
            starButtons.add(btn);
            starRatingBox.getChildren().add(btn);
        }
    }

    private void setRating(int rating) {
        selectedRating = Math.max(1, Math.min(5, rating));
        highlightStars(selectedRating);
        ratingHintLabel.setText(RATING_LABELS[selectedRating]);
    }

    private void highlightStars(int upTo) {
        for (int i = 0; i < starButtons.size(); i++) {
            Button btn = starButtons.get(i);
            btn.getStyleClass().remove("star-btn-active");
            if (i < upTo) {
                btn.getStyleClass().add("star-btn-active");
            }
        }
    }

    private void loadData() {
        reviewsList.getChildren().clear();
        reviewsList.getChildren().add(ComponentFactory.createLoadingState());
        String studentId = SessionManager.getInstance().getCurrentStudent().getId();

        Task<LoadResult> task = new Task<>() {
            @Override protected LoadResult call() {
                List<Review> reviews = reviewService.findReviewsByStudent(studentId);
                List<Enrollment> enrollments = courseService.getStudentEnrollments(studentId);
                return new LoadResult(reviews, enrollments);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            LoadResult result = task.getValue();
            cachedReviews = result.reviews() != null ? result.reviews() : List.of();
            buildReviewsList(cachedReviews);
            populateCourseCombo(result.enrollments(), cachedReviews);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            reviewsList.getChildren().clear();
            reviewsList.getChildren().add(ComponentFactory.createEmptyState("⚠️", "Failed to load reviews"));
        }));
        new Thread(task).start();
    }

    private void populateCourseCombo(List<Enrollment> enrollments, List<Review> existing) {
        enrollmentByCourseId.clear();
        Set<String> reviewedCourseIds = new HashSet<>();
        for (Review r : existing) {
            if (r.getCourseId() != null) reviewedCourseIds.add(r.getCourseId());
        }

        List<CourseOption> options = new ArrayList<>();
        if (enrollments != null) {
            for (Enrollment en : enrollments) {
                if (en.getCourseId() == null) continue;
                enrollmentByCourseId.put(en.getCourseId(), en);
                String title = en.getCourseTitle() != null ? en.getCourseTitle() : "Course";
                String instructor = en.getInstructorName() != null ? " · " + en.getInstructorName() : "";
                if (reviewedCourseIds.contains(en.getCourseId())) {
                    options.add(new CourseOption(en.getCourseId(), en.getId(), title + instructor + " (update)"));
                } else {
                    options.add(new CourseOption(en.getCourseId(), en.getId(), title + instructor));
                }
            }
        }
        courseCombo.setItems(FXCollections.observableArrayList(options));
        courseCombo.setDisable(options.isEmpty());
        submitBtn.setDisable(options.isEmpty());

        if (options.isEmpty()) {
            courseHintLabel.setText("Enroll in a course to leave a review.");
        } else {
            courseHintLabel.setText("Choose the course you want to rate");
            courseCombo.getSelectionModel().selectFirst();
        }

        courseCombo.setOnAction(ev -> {
            CourseOption opt = courseCombo.getSelectionModel().getSelectedItem();
            if (opt != null) {
                updateCourseHint(opt);
                prefillFromExisting(opt.courseId(), existing);
            }
        });

        CourseOption selected = courseCombo.getSelectionModel().getSelectedItem();
        if (selected != null) {
            updateCourseHint(selected);
        }
    }

    private void updateCourseHint(CourseOption opt) {
        Enrollment en = enrollmentByCourseId.get(opt.courseId());
        if (en != null && en.getInstructorName() != null) {
            courseHintLabel.setText("Instructor: " + en.getInstructorName());
        } else {
            courseHintLabel.setText("Share feedback for this course");
        }
    }

    private void prefillFromExisting(String courseId, List<Review> reviews) {
        for (Review r : reviews) {
            if (courseId.equals(r.getCourseId())) {
                setRating(r.getRating());
                titleField.setText(r.getTitle() != null ? r.getTitle() : "");
                commentArea.setText(r.getComment() != null ? r.getComment() : "");
                return;
            }
        }
        setRating(5);
        titleField.clear();
        commentArea.clear();
    }

    private void buildReviewsList(List<Review> reviews) {
        reviewsList.getChildren().clear();
        if (reviews == null || reviews.isEmpty()) {
            VBox empty = new VBox(16);
            empty.setAlignment(Pos.CENTER);
            empty.getChildren().add(ComponentFactory.createEmptyState("⭐", "No reviews yet — share your first experience!"));
            Button cta = new Button("Write your first review");
            cta.getStyleClass().add("btn-gradient");
            cta.setOnAction(e -> handleOpenWriteView());
            empty.getChildren().add(cta);
            reviewsList.getChildren().add(empty);
            return;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM d, yyyy");
        for (Review r : reviews) {
            VBox card = new VBox(10);
            card.getStyleClass().add("review-card");

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label stars = new Label("★".repeat(Math.max(1, Math.min(5, r.getRating()))));
            stars.getStyleClass().add("review-rating");
            Label ratingNum = new Label(r.getRating() + ".0");
            ratingNum.getStyleClass().add("review-rating");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label date = new Label(r.getCreatedAt() != null ? r.getCreatedAt().format(dtf) : "");
            date.getStyleClass().add("review-meta");
            header.getChildren().addAll(stars, ratingNum, spacer, date);

            Label courseTitle = new Label(r.getCourseTitle() != null ? r.getCourseTitle() : "Course");
            courseTitle.getStyleClass().add("review-course-title");

            if (r.getInstructorName() != null) {
                Label instructor = new Label("Instructor · " + r.getInstructorName());
                instructor.getStyleClass().add("review-meta");
                card.getChildren().add(instructor);
            }

            if (r.getTitle() != null && !r.getTitle().isBlank()) {
                Label title = new Label(r.getTitle());
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");
                card.getChildren().add(title);
            }

            Label comment = new Label(r.getComment() != null ? r.getComment() : "");
            comment.getStyleClass().add("review-comment");
            comment.setWrapText(true);

            HBox actions = new HBox();
            actions.setAlignment(Pos.CENTER_RIGHT);
            Button editBtn = new Button("Edit review →");
            editBtn.getStyleClass().add("review-card-edit-btn");
            String courseId = r.getCourseId();
            editBtn.setOnAction(e -> showWriteView(courseId));
            actions.getChildren().add(editBtn);

            card.getChildren().addAll(header, courseTitle, comment, actions);
            reviewsList.getChildren().add(card);
        }
    }

    private void showFormError(String msg) {
        formErrorLabel.setText(msg);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    private void hideFormError() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    private Stage getStage() {
        if (reviewsRoot != null && reviewsRoot.getScene() != null) {
            return (Stage) reviewsRoot.getScene().getWindow();
        }
        return reviewsList.getScene() != null ? (Stage) reviewsList.getScene().getWindow() : null;
    }

    private record CourseOption(String courseId, String enrollmentId, String label) {
        @Override public String toString() { return label; }
    }

    private record LoadResult(List<Review> reviews, List<Enrollment> enrollments) {}
}
