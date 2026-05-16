package com.stla.ui.student;

import com.stla.domain.enums.QuestionType;
import com.stla.domain.models.*;
import com.stla.services.QuizService;
import com.stla.ui.components.AnimationUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.util.*;

/**
 * Student quiz-taking controller supporting all 9 question types.
 */
public class QuizTakeController {

    @javafx.fxml.FXML private Label quizTitleLabel, quizDescLabel, timerLabel;
    @javafx.fxml.FXML private HBox questionNav;
    @javafx.fxml.FXML private VBox questionContent;
    @javafx.fxml.FXML private Button btnPrev, btnNextQ, btnSubmitQuiz;

    private final QuizService quizService = new QuizService();
    private Quiz quiz;
    private String studentId;
    private List<QuizQuestion> questions;
    private int currentQuestionIndex = 0;
    private final Map<String, List<String>> selectedAnswers = new HashMap<>();
    private final Map<String, String> textAnswers = new HashMap<>();
    private final Map<String, String> matchingAnswers = new HashMap<>();
    private final Map<String, List<String>> orderingAnswers = new HashMap<>();
    private Timeline timer;
    private int remainingSeconds;
    private ResultCallback resultCallback;

    public interface ResultCallback {
        void onSubmitted(Quiz quiz, QuizAttempt attempt, List<QuizQuestion> questions,
                         Map<String, QuizResultController.QuizResponseSnapshot> snapshots);
    }

    @javafx.fxml.FXML public void initialize() {}

    public void setResultCallback(ResultCallback callback) { this.resultCallback = callback; }

    public void setQuiz(Quiz quiz, String studentId) {
        this.quiz = quiz; this.studentId = studentId;
        quizTitleLabel.setText("📝 " + quiz.getTitle());
        quizDescLabel.setText(quiz.getDescription() != null ? quiz.getDescription() : "");
        loadQuestions();
    }

    private void loadQuestions() {
        new Thread(() -> {
            questions = quizService.getQuestionsWithOptions(quiz.getId());
            if (quiz.isShuffleQuestions()) Collections.shuffle(questions);
            Platform.runLater(() -> { buildNavigation(); if (!questions.isEmpty()) showQuestion(0); startTimer(); });
        }).start();
    }

