package com.stla.ui.components;

import com.stla.domain.models.Course;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.math.BigDecimal;

public class CourseCardController {

    @FXML private VBox cardRoot;
    @FXML private StackPane thumbnailPane;
    @FXML private Region thumbSkeleton;
    @FXML private VBox thumbPlaceholder;
    @FXML private ImageView thumbnailView;
    @FXML private Label titleLabel;
    @FXML private Label instructorLabel;
    @FXML private Label ratingLabel;
    @FXML private Label studentsLabel;
    @FXML private Label levelLabel;
    @FXML private Label durationLabel;
    @FXML private Label quizBadgeLabel;
    @FXML private Label certificateBadgeLabel;
    @FXML private Label priceLabel;
    @FXML private Button enrollButton;
    @FXML private Button favoriteButton;

    private Course course;
    private Runnable onCardClick;
    private Runnable onEnrollClick;
    private boolean favorited;

    @FXML
    public void initialize() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(thumbnailPane.widthProperty());
        clip.heightProperty().bind(thumbnailPane.heightProperty());
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        thumbnailPane.setClip(clip);
        thumbnailView.fitWidthProperty().bind(thumbnailPane.widthProperty());
        thumbnailView.fitHeightProperty().bind(thumbnailPane.heightProperty());

        cardRoot.setOnMouseClicked(e -> {
            if (onCardClick != null && !e.isConsumed()) {
                onCardClick.run();
                e.consume();
            }
        });
        cardRoot.setCursor(Cursor.HAND);

        favoriteButton.setOnAction(e -> e.consume());
    }

    public void bind(Course course, Runnable onCardClick, Runnable onEnrollClick) {
        bind(course, false, onCardClick, onEnrollClick);
    }

    public void bind(Course course, boolean enrolled, int quizCount, Runnable onCardClick, Runnable onEnrollClick) {
        this.course = course;
        this.onCardClick = onCardClick;
        this.onEnrollClick = onEnrollClick;

        titleLabel.setText(course.getTitle() != null ? course.getTitle() : "Untitled Course");
        instructorLabel.setText(course.getInstructorName() != null ? course.getInstructorName() : "Instructor");
        ratingLabel.setText("⭐ " + (course.getRatingAvg() != null ? course.getRatingAvg() : "0.0"));
        studentsLabel.setText("👥 " + course.getEnrollmentCount());

        if (course.getLevel() != null) {
            levelLabel.setText(course.getLevel().getValue().toUpperCase());
            levelLabel.setVisible(true);
            levelLabel.setManaged(true);
        } else {
            levelLabel.setVisible(false);
            levelLabel.setManaged(false);
        }

        int hours = course.getEstimatedHours() != null ? course.getEstimatedHours() : 0;
        durationLabel.setText(hours > 0 ? "⏱ " + hours + "h" : "⏱ —");

        BigDecimal price = course.getPrice();
        priceLabel.setText(price == null || price.compareTo(BigDecimal.ZERO) == 0 ? "Free" : "$" + price);

        enrollButton.getStyleClass().removeAll("btn-primary", "btn-accent");
        if (enrolled) {
            enrollButton.setText("Continue Learning →");
            enrollButton.getStyleClass().add("btn-primary");
        } else {
            enrollButton.setText(price == null || price.compareTo(BigDecimal.ZERO) == 0 ? "Enroll Free" : "Enroll Now");
            enrollButton.getStyleClass().add("btn-accent");
        }

        int quizzes = quizCount > 0 ? quizCount : (course.isHasQuiz() ? 1 : 0);
        boolean showQuiz = quizzes > 0;
        quizBadgeLabel.setText("🎯 " + quizzes + (quizzes == 1 ? " Quiz" : " Quizzes"));
        quizBadgeLabel.setVisible(showQuiz);
        quizBadgeLabel.setManaged(showQuiz);
        certificateBadgeLabel.setVisible(course.isHasCertificate());
        certificateBadgeLabel.setManaged(course.isHasCertificate());

        enrollButton.setOnAction(e -> {
            e.consume();
            handleEnroll();
        });

        loadThumbnail(course.getThumbnailUrl());
    }

    public void bind(Course course, boolean enrolled, Runnable onCardClick, Runnable onEnrollClick) {
        bind(course, enrolled, 0, onCardClick, onEnrollClick);
    }

    public VBox getRoot() { return cardRoot; }

    public Course getCourse() { return course; }

    private void loadThumbnail(String url) {
        thumbSkeleton.setVisible(true);
        thumbSkeleton.setManaged(true);
        thumbPlaceholder.setVisible(false);
        thumbPlaceholder.setManaged(false);
        thumbnailView.setVisible(false);
        thumbnailView.setImage(null);

        Runnable showPlaceholder = () -> {
            thumbSkeleton.setVisible(false);
            thumbSkeleton.setManaged(false);
            thumbPlaceholder.setVisible(true);
            thumbPlaceholder.setManaged(true);
            thumbnailView.setVisible(false);
        };

        Runnable showImage = () -> {
            thumbSkeleton.setVisible(false);
            thumbSkeleton.setManaged(false);
            thumbPlaceholder.setVisible(false);
            thumbPlaceholder.setManaged(false);
            thumbnailView.setVisible(true);
        };

        CourseThumbnailLoader.load(url, thumbnailView, showImage, showPlaceholder);
    }

    @FXML
    private void handleEnroll() {
        if (onEnrollClick != null) onEnrollClick.run();
        else if (onCardClick != null) onCardClick.run();
    }

    @FXML
    private void handleFavorite() {
        favorited = !favorited;
        favoriteButton.setText(favorited ? "♥" : "♡");
        favoriteButton.getStyleClass().remove("course-favorite-active");
        if (favorited) favoriteButton.getStyleClass().add("course-favorite-active");
    }
}
