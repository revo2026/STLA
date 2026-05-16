package com.stla.ui.student;

import com.stla.domain.enums.QuestionType;
import com.stla.domain.models.Quiz;
import com.stla.domain.models.QuizAttempt;
import com.stla.domain.models.QuizOption;
import com.stla.domain.models.QuizQuestion;
import com.stla.domain.models.QuestionSequenceItem;
import com.stla.ui.components.AnimationUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class QuizResultController {

    @FXML private VBox rootBox;
    @FXML private Label quizTitleLabel;
    @FXML private Label resultBadgeLabel;
    @FXML private Label scoreLabel;
    @FXML private Label pointsLabel;
    @FXML private Label attemptLabel;
    @FXML private Label passingLabel;
    @FXML private Label correctLabel;
    @FXML private Label wrongLabel;
    @FXML private VBox reviewBox;
    @FXML private Button reviewButton;
    @FXML private Button continueButton;

    private Quiz quiz;
    private QuizAttempt attempt;
    private List<QuizQuestion> questions;
    private Map<String, QuizResponseSnapshot> snapshots;
    private Consumer<String> onContinue;
    private boolean reviewBuilt;

    public void setOnContinue(Consumer<String> onContinue) { this.onContinue = onContinue; }

    public void showResult(Quiz quiz, QuizAttempt attempt, List<QuizQuestion> questions,
                           Map<String, QuizResponseSnapshot> snapshots) {
        this.quiz = quiz;
        this.attempt = attempt;
        this.questions = questions;
        this.snapshots = snapshots;
        this.reviewBuilt = false;

        quizTitleLabel.setText(quiz.getTitle());
        double pct = attempt.getScorePercent();
        scoreLabel.setText(String.format("%.0f%%", pct));
        scoreLabel.setStyle("-fx-text-fill: " + (attempt.isPassed() ? "#10B981" : "#EF4444") + ";");

        resultBadgeLabel.getStyleClass().removeAll("quiz-badge-pass", "quiz-badge-fail");
        if (attempt.isPassed()) {
            resultBadgeLabel.setText("🎉 PASSED");
            resultBadgeLabel.getStyleClass().add("quiz-badge-pass");
        } else {
            resultBadgeLabel.setText("❌ NOT PASSED");
            resultBadgeLabel.getStyleClass().add("quiz-badge-fail");
        }

        pointsLabel.setText(attempt.getScore() + " / " + attempt.getTotalPoints() + " questions correct");
        attemptLabel.setText("Attempt " + Math.max(1, attempt.getAttemptNumber()));
        passingLabel.setText("Passing score: " + quiz.getPassingScore() + "%");
        correctLabel.setText("✅ " + attempt.getScore() + " correct");
        wrongLabel.setText("❌ " + Math.max(0, attempt.getTotalPoints() - attempt.getScore()) + " wrong");

        reviewBox.getChildren().clear();
        reviewBox.setVisible(false);
        reviewBox.setManaged(false);

        if (quiz.isShowAnswersAfterSubmit() && snapshots != null && !snapshots.isEmpty()) {
            reviewButton.setVisible(true);
            reviewButton.setManaged(true);
            reviewButton.setText("Review Answers");
        } else {
            reviewButton.setVisible(false);
            reviewButton.setManaged(false);
        }
        AnimationUtils.fadeIn(rootBox, 350);
    }

    @FXML
    private void handleReview() {
        if (!reviewBuilt) {
            buildReview();
            reviewBuilt = true;
        }
        boolean show = !reviewBox.isVisible();
        reviewBox.setVisible(show);
        reviewBox.setManaged(show);
        reviewButton.setText(show ? "Hide Review" : "Review Answers");
        if (show) AnimationUtils.fadeIn(reviewBox, 250);
    }

    private void buildReview() {
        reviewBox.getChildren().clear();
        reviewBox.getChildren().add(sectionTitle("Answer review"));

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            QuizResponseSnapshot snap = snapshots.get(q.getId());
            boolean ok = snap != null && snap.correct();
            QuestionType type = QuestionType.fromDbValue(q.getQuestionType());

            VBox card = new VBox(8);
            card.getStyleClass().add("quiz-summary-card");
            card.setPadding(new Insets(14));
            card.setStyle("-fx-background-color: " + (ok ? "#ECFDF5" : "#FEF2F2") + ";");

            Label qLabel = new Label((ok ? "✅" : "❌") + " Q" + (i + 1) + ": " + q.getQuestionText());
            qLabel.setWrapText(true);
            qLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            card.getChildren().add(qLabel);

            if (type.usesOptions() && q.getOptions() != null) {
                List<String> userIds = snap != null && snap.selectedOptionIds() != null
                        ? Arrays.asList(snap.selectedOptionIds()) : List.of();
                for (QuizOption opt : q.getOptions()) {
                    boolean picked = userIds.contains(opt.getId());
                    boolean correct = opt.isCorrect();
                    String prefix = correct ? "✅ " : "   ";
                    String suffix = picked ? " ← Your answer" : "";
                    Label optLabel = new Label(prefix + opt.getOptionText() + suffix);
                    optLabel.setWrapText(true);
                    String color = correct ? "#059669" : (picked ? "#DC2626" : "#6B7280");
                    optLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
                    card.getChildren().add(optLabel);
                }
            } else if (type == QuestionType.SHORT_ANSWER || type == QuestionType.FILL_BLANK) {
                String userAns = snap != null && snap.answerText() != null && !snap.answerText().isBlank()
                        ? snap.answerText() : "(no answer)";
                card.getChildren().add(metaLabel("Your answer: " + userAns));
                if (q.getAcceptedAnswers() != null && !q.getAcceptedAnswers().isEmpty()) {
                    String accepted = q.getAcceptedAnswers().stream()
                            .map(a -> a.getAnswerText())
                            .reduce((a, b) -> a + ", " + b).orElse("");
                    card.getChildren().add(metaLabel("Accepted: " + accepted));
                }
            } else if (type == QuestionType.ESSAY) {
                String essay = snap != null && snap.answerText() != null ? snap.answerText() : "(no answer)";
                Label essayLabel = new Label(essay);
                essayLabel.setWrapText(true);
                essayLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
                card.getChildren().add(essayLabel);
                card.getChildren().add(metaLabel("📝 Essay — requires manual grading"));
            } else if (type == QuestionType.MATCHING) {
                card.getChildren().add(metaLabel("Your matches: "
                        + (snap != null && snap.matchingAnswers() != null ? snap.matchingAnswers() : "(none)")));
            } else if (type == QuestionType.ORDERING && q.getSequenceItems() != null) {
                if (snap != null && snap.orderingAnswer() != null) {
                    for (int j = 0; j < snap.orderingAnswer().length; j++) {
                        String itemId = snap.orderingAnswer()[j];
                        String text = q.getSequenceItems().stream()
                                .filter(si -> si.getId().equals(itemId))
                                .map(QuestionSequenceItem::getItemText)
                                .findFirst().orElse(itemId);
                        card.getChildren().add(metaLabel((j + 1) + ". " + text));
                    }
                } else {
                    card.getChildren().add(metaLabel("(no ordering submitted)"));
                }
            }

            if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                Label expl = new Label("💡 " + q.getExplanation());
                expl.setWrapText(true);
                expl.setStyle("-fx-text-fill: #7C3AED; -fx-font-size: 11px; -fx-font-style: italic;");
                card.getChildren().add(expl);
            }
            reviewBox.getChildren().add(card);
        }
    }

    @FXML
    private void handleContinue() {
        if (onContinue != null && quiz != null) onContinue.accept(quiz.getCourseId());
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        return l;
    }

    private Label metaLabel(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        return l;
    }

    public record QuizResponseSnapshot(
            boolean correct,
            String answerText,
            String[] selectedOptionIds,
            String matchingAnswers,
            String[] orderingAnswer
    ) {}
}