    // ==================== TIMER ====================
    private void startTimer() {
        if (quiz.getTimeLimitMinutes() == null || quiz.getTimeLimitMinutes() <= 0) { timerLabel.setText("⏱ No Limit"); return; }
        remainingSeconds = quiz.getTimeLimitMinutes() * 60;
        updateTimerLabel();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerLabel();
            if (remainingSeconds <= 0) { timer.stop(); autoSubmit(); }
            if (remainingSeconds <= 60) timerLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#EF4444;");
        }));
        timer.setCycleCount(Timeline.INDEFINITE); timer.play();
    }

    private void updateTimerLabel() { timerLabel.setText(String.format("⏱ %02d:%02d", remainingSeconds/60, remainingSeconds%60)); }
    private void autoSubmit() { Platform.runLater(() -> { new Alert(Alert.AlertType.WARNING, "⏰ Time's up!").showAndWait(); submitQuiz(); }); }

    // ==================== NAVIGATION ====================
    private void buildNavigation() {
        questionNav.getChildren().clear();
        for (int i = 0; i < questions.size(); i++) {
            int idx = i;
            Button btn = new Button(String.valueOf(i + 1));
            btn.setPrefSize(36, 36);
            btn.setStyle(getNavStyle(i, false));
            btn.setOnAction(e -> showQuestion(idx));
            questionNav.getChildren().add(btn);
        }
    }

    private void updateNavigation() {
        for (int i = 0; i < questionNav.getChildren().size(); i++) {
            Button btn = (Button) questionNav.getChildren().get(i);
            boolean answered = isAnswered(questions.get(i));
            btn.setStyle(getNavStyle(i, answered));
        }
    }

    private boolean isAnswered(QuizQuestion q) {
        String id = q.getId();
        if (selectedAnswers.containsKey(id) && !selectedAnswers.get(id).isEmpty()) return true;
        if (textAnswers.containsKey(id) && !textAnswers.get(id).isBlank()) return true;
        if (matchingAnswers.containsKey(id) && !matchingAnswers.get(id).isBlank()) return true;
        if (orderingAnswers.containsKey(id) && !orderingAnswers.get(id).isEmpty()) return true;
        return false;
    }

    private String getNavStyle(int index, boolean answered) {
        if (index == currentQuestionIndex) return "-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;";
        if (answered) return "-fx-background-color:#10B981;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;";
        return "-fx-background-color:#F3F4F6;-fx-text-fill:#374151;-fx-background-radius:8;-fx-cursor:hand;";
    }

    private void showQuestion(int index) {
        currentQuestionIndex = index;
        QuizQuestion q = questions.get(index);
        QuestionType type = QuestionType.fromDbValue(q.getQuestionType());
        questionContent.getChildren().clear();

        VBox card = new VBox(12); card.getStyleClass().add("card"); card.setPadding(new Insets(24));
        card.setStyle("-fx-border-color:#E5E7EB;-fx-border-radius:16;-fx-background-radius:16;");

        HBox qHeader = new HBox(12); qHeader.setAlignment(Pos.CENTER_LEFT);
        qHeader.getChildren().addAll(
            lbl("Question " + (index+1) + " of " + questions.size(), "-fx-font-size:13px;-fx-text-fill:#6B7280;"),
            lbl(q.getPoints() + " pts", "-fx-background-color:#EDE9FE;-fx-text-fill:#7C3AED;-fx-padding:2 8;-fx-background-radius:8;-fx-font-size:11px;-fx-font-weight:bold;"),
            lbl(type.getIcon() + " " + type.getDisplayLabel(), "-fx-background-color:#E0E7FF;-fx-text-fill:#4338CA;-fx-padding:2 8;-fx-background-radius:8;-fx-font-size:11px;")
        );
        card.getChildren().add(qHeader);

        // Image
        if (q.getQuestionImageUrl() != null && !q.getQuestionImageUrl().isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(q.getQuestionImageUrl(), 400, 200, true, true));
                HBox imgBox = new HBox(iv); imgBox.setAlignment(Pos.CENTER);
                imgBox.setStyle("-fx-background-color:#F3F4F6;-fx-background-radius:12;-fx-padding:12;");
                card.getChildren().add(imgBox);
            } catch (Exception ignored) {}
        }

        Label qText = new Label(q.getQuestionText());
        qText.setWrapText(true);
        qText.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        card.getChildren().add(qText);
        if (q.getDescription() != null && !q.getDescription().isBlank()) {
            Label desc = new Label(q.getDescription()); desc.setWrapText(true);
            desc.setStyle("-fx-font-size:13px;-fx-text-fill:#6B7280;"); card.getChildren().add(desc);
        }

        // Type-specific UI
        switch (type) {
            case SINGLE_CHOICE, IMAGE_QUESTION -> card.getChildren().add(buildChoiceUI(q, false));
            case MULTIPLE_CHOICE -> card.getChildren().add(buildChoiceUI(q, true));
            case TRUE_FALSE -> card.getChildren().add(buildChoiceUI(q, false));
            case SHORT_ANSWER -> card.getChildren().add(buildShortAnswerUI(q));
            case ESSAY -> card.getChildren().add(buildEssayUI(q));
            case FILL_BLANK -> card.getChildren().add(buildFillBlankUI(q));
            case MATCHING -> card.getChildren().add(buildMatchingUI(q));
            case ORDERING -> card.getChildren().add(buildOrderingUI(q));
        }

        questionContent.getChildren().add(card);
        AnimationUtils.fadeIn(card, 200);
        btnPrev.setVisible(index > 0);
        btnNextQ.setVisible(index < questions.size() - 1);
        btnSubmitQuiz.setVisible(index == questions.size() - 1);
        updateNavigation();
    }

    // ==================== CHOICE UI ====================
    private VBox buildChoiceUI(QuizQuestion q, boolean multi) {
        VBox box = new VBox(8); box.setPadding(new Insets(8, 0, 0, 0));
        List<String> current = selectedAnswers.getOrDefault(q.getId(), new ArrayList<>());
        ToggleGroup tg = multi ? null : new ToggleGroup();
        for (QuizOption opt : q.getOptions()) {
            HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(12, 16, 12, 16));
            boolean isSel = current.contains(opt.getId());
            row.setStyle(isSel ? "-fx-background-color:#EDE9FE;-fx-border-color:#7C3AED;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;"
                : "-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
            if (multi) {
                CheckBox cb = new CheckBox(opt.getOptionText()); cb.setSelected(isSel); cb.setStyle("-fx-font-size:14px;");
                cb.setOnAction(e -> {
                    List<String> sel = selectedAnswers.computeIfAbsent(q.getId(), k -> new ArrayList<>());
                    if (cb.isSelected()) { if (!sel.contains(opt.getId())) sel.add(opt.getId()); } else sel.remove(opt.getId());
                    updateNavigation(); showQuestion(currentQuestionIndex);
                });
                row.getChildren().add(cb);
            } else {
                RadioButton rb = new RadioButton(opt.getOptionText()); rb.setToggleGroup(tg); rb.setSelected(isSel); rb.setStyle("-fx-font-size:14px;");
                rb.setOnAction(e -> {
                    selectedAnswers.put(q.getId(), new ArrayList<>(List.of(opt.getId())));
                    updateNavigation(); showQuestion(currentQuestionIndex);
                });
                row.getChildren().add(rb);
            }
            box.getChildren().add(row);
        }
        return box;
    }

    // ==================== SHORT ANSWER UI ====================
    private VBox buildShortAnswerUI(QuizQuestion q) {
        VBox box = new VBox(8);
        TextField tf = new TextField(textAnswers.getOrDefault(q.getId(), ""));
        tf.setPromptText("Type your answer here...");
        tf.setStyle("-fx-font-size:14px;-fx-padding:12;");
        tf.textProperty().addListener((o,ov,nv) -> { textAnswers.put(q.getId(), nv); updateNavigation(); });
        box.getChildren().add(tf);
        return box;
    }

    // ==================== ESSAY UI ====================
    private VBox buildEssayUI(QuizQuestion q) {
        VBox box = new VBox(8);
        TextArea ta = new TextArea(textAnswers.getOrDefault(q.getId(), ""));
        ta.setPromptText("Write your essay here...");
        ta.setPrefRowCount(6); ta.setWrapText(true);
        ta.textProperty().addListener((o,ov,nv) -> { textAnswers.put(q.getId(), nv); updateNavigation(); });
        Label wc = new Label("Words: 0");
        wc.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
        ta.textProperty().addListener((o,ov,nv) -> {
            int words = nv.isBlank() ? 0 : nv.trim().split("\\s+").length;
            String max = q.getMaxWordCount() != null ? " / " + q.getMaxWordCount() : "";
            wc.setText("Words: " + words + max);
        });
        box.getChildren().addAll(ta, wc);
        return box;
    }

    // ==================== FILL BLANK UI ====================
    private VBox buildFillBlankUI(QuizQuestion q) {
        VBox box = new VBox(8);
        if (q.getBlankTemplate() != null) {
            Label tmpl = new Label(q.getBlankTemplate()); tmpl.setWrapText(true);
            tmpl.setStyle("-fx-font-size:14px;-fx-text-fill:#374151;-fx-padding:8;-fx-background-color:#F9FAFB;-fx-background-radius:8;");
            box.getChildren().add(tmpl);
        }
        TextField tf = new TextField(textAnswers.getOrDefault(q.getId(), ""));
        tf.setPromptText("Fill in the blank...");
        tf.textProperty().addListener((o,ov,nv) -> { textAnswers.put(q.getId(), nv); updateNavigation(); });
        box.getChildren().add(tf);
        return box;
    }

    // ==================== MATCHING UI ====================
    private VBox buildMatchingUI(QuizQuestion q) {
        VBox box = new VBox(8);
        Label hint = new Label("Match each item on the left with the correct item on the right");
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");
        box.getChildren().add(hint);
        List<String> rightItems = new ArrayList<>(q.getMatchPairs().stream().map(QuestionMatchPair::getRightItem).toList());
        Collections.shuffle(rightItems);
        Map<String, String> currentMatch = new HashMap<>();
        // Parse existing matching answers
        String existing = matchingAnswers.getOrDefault(q.getId(), "");
        for (QuestionMatchPair pair : q.getMatchPairs()) {
            HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-background-radius:10;");
            Label left = new Label(pair.getLeftItem()); left.setStyle("-fx-font-size:14px;-fx-font-weight:bold;"); left.setPrefWidth(200);
            Label arrow = new Label("→"); arrow.setStyle("-fx-font-size:16px;-fx-text-fill:#7C3AED;");
            ComboBox<String> cb = new ComboBox<>(); cb.getItems().addAll(rightItems); cb.setPromptText("Select match...");
            cb.setPrefWidth(200);
            cb.setOnAction(e -> {
                currentMatch.put(pair.getLeftItem(), cb.getValue());
                StringBuilder json = new StringBuilder("{");
                currentMatch.forEach((k,v) -> json.append("\"").append(k).append("\":\"").append(v).append("\","));
                if (json.length() > 1) json.setLength(json.length()-1);
                json.append("}");
                matchingAnswers.put(q.getId(), json.toString());
                updateNavigation();
            });
            row.getChildren().addAll(left, arrow, cb);
            box.getChildren().add(row);
        }
        return box;
    }

    // ==================== ORDERING UI ====================
    private VBox buildOrderingUI(QuizQuestion q) {
        VBox box = new VBox(8);
        Label hint = new Label("Arrange items in the correct order using ▲▼ buttons");
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");
        box.getChildren().add(hint);
        // Initialize ordering if not set
        List<String> currentOrder = orderingAnswers.computeIfAbsent(q.getId(), k -> {
            List<String> ids = new ArrayList<>(q.getSequenceItems().stream().map(QuestionSequenceItem::getId).toList());
            Collections.shuffle(ids);
            return ids;
        });
        VBox itemList = new VBox(6);
        Runnable rebuild = () -> rebuildOrderUI(itemList, q, currentOrder);
        rebuild.run();
        box.getChildren().add(itemList);
        return box;
    }

    private void rebuildOrderUI(VBox list, QuizQuestion q, List<String> order) {
        list.getChildren().clear();
        for (int i = 0; i < order.size(); i++) {
            String itemId = order.get(i);
            QuestionSequenceItem item = q.getSequenceItems().stream().filter(s -> s.getId().equals(itemId)).findFirst().orElse(null);
            if (item == null) continue;
            int idx = i;
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
            Label num = new Label(String.valueOf(i+1)); num.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:4 10;-fx-background-radius:8;-fx-font-size:12px;");
            Label text = new Label(item.getItemText()); text.setStyle("-fx-font-size:14px;"); HBox.setHgrow(text, Priority.ALWAYS);
            Button up = new Button("▲"); up.setStyle("-fx-background-color:#F3F4F6;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:4 8;");
            up.setOnAction(e -> { if(idx>0){ Collections.swap(order,idx,idx-1); rebuildOrderUI(list,q,order); updateNavigation(); }});
            Button dn = new Button("▼"); dn.setStyle("-fx-background-color:#F3F4F6;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:4 8;");
            dn.setOnAction(e -> { if(idx<order.size()-1){ Collections.swap(order,idx,idx+1); rebuildOrderUI(list,q,order); updateNavigation(); }});
            row.getChildren().addAll(num, text, up, dn);
            list.getChildren().add(row);
        }
    }

    // ==================== NAVIGATION ACTIONS ====================
    @javafx.fxml.FXML private void handlePrev() { if (currentQuestionIndex > 0) showQuestion(currentQuestionIndex - 1); }
    @javafx.fxml.FXML private void handleNextQ() { if (currentQuestionIndex < questions.size() - 1) showQuestion(currentQuestionIndex + 1); }

    @javafx.fxml.FXML
    private void handleSubmitQuiz() {
        long answered = questions.stream().filter(this::isAnswered).count();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "You answered " + answered + " of " + questions.size() + " questions.\nSubmit quiz?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Submit Quiz");
        confirm.showAndWait().ifPresent(btn -> { if (btn == ButtonType.YES) submitQuiz(); });
    }

    private void submitQuiz() {
        if (timer != null) timer.stop();
        btnSubmitQuiz.setDisable(true);
        var validation = quizService.validateCanStart(studentId, quiz.getId());
        if (validation.isPresent()) {
            new Alert(Alert.AlertType.WARNING, validation.get()).showAndWait();
            btnSubmitQuiz.setDisable(false);
            return;
        }
        List<QuizResponse> responses = new ArrayList<>();
        for (QuizQuestion q : questions) {
            QuizResponse r = new QuizResponse();
            r.setQuestionId(q.getId());
            List<String> sel = selectedAnswers.getOrDefault(q.getId(), new ArrayList<>());
            r.setSelectedOptionIds(sel.toArray(new String[0]));
            r.setAnswerText(textAnswers.getOrDefault(q.getId(), null));
            r.setMatchingAnswers(matchingAnswers.getOrDefault(q.getId(), null));
            List<String> ord = orderingAnswers.get(q.getId());
            if (ord != null) r.setOrderingAnswer(ord.toArray(new String[0]));
            responses.add(r);
        }
        new Thread(() -> {
            QuizAttempt attempt = quizService.submitAttempt(studentId, quiz.getId(), responses);
            Platform.runLater(() -> showResults(attempt));
        }).start();
    }

    // ==================== RESULTS ====================
    private void showResults(QuizAttempt attempt) {
        Map<String, QuizResultController.QuizResponseSnapshot> snapshots = new HashMap<>();
        for (QuizQuestion q : questions) {
            QuestionType type = QuestionType.fromDbValue(q.getQuestionType());
            List<String> sel = selectedAnswers.getOrDefault(q.getId(), List.of());
            List<String> ord = orderingAnswers.get(q.getId());
            snapshots.put(q.getId(), new QuizResultController.QuizResponseSnapshot(
                    checkCorrect(q, type),
                    textAnswers.getOrDefault(q.getId(), ""),
                    sel.toArray(new String[0]),
                    matchingAnswers.getOrDefault(q.getId(), null),
                    ord != null ? ord.toArray(new String[0]) : null));
        }
        if (resultCallback != null) {
            resultCallback.onSubmitted(quiz, attempt, questions, snapshots);
            return;
        }
        questionNav.getChildren().clear();
        questionContent.getChildren().clear();
        btnPrev.setVisible(false);
        btnNextQ.setVisible(false);
        btnSubmitQuiz.setVisible(false);
        timerLabel.setText("");
        quizTitleLabel.setText("📊 Quiz Results");
        double pct = attempt.getScorePercent();
        VBox resultCard = new VBox(16);
        resultCard.setAlignment(Pos.CENTER);
        resultCard.getChildren().add(lbl(String.format("%.0f%%", pct),
                "-fx-font-size:48px;-fx-font-weight:bold;-fx-text-fill:" + (attempt.isPassed() ? "#10B981" : "#EF4444") + ";"));
        questionContent.getChildren().add(resultCard);
    }

    private boolean checkCorrect(QuizQuestion q, QuestionType type) {
        if (type.usesOptions()) {
            List<String> userSel = selectedAnswers.getOrDefault(q.getId(), List.of());
            List<String> correctIds = q.getOptions().stream().filter(QuizOption::isCorrect).map(QuizOption::getId).toList();
            return new HashSet<>(userSel).equals(new HashSet<>(correctIds));
        } else if (type == QuestionType.SHORT_ANSWER || type == QuestionType.FILL_BLANK) {
            String ans = textAnswers.getOrDefault(q.getId(), "");
            return q.getAcceptedAnswers().stream().anyMatch(a -> a.getAnswerText().equalsIgnoreCase(ans.trim()));
        }
        return false;
    }

    private Label lbl(String text, String style) { Label l = new Label(text); l.setStyle(style); l.setWrapText(true); return l; }
}
