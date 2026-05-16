package com.stla.ui.student;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.StudentQuizSummary;
import com.stla.services.QuizService;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.StudentQuizUiHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StudentQuizzesController {

    @FXML private VBox quizzesContainer;

    private final QuizService quizService = new QuizService();
    private Consumer<String> onStartQuiz;

    public void setOnStartQuiz(Consumer<String> onStartQuiz) {
        this.onStartQuiz = onStartQuiz;
        if (quizzesContainer != null) {
            loadQuizzes();
        }
    }

    @FXML
    public void initialize() {
        if (quizzesContainer.getChildren().isEmpty()) {
            quizzesContainer.getChildren().add(ComponentFactory.createLoadingState());
        }
    }

    public void refresh() {
        loadQuizzes();
    }

    private void loadQuizzes() {
        var student = SessionManager.getInstance().getCurrentStudent();
        if (student == null) {
            showEmpty("Sign in to view your quizzes.");
            return;
        }

        new Thread(() -> {
            List<StudentQuizSummary> summaries = quizService.getAllStudentQuizzes(student.getId());
            Platform.runLater(() -> display(summaries, student.getId()));
        }).start();
    }

    private void display(List<StudentQuizSummary> summaries, String studentId) {
        quizzesContainer.getChildren().clear();
        if (summaries.isEmpty()) {
            showEmpty("No quizzes yet. Enroll in a course that includes quizzes to see them here.");
            return;
        }

        Map<String, List<StudentQuizSummary>> byCourse = new LinkedHashMap<>();
        for (StudentQuizSummary s : summaries) {
            String key = s.getCourseTitle() != null ? s.getCourseTitle() : "Course";
            byCourse.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(s);
        }

        for (Map.Entry<String, List<StudentQuizSummary>> entry : byCourse.entrySet()) {
            Label courseHeader = new Label("📚 " + entry.getKey());
            courseHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 8 0 4 0;");
            quizzesContainer.getChildren().add(courseHeader);

            for (StudentQuizSummary s : entry.getValue()) {
                quizzesContainer.getChildren().add(
                        StudentQuizUiHelper.buildQuizCard(s, quizId -> {
                            if (onStartQuiz != null) onStartQuiz.accept(quizId);
                        }, studentId));
            }
        }
    }

    private void showEmpty(String message) {
        quizzesContainer.getChildren().clear();
        quizzesContainer.getChildren().add(ComponentFactory.createEmptyState("🎯", message));
    }
}
