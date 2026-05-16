package com.stla.ui.admin;

import com.stla.data.repositories.CourseRepositoryImpl;
import com.stla.domain.models.Course;
import com.stla.ui.components.AnimationUtils;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.CourseThumbnailLoader;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Admin course review list — compact cards with View button; details on a separate page.
 */
public class CourseReviewController {

    @FXML private StackPane rootStack;
    @FXML private VBox listPane;
    @FXML private VBox coursesContainer;
    @FXML private Label statusLabel;

    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();

    @FXML
    public void initialize() {
        loadPendingCourses();
    }

    private void loadPendingCourses() {
        Task<List<Course>> task = new Task<>() {
            @Override protected List<Course> call() {
                return courseRepo.findPendingReview();
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> buildList(getValue()));
            }
            @Override protected void failed() {
                Platform.runLater(() -> statusLabel.setText("Error loading courses."));
            }
        };
        new Thread(task).start();
    }

    private void buildList(List<Course> courses) {
        coursesContainer.getChildren().clear();
        if (courses == null || courses.isEmpty()) {
            statusLabel.setText("No pending courses to review.");
            coursesContainer.getChildren().add(
                ComponentFactory.createEmptyState("✅", "All courses have been reviewed!")
            );
            return;
        }
        statusLabel.setText(courses.size() + " course(s) pending review");
        for (Course course : courses) {
            coursesContainer.getChildren().add(buildSummaryCard(course));
        }
        AnimationUtils.staggerFadeIn(coursesContainer, 80);
    }

    private HBox buildSummaryCard(Course course) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-background-radius: 12;");

        StackPane thumb = new StackPane();
        thumb.setPrefSize(100, 64);
        thumb.setMinSize(100, 64);
        thumb.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8;");
        Label ph = new Label("📚");
        ImageView iv = new ImageView();
        iv.setFitWidth(100);
        iv.setFitHeight(64);
        iv.setPreserveRatio(true);
        thumb.getChildren().addAll(ph, iv);
        CourseThumbnailLoader.loadWithPlaceholder(course.getThumbnailUrl(), iv, ph);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(course.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        title.setWrapText(true);

        String sub = course.getSubtitle() != null && !course.getSubtitle().isBlank()
            ? course.getSubtitle() : "No subtitle";
        Label subtitle = new Label(sub);
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        subtitle.setWrapText(true);

        String meta = "👨‍🏫 " + (course.getInstructorName() != null ? course.getInstructorName() : "Unknown")
            + "  ·  📂 " + (course.getCategoryName() != null ? course.getCategoryName() : "—")
            + "  ·  💰 $" + (course.getPrice() != null ? course.getPrice() : "0");
        Label metaLbl = new Label(meta);
        metaLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");

        info.getChildren().addAll(title, subtitle, metaLbl);

        Button viewBtn = new Button("View");
        viewBtn.getStyleClass().addAll("btn-primary", "btn-sm");
        viewBtn.setStyle("-fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> openDetail(course));

        card.getChildren().addAll(thumb, info, viewBtn);
        return card;
    }

    private void openDetail(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/stla/views/admin/course-review-detail.fxml"));
            Parent detail = loader.load();
            CourseReviewDetailController ctrl = loader.getController();
            ctrl.init(course, this::showList, this::loadPendingCourses);
            rootStack.getChildren().setAll(detail);
        } catch (Exception e) {
            System.err.println("Error opening course review detail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showList() {
        rootStack.getChildren().setAll(listPane);
    }
}
