package com.stla.ui.student;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.Enrollment;
import com.stla.domain.models.Review;
import com.stla.services.ReviewService;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class StudentRateInstructorController {

    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label courseTitleLabel;
    @FXML private Label instructorLabel;
    @FXML private HBox starRatingBox;
    @FXML private Label ratingHintLabel;
    @FXML private TextField titleField;
    @FXML private TextArea commentArea;
    @FXML private Label formErrorLabel;
    @FXML private Button submitBtn;
    @FXML private Button cancelBtn;
    @FXML private ProgressIndicator submitLoading;

    private final ReviewService reviewService = new ReviewService();
    private final List<Button> starButtons = new ArrayList<>();
    private Enrollment enrollment;
    private Runnable onBack;
    private Consumer<Enrollment> onSubmitted;
    private int selectedRating = 5;

    private static final String[] RATING_LABELS = {
            "", "Poor — needs improvement", "Fair — room to grow",
            "Good — solid experience", "Very good — would recommend", "Excellent — outstanding!"
    };

    @FXML
    public void initialize() {
        buildStarRatingPicker();
        setRating(5);
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
        if (enrollment == null) return;

        String title = enrollment.getCourseTitle() != null ? enrollment.getCourseTitle() : "Course";
        courseTitleLabel.setText(title);

        if (enrollment.getInstructorName() != null && !enrollment.getInstructorName().isBlank()) {
            instructorLabel.setText("👨‍🏫 " + enrollment.getInstructorName());
            pageSubtitleLabel.setText("Your review helps " + enrollment.getInstructorName() + " and other students.");
        } else {
            instructorLabel.setText("");
            pageSubtitleLabel.setText("Tell us how your learning experience went.");
        }

        loadExistingReview();
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setOnSubmitted(Consumer<Enrollment> onSubmitted) {
        this.onSubmitted = onSubmitted;
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    @FXML
    private void handleSubmit() {
        if (enrollment == null || enrollment.getCourseId() == null) {
            showFormError("Course information is missing.");
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
        submitLoading.setVisible(true);
        submitLoading.setManaged(true);
        submitBtn.setDisable(true);
        cancelBtn.setDisable(true);
        hideFormError();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                reviewService.submitReview(studentId, enrollment.getCourseId(), enrollment.getId(),
                        selectedRating, titleField.getText(), comment);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            submitLoading.setVisible(false);
            submitLoading.setManaged(false);
            submitBtn.setDisable(false);
            cancelBtn.setDisable(false);
            ToastNotification.show(getStage(), "Thank you! Your review was submitted.", ToastNotification.Type.SUCCESS);
            if (onSubmitted != null) {
                onSubmitted.accept(enrollment);
            } else if (onBack != null) {
                onBack.run();
            }
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

    private void loadExistingReview() {
        if (enrollment == null || enrollment.getCourseId() == null) return;
        String studentId = SessionManager.getInstance().getCurrentStudent().getId();

        Task<Optional<Review>> task = new Task<>() {
            @Override protected Optional<Review> call() {
                return reviewService.findStudentReview(studentId, enrollment.getCourseId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Optional<Review> existing = task.getValue();
            if (existing.isPresent()) {
                Review r = existing.get();
                pageTitleLabel.setText("Update Your Review");
                submitBtn.setText("Update Review");
                setRating(r.getRating());
                titleField.setText(r.getTitle() != null ? r.getTitle() : "");
                commentArea.setText(r.getComment() != null ? r.getComment() : "");
            } else {
                pageTitleLabel.setText("Rate Instructor");
                submitBtn.setText("Submit Review");
                setRating(5);
                titleField.clear();
                commentArea.clear();
            }
        }));
        new Thread(task).start();
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
        return courseTitleLabel.getScene() != null
                ? (Stage) courseTitleLabel.getScene().getWindow() : null;
    }
}
