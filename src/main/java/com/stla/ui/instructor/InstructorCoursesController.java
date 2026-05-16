package com.stla.ui.instructor;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.Course;
import com.stla.services.DashboardService;
import com.stla.ui.components.ComponentFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class InstructorCoursesController {

    @javafx.fxml.FXML private VBox coursesContainer;
    private final DashboardService service = new DashboardService();
    private Consumer<Course> onManageCourse;

    /** Set by {@link InstructorDashboardController} to open the course manager. */
    public void setOnManageCourse(Consumer<Course> handler) {
        this.onManageCourse = handler;
    }

    @javafx.fxml.FXML
    public void initialize() {
        load();
    }

    private void load() {
        coursesContainer.getChildren().clear();
        coursesContainer.getChildren().add(ComponentFactory.createLoadingState());
        Task<List<Course>> task = new Task<>() {
            @Override protected List<Course> call() {
                return service.getInstructorCourses(SessionManager.getInstance().getCurrentInstructor().getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> build(task.getValue())));
        new Thread(task).start();
    }

    private void build(List<Course> courses) {
        coursesContainer.getChildren().clear();
        if (courses == null || courses.isEmpty()) {
            coursesContainer.getChildren().add(ComponentFactory.createEmptyState(
                    "📚", "You haven't created any courses yet.\nUse Create Course in the sidebar."));
            return;
        }
        for (Course c : courses) {
            coursesContainer.getChildren().add(buildCourseRow(c));
        }
    }

    private HBox buildCourseRow(Course c) {
        HBox row = new HBox(16);
        row.getStyleClass().add("card");
        row.setPadding(new Insets(16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.HAND);

        VBox thumb = new VBox();
        thumb.setPrefSize(60, 60);
        thumb.setStyle("-fx-background-color: linear-gradient(to bottom right, #0E4A54, #0F3D45); -fx-background-radius: 10;");
        thumb.setAlignment(Pos.CENTER);
        thumb.getChildren().add(new Label("📚") {{ setStyle("-fx-font-size: 24px;"); }});

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.getChildren().addAll(
                new Label(c.getTitle()) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 15px;"); }},
                new HBox(8) {{
                    setAlignment(Pos.CENTER_LEFT);
                    getChildren().addAll(
                            ComponentFactory.createStatusBadge(c.getStatus().getValue(), c.getStatus().getValue()),
                            new Label("$" + c.getPrice()) {{ setStyle("-fx-text-fill: #D44304; -fx-font-weight: bold;"); }},
                            new Label("👥 " + c.getEnrollmentCount()) {{ getStyleClass().add("text-secondary"); }},
                            new Label("⭐ " + (c.getRatingAvg() != null ? c.getRatingAvg() : "0.0")) {{ getStyleClass().add("course-rating"); }}
                    );
                }},
                new Label("Click to manage course →") {{ getStyleClass().add("text-secondary"); setStyle("-fx-font-size: 12px;"); }}
        );

        row.getChildren().addAll(thumb, info);
        row.setOnMouseClicked(e -> openManage(c));
        return row;
    }

    private void openManage(Course course) {
        if (onManageCourse != null) {
            onManageCourse.accept(course);
        }
    }
}
