package com.stla.ui.components;

import com.stla.domain.enums.QuizAccessStatus;
import com.stla.domain.models.Quiz;
import com.stla.domain.models.QuizAttempt;
import com.stla.domain.models.StudentQuizSummary;
import com.stla.services.QuizService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Builds quiz summary cards for student course views. */
public final class StudentQuizUiHelper {

    private StudentQuizUiHelper() {}

    public static VBox buildQuizCard(StudentQuizSummary summary, Consumer<String> onStartQuiz) {
        return buildQuizCard(summary, onStartQuiz, null);
    }

    public static VBox buildQuizCard(StudentQuizSummary summary, Consumer<String> onStartQuiz, String studentId) {
        Quiz quiz = summary.getQuiz();
        VBox card = new VBox(10);
        card.getStyleClass().addAll("quiz-summary-card");
        if (summary.isLocked()) card.getStyleClass().add("quiz-summary-locked");
        card.setPadding(new Insets(14));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.getChildren().add(new Label(quiz.getTitle()) {{
            setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
            setWrapText(true);
        }});
        titleBox.getChildren().add(new Label(summary.getQuestionCount() + " questions • Pass "
                + quiz.getPassingScore() + "% • " + formatAttempts(summary)) {{
            getStyleClass().add("text-secondary");
        }});
        Label statusBadge = new Label(summary.getStatus().getLabel());
        statusBadge.getStyleClass().add("quiz-badge-quiz");
        if (summary.getStatus() == QuizAccessStatus.COMPLETED) statusBadge.getStyleClass().add("quiz-badge-pass");
        if (summary.isLocked()) statusBadge.getStyleClass().add("quiz-badge-locked");
        header.getChildren().addAll(titleBox, statusBadge);

        HBox meta = new HBox(16);
        if (quiz.getTimeLimitMinutes() != null && quiz.getTimeLimitMinutes() > 0) {
            meta.getChildren().add(new Label("⏱ " + quiz.getTimeLimitMinutes() + " min"));
        }
        if (summary.getAttemptsUsed() > 0) {
            meta.getChildren().add(new Label("Best: " + String.format("%.0f%%", summary.getBestScorePercent())));
        }
        card.getChildren().addAll(header, meta);

        if (summary.isLocked()) {
            card.getChildren().add(new Label("🔒 Enroll in this course to access quizzes.") {{
                setWrapText(true);
                setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            }});
        } else {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            if (summary.getAttemptsRemaining() > 0) {
                Button start = new Button(summary.getAttemptsUsed() > 0 ? "Retry Quiz" : "Start Quiz");
                start.getStyleClass().addAll("btn-primary", "btn-sm");
                start.setOnAction(e -> { if (onStartQuiz != null) onStartQuiz.accept(quiz.getId()); });
                actions.getChildren().add(start);
            } else {
                actions.getChildren().add(new Label("No attempts remaining") {{
                    getStyleClass().add("text-secondary");
                }});
            }
            card.getChildren().add(actions);
        }

        if (studentId != null && summary.getAttemptsUsed() > 0) {
            VBox history = new VBox(4);
            history.getChildren().add(new Label("Previous attempts") {{
                setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
            }});
            QuizService quizService = new QuizService();
            for (QuizAttempt a : quizService.getStudentQuizAttempts(quiz.getId(), studentId)) {
                String date = a.getSubmittedAt() != null ? a.getSubmittedAt().toLocalDate().toString()
                        : (a.getCompletedAt() != null ? a.getCompletedAt().toLocalDate().toString() : "—");
                history.getChildren().add(new Label("Attempt " + Math.max(1, a.getAttemptNumber())
                        + " • " + String.format("%.0f%%", a.getScorePercent())
                        + (a.isPassed() ? " ✅ Passed" : " ❌ Failed") + " • " + date) {{
                    setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
                }});
            }
            card.getChildren().add(history);
        }
        return card;
    }

    private static String formatAttempts(StudentQuizSummary s) {
        Quiz q = s.getQuiz();
        return s.getAttemptsUsed() + "/" + q.getAttemptsAllowed() + " attempts";
    }
}
