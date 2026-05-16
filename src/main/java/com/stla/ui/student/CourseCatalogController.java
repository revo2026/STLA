package com.stla.ui.student;

import com.stla.core.navigation.StudentNavigationContext;
import com.stla.core.session.SessionManager;
import com.stla.domain.models.*;
import com.stla.services.DashboardService;
import com.stla.services.EnrollmentService;
import com.stla.services.QuizService;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.CourseCardFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CourseCatalogController {

    @javafx.fxml.FXML private TextField searchField;
    @javafx.fxml.FXML private ComboBox<String> categoryFilter;
    @javafx.fxml.FXML private ComboBox<String> levelFilter;
    @javafx.fxml.FXML private FlowPane coursesGrid;
    @javafx.fxml.FXML private Label resultCount;

    private final DashboardService service = new DashboardService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final QuizService quizService = new QuizService();
    private List<Course> allCourses;
    private Set<String> enrolledCourseIds = Set.of();
    private Map<String, Integer> quizCountByCourse = Map.of();
    private final Map<String, String> categoryNameToId = new HashMap<>();
    private java.util.function.Consumer<String> onCourseSelected;
    private java.util.function.Consumer<String> onContinueLearning;

    public void setOnCourseSelected(java.util.function.Consumer<String> onCourseSelected) {
        this.onCourseSelected = onCourseSelected;
        if (allCourses != null) {
            filterCourses();
        }
    }

    public void setOnContinueLearning(java.util.function.Consumer<String> onContinueLearning) {
        this.onContinueLearning = onContinueLearning;
        if (allCourses != null) {
            filterCourses();
        }
    }

    @javafx.fxml.FXML
    public void initialize() {
        levelFilter.getItems().addAll("All Levels", "beginner", "intermediate", "advanced", "expert");
        levelFilter.setValue("All Levels");
        categoryFilter.getItems().add("All Categories");
        categoryFilter.setValue("All Categories");

        new Thread(() -> {
            List<Category> cats = service.getActiveCategories();
            Platform.runLater(() -> {
                for (Category c : cats) {
                    if (c.getName() != null && !c.getName().isBlank()) {
                        categoryFilter.getItems().add(c.getName());
                        if (c.getId() != null) {
                            categoryNameToId.put(c.getName(), c.getId());
                        }
                    }
                }
                if (allCourses != null) {
                    filterCourses();
                }
            });
        }).start();

        searchField.textProperty().addListener((o, ov, nv) -> filterCourses());
        categoryFilter.setOnAction(e -> filterCourses());
        levelFilter.setOnAction(e -> filterCourses());

        loadCourses();
    }

    private void loadCourses() {
        coursesGrid.getChildren().clear();
        coursesGrid.getChildren().add(ComponentFactory.createLoadingState());

        Task<CatalogLoadResult> task = new Task<>() {
            @Override protected CatalogLoadResult call() {
                List<Course> courses = service.getApprovedCourses();
                Set<String> enrolled = new HashSet<>();
                var student = SessionManager.getInstance().getCurrentStudent();
                if (student != null) {
                    enrolled.addAll(enrollmentService.getEnrolledCourseIds(student.getId()));
                }
                Map<String, Integer> quizCounts = new HashMap<>();
                for (Course c : courses) {
                    if (c.getId() != null) {
                        quizCounts.put(c.getId(), quizService.countPublishedQuizzes(c.getId()));
                    }
                }
                return new CatalogLoadResult(courses, enrolled, quizCounts);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            CatalogLoadResult result = task.getValue();
            allCourses = result.courses();
            enrolledCourseIds = result.enrolledCourseIds();
            quizCountByCourse = result.quizCountByCourse();
            displayCourses(allCourses);
        }));
        new Thread(task).start();
    }

    private void filterCourses() {
        if (allCourses == null) return;
        String keyword = searchField.getText().toLowerCase().trim();
        String cat = categoryFilter.getValue();
        String lvl = levelFilter.getValue();

        List<Course> filtered = allCourses.stream().filter(c -> {
            boolean matchKeyword = keyword.isEmpty()
                    || (c.getTitle() != null && c.getTitle().toLowerCase().contains(keyword))
                    || (c.getSubtitle() != null && c.getSubtitle().toLowerCase().contains(keyword));
            boolean matchLevel = lvl == null || "All Levels".equals(lvl)
                    || (c.getLevel() != null && c.getLevel().getValue().equalsIgnoreCase(lvl));
            boolean matchCategory = matchesCategory(cat, c);
            return matchKeyword && matchLevel && matchCategory;
        }).toList();
        displayCourses(filtered);
    }

    private void displayCourses(List<Course> courses) {
        coursesGrid.getChildren().clear();
        resultCount.setText(courses.size() + " courses found");
        if (courses.isEmpty()) {
            coursesGrid.getChildren().add(ComponentFactory.createEmptyState("🔍", "No courses match your search"));
            return;
        }
        for (Course c : courses) {
            String id = c.getId();
            if (id == null || id.isBlank()) continue;
            boolean enrolled = enrolledCourseIds.contains(id);
            int quizCount = quizCountByCourse.getOrDefault(id, 0);
            coursesGrid.getChildren().add(
                    CourseCardFactory.createCard(
                            c,
                            enrolled,
                            quizCount,
                            () -> openCourse(id),
                            () -> {
                                if (enrolled) continueCourse(id);
                                else openCourse(id);
                            }));
        }
    }

    private void openCourse(String courseId) {
        if (onCourseSelected != null) {
            onCourseSelected.accept(courseId);
        } else {
            StudentNavigationContext.goToCourseDetails(courseId);
        }
    }

    private void continueCourse(String courseId) {
        if (onContinueLearning != null) {
            onContinueLearning.accept(courseId);
        } else {
            StudentNavigationContext.goToCoursePlayer(courseId);
        }
    }

    private boolean matchesCategory(String selectedCategory, Course course) {
        if (selectedCategory == null || "All Categories".equals(selectedCategory)) {
            return true;
        }
        String selectedId = categoryNameToId.get(selectedCategory);
        if (course.getCategoryName() != null
                && course.getCategoryName().equalsIgnoreCase(selectedCategory.trim())) {
            return true;
        }
        return selectedId != null && selectedId.equals(course.getCategoryId());
    }

    @javafx.fxml.FXML private void handleSearch() { filterCourses(); }

    private record CatalogLoadResult(List<Course> courses, Set<String> enrolledCourseIds,
                                     Map<String, Integer> quizCountByCourse) {}
}
