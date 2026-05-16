package com.stla.ui.components;

import com.stla.domain.models.Course;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public final class CourseCardFactory {

    private CourseCardFactory() {}

    public static VBox createCard(Course course, Runnable onOpen, Runnable onEnroll) {
        return createCard(course, false, 0, onOpen, onEnroll);
    }

    public static VBox createCard(Course course, boolean enrolled, Runnable onOpen, Runnable onEnroll) {
        return createCard(course, enrolled, 0, onOpen, onEnroll);
    }

    public static VBox createCard(Course course, boolean enrolled, int quizCount, Runnable onOpen, Runnable onEnroll) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    CourseCardFactory.class.getResource("/com/stla/views/components/course-card.fxml"));
            loader.load();
            CourseCardController ctrl = loader.getController();
            ctrl.bind(course, enrolled, quizCount, onOpen, onEnroll != null ? onEnroll : onOpen);
            return ctrl.getRoot();
        } catch (IOException e) {
            System.err.println("Course card load error: " + e.getMessage());
            return ComponentFactory.createCourseCard(
                    course.getTitle(),
                    course.getLevel() != null ? course.getLevel().getValue() : "",
                    course.getPrice() != null ? "$" + course.getPrice() : "Free",
                    course.getRatingAvg() != null ? course.getRatingAvg().toString() : "0.0",
                    course.getEnrollmentCount(),
                    onOpen);
        }
    }
}
