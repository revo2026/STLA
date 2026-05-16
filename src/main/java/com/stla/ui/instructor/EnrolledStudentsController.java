package com.stla.ui.instructor;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.Enrollment;
import com.stla.services.CourseService;
import com.stla.ui.components.ComponentFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class EnrolledStudentsController {
    @javafx.fxml.FXML private VBox studentsContainer;
    private final CourseService service = new CourseService();

    @javafx.fxml.FXML public void initialize() { load(); }

    private void load() {
        studentsContainer.getChildren().clear();
        studentsContainer.getChildren().add(ComponentFactory.createLoadingState());
        Task<List<Enrollment>> task = new Task<>() {
            @Override protected List<Enrollment> call() {
                return service.getEnrolledStudentsForInstructor(SessionManager.getInstance().getCurrentInstructor().getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> build(task.getValue())));
        new Thread(task).start();
    }

    private void build(List<Enrollment> enrollments) {
        studentsContainer.getChildren().clear();
        if (enrollments.isEmpty()) {
            studentsContainer.getChildren().add(ComponentFactory.createEmptyState("👥", "No students enrolled yet"));
            return;
        }
        // Table-style header
        HBox header = new HBox(16);
        header.setPadding(new Insets(8, 16, 8, 16));
        header.getChildren().addAll(
            makeHeaderLabel("Student", 200), makeHeaderLabel("Course", 250),
            makeHeaderLabel("Progress", 180), makeHeaderLabel("Enrolled", 120),
            makeHeaderLabel("Status", 100)
        );
        studentsContainer.getChildren().add(header);

        for (Enrollment en : enrollments) {
            HBox row = new HBox(16);
            row.getStyleClass().add("card");
            row.setPadding(new Insets(12, 16, 12, 16));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(
                makeCell(en.getStudentName(), 200, true),
                makeCell(en.getCourseTitle(), 250, false),
                wrapInBox(ComponentFactory.createProgressBar(en.getProgressPercent()), 180),
                makeCell(en.getEnrolledAt() != null ? en.getEnrolledAt().format(DateTimeFormatter.ofPattern("MMM d, yy")) : "—", 120, false),
                wrapInBox(ComponentFactory.createStatusBadge(en.getStatus().getValue(), en.getStatus().getValue()), 100)
            );
            studentsContainer.getChildren().add(row);
        }
    }

    private Label makeHeaderLabel(String text, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        l.setPrefWidth(width);
        return l;
    }

    private Label makeCell(String text, double width, boolean bold) {
        Label l = new Label(text != null ? text : "—");
        l.setPrefWidth(width);
        if (bold) l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private HBox wrapInBox(javafx.scene.Node node, double width) {
        HBox box = new HBox(node);
        box.setPrefWidth(width);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
