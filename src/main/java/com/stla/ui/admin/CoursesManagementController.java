package com.stla.ui.admin;

import com.stla.domain.models.Course;
import com.stla.data.repositories.CourseRepositoryImpl;
import com.stla.ui.components.ComponentFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class CoursesManagementController {
    @javafx.fxml.FXML private ComboBox<String> statusFilter;
    @javafx.fxml.FXML private VBox coursesContainer;

    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();

    @javafx.fxml.FXML public void initialize() {
        statusFilter.getItems().addAll("All", "approved", "rejected");
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> load());
        load();
    }

    private void load() {
        coursesContainer.getChildren().clear();
        coursesContainer.getChildren().add(ComponentFactory.createLoadingState());
        String status = statusFilter.getValue();
        Task<List<Course>> task = new Task<>() {
            @Override protected List<Course> call() {
                if ("All".equals(status)) {
                    return courseRepo.findForAdminManagement();
                }
                return courseRepo.findByStatus(status);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> build(task.getValue())));
        new Thread(task).start();
    }

    private void build(List<Course> courses) {
        coursesContainer.getChildren().clear();
        if (courses.isEmpty()) {
            coursesContainer.getChildren().add(ComponentFactory.createEmptyState("📚", "No courses found"));
            return;
        }
        for (Course c : courses) {
            HBox row = new HBox(16);
            row.getStyleClass().add("card");
            row.setPadding(new Insets(14, 16, 14, 16));
            row.setAlignment(Pos.CENTER_LEFT);

            Label title = new Label(c.getTitle());
            title.setPrefWidth(280);
            title.setStyle("-fx-font-weight: bold;");

            HBox statusBox = new HBox(ComponentFactory.createStatusBadge(c.getStatus().getValue(), c.getStatus().getValue()));
            statusBox.setPrefWidth(100);

            Label price = new Label("$" + c.getPrice());
            price.setPrefWidth(80);
            price.setStyle("-fx-text-fill: #D44304; -fx-font-weight: bold;");

            Label students = new Label("👥 " + c.getEnrollmentCount());
            students.setPrefWidth(80);

            row.getChildren().addAll(title, statusBox, price, students);
            coursesContainer.getChildren().add(row);
        }
    }
}
