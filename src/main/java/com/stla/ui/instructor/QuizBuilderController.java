package com.stla.ui.instructor;

import com.stla.domain.enums.QuestionDifficulty;
import com.stla.domain.enums.QuestionType;
import com.stla.domain.models.*;
import com.stla.services.QuestionService;
import com.stla.services.QuizService;
import com.stla.ui.components.AnimationUtils;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QuizBuilderController {

    @javafx.fxml.FXML private BorderPane rootPane;
    @javafx.fxml.FXML private Label quizTitle, breadcrumbLabel;
    @javafx.fxml.FXML private Button btnBack, btnSave, btnPublish, btnPreview, btnAddQuestion;
    @javafx.fxml.FXML private VBox settingsBox, questionsContainer, typeSelectorBox, questionEditorBox;
    @javafx.fxml.FXML private HBox summaryBar;

    private final QuizService quizService = new QuizService();
    private final QuestionService questionService = new QuestionService();
    private Quiz quiz;
    private String courseId;
    private Course course;
    private QuestionType selectedType = QuestionType.SINGLE_CHOICE;
    private QuizQuestion editingQuestion = null;
    private boolean isEditing = false;

    private TextField titleField, descField, timeLimitField, passingScoreField, attemptsField;
    private CheckBox shuffleCb, showAnswersCb;

    @javafx.fxml.FXML
    public void initialize() { buildSettingsForm(); }

    public void setCourse(Course course) {
        this.course = course; this.courseId = course.getId();
        if (breadcrumbLabel != null) breadcrumbLabel.setText(course.getTitle() + "  ›  Quiz Builder");
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
        new Thread(() -> {
            List<Quiz> quizzes = quizService.getQuizzes(courseId);
            Platform.runLater(() -> { if (!quizzes.isEmpty()) loadQuiz(quizzes.get(0)); });
        }).start();
    }

    @javafx.fxml.FXML
    private void handleBack() {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/stla/views/instructor/course-player.fxml"));
            javafx.scene.Parent player = loader.load();
            CoursePlayerController ctrl = loader.getController();
            if (course != null) ctrl.setCourse(course);
            rootPane.getScene().setRoot(player);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadQuiz(Quiz q) {
        this.quiz = q;
        quizTitle.setText("📝 Quiz: " + q.getTitle());
        titleField.setText(q.getTitle() != null ? q.getTitle() : "");
        descField.setText(q.getDescription() != null ? q.getDescription() : "");
        timeLimitField.setText(q.getTimeLimitMinutes() != null ? String.valueOf(q.getTimeLimitMinutes()) : "");
        passingScoreField.setText(String.valueOf(q.getPassingScore()));
        attemptsField.setText(String.valueOf(q.getAttemptsAllowed()));
        shuffleCb.setSelected(q.isShuffleQuestions());
        showAnswersCb.setSelected(q.isShowAnswersAfterSubmit());
        btnPublish.setText(q.isPublished() ? "📤 Unpublish" : "📤 Publish");
        loadQuestions();
    }

    private void loadQuestions() {
        if (quiz == null) return;
        new Thread(() -> {
            List<QuizQuestion> questions = questionService.loadAllFullQuestions(quiz.getId());
            Platform.runLater(() -> {
                buildQuestionsUI(questions);
                updateSummaryBar(questions);
            });
        }).start();
    }

    // ==================== SETTINGS ====================

    private void buildSettingsForm() {
        settingsBox.getChildren().clear();
        VBox card = new VBox(12); card.getStyleClass().add("card"); card.setPadding(new Insets(16));
        Label header = new Label("⚙ Quiz Settings");
        header.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        titleField = new TextField(); titleField.setPromptText("Quiz Title *");
        descField = new TextField(); descField.setPromptText("Description");
        timeLimitField = new TextField(); timeLimitField.setPromptText("Time Limit (min)"); timeLimitField.setPrefWidth(150);
        passingScoreField = new TextField("70"); passingScoreField.setPromptText("Passing Score (%)"); passingScoreField.setPrefWidth(150);
        attemptsField = new TextField("3"); attemptsField.setPromptText("Attempts"); attemptsField.setPrefWidth(150);
        HBox r1 = new HBox(12, fg("Title *", titleField), fg("Description", descField));
        HBox r2 = new HBox(12, fg("Time Limit", timeLimitField), fg("Passing %", passingScoreField), fg("Attempts", attemptsField));
        shuffleCb = new CheckBox("🔀 Shuffle Questions");
        showAnswersCb = new CheckBox("📊 Show Answers After Submit"); showAnswersCb.setSelected(true);
        card.getChildren().addAll(header, r1, r2, new HBox(24, shuffleCb, showAnswersCb));
        settingsBox.getChildren().add(card);
    }

    // ==================== SUMMARY BAR ====================

    private void updateSummaryBar(List<QuizQuestion> questions) {
        summaryBar.getChildren().clear();
        int totalPts = questions.stream().mapToInt(QuizQuestion::getPoints).sum();
        summaryBar.setVisible(true); summaryBar.setManaged(true);
        addSummaryItem(summaryBar, "📝 Questions", String.valueOf(questions.size()));
        addSummaryItem(summaryBar, "⭐ Total Points", String.valueOf(totalPts));
        addSummaryItem(summaryBar, "📊 Status", quiz != null && quiz.isPublished() ? "Published" : "Draft");
    }

    private void addSummaryItem(HBox bar, String label, String value) {
        VBox item = new VBox(2); item.setAlignment(Pos.CENTER);
        Label vl = new Label(value); vl.getStyleClass().add("summary-value");
        Label ll = new Label(label); ll.getStyleClass().add("summary-label");
        item.getChildren().addAll(vl, ll); bar.getChildren().add(item);
    }

    // ==================== ADD / EDIT QUESTION ====================

    @javafx.fxml.FXML
    private void handleAddQuestion() {
        if (quiz == null) { handleSaveQuiz(); if (quiz == null) return; }
        isEditing = false;
        editingQuestion = new QuizQuestion();
        editingQuestion.setQuizId(quiz.getId());
        editingQuestion.setQuestionType(selectedType.getDbValue());
        selectedType = QuestionType.SINGLE_CHOICE;
        showTypeSelector();
    }

    private void showTypeSelector() {
        typeSelectorBox.getChildren().clear();
        typeSelectorBox.setVisible(true); typeSelectorBox.setManaged(true);
        typeSelectorBox.getChildren().add(QuestionEditorHelper.buildTypeSelector(selectedType, type -> {
            selectedType = type;
            editingQuestion.setQuestionType(type.getDbValue());
            editingQuestion.setOptions(new ArrayList<>());
            editingQuestion.setAcceptedAnswers(new ArrayList<>());
            editingQuestion.setMatchPairs(new ArrayList<>());
            editingQuestion.setSequenceItems(new ArrayList<>());
            showQuestionEditor();
        }));
        questionEditorBox.setVisible(false); questionEditorBox.setManaged(false);
        AnimationUtils.fadeInUp(typeSelectorBox, 250);
    }

    private void showQuestionEditor() {
        questionEditorBox.getChildren().clear();
        questionEditorBox.setVisible(true); questionEditorBox.setManaged(true);
        typeSelectorBox.setVisible(false); typeSelectorBox.setManaged(false);

        VBox editor = QuestionEditorHelper.buildEditor(editingQuestion, selectedType);

        // Image upload button
        HBox imgRow = new HBox(12); imgRow.setAlignment(Pos.CENTER_LEFT);
        Button imgBtn = new Button("🖼 Upload Image");
        imgBtn.getStyleClass().addAll("btn-outline", "btn-sm");
        imgBtn.setOnAction(e -> uploadQuestionImage());
        imgRow.getChildren().add(imgBtn);
        if (editingQuestion.getQuestionImageUrl() != null && !editingQuestion.getQuestionImageUrl().isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(editingQuestion.getQuestionImageUrl(), 120, 80, true, true));
                imgRow.getChildren().add(iv);
                Button rmImg = new Button("✕ Remove");
                rmImg.getStyleClass().addAll("btn-outline", "btn-sm");
                rmImg.setOnAction(e -> { editingQuestion.setQuestionImageUrl(null); showQuestionEditor(); });
                imgRow.getChildren().add(rmImg);
            } catch (Exception ignored) {}
        }

        // Action buttons
        HBox actions = new HBox(12); actions.setAlignment(Pos.CENTER_RIGHT); actions.setPadding(new Insets(8,0,0,0));
        Button cancelBtn = new Button("Cancel"); cancelBtn.getStyleClass().addAll("btn-outline","btn-sm");
        cancelBtn.setOnAction(e -> hideEditor());
        Button changeType = new Button("← Change Type"); changeType.getStyleClass().addAll("btn-outline","btn-sm");
        changeType.setOnAction(e -> showTypeSelector());
        Button saveBtn = new Button(isEditing ? "💾 Update Question" : "💾 Save Question");
        saveBtn.getStyleClass().addAll("btn-primary","btn-sm");
        saveBtn.setOnAction(e -> saveQuestion());
        actions.getChildren().addAll(cancelBtn, changeType, saveBtn);

        questionEditorBox.getChildren().addAll(imgRow, editor, actions);
        AnimationUtils.fadeInUp(questionEditorBox, 250);
    }

    private void hideEditor() {
        typeSelectorBox.setVisible(false); typeSelectorBox.setManaged(false);
        questionEditorBox.setVisible(false); questionEditorBox.setManaged(false);
        editingQuestion = null;
    }

    private void saveQuestion() {
        List<String> errors = questionService.validateQuestion(editingQuestion);
        if (!errors.isEmpty()) {
            showToast("⚠ " + errors.get(0), ToastNotification.Type.WARNING);
            return;
        }
        new Thread(() -> {
            if (isEditing) questionService.updateFullQuestion(editingQuestion);
            else questionService.saveFullQuestion(editingQuestion);
            Platform.runLater(() -> {
                hideEditor();
                loadQuestions();
                showToast(isEditing ? "Question updated!" : "Question added!", ToastNotification.Type.SUCCESS);
            });
        }).start();
    }

    private void startEditQuestion(QuizQuestion q) {
        isEditing = true;
        editingQuestion = q;
        selectedType = QuestionType.fromDbValue(q.getQuestionType());
        showQuestionEditor();
    }

    // ==================== QUESTIONS UI ====================

    private void buildQuestionsUI(List<QuizQuestion> questions) {
        questionsContainer.getChildren().clear();
        if (questions == null || questions.isEmpty()) {
            questionsContainer.getChildren().add(new Label("No questions yet. Click '➕ Add Question' to start."));
            return;
        }
        for (int i = 0; i < questions.size(); i++) {
            questionsContainer.getChildren().add(buildQuestionCard(questions.get(i), i + 1, questions));
        }
        AnimationUtils.staggerFadeIn(questionsContainer, 60);
    }

    private VBox buildQuestionCard(QuizQuestion q, int num, List<QuizQuestion> all) {
        VBox card = new VBox(8); card.getStyleClass().add("question-list-card");
        QuestionType type = QuestionType.fromDbValue(q.getQuestionType());
        QuestionDifficulty diff = QuestionDifficulty.fromDbValue(q.getDifficulty());

        // Header
        HBox hdr = new HBox(8); hdr.setAlignment(Pos.CENTER_LEFT);
        Label numLbl = new Label("Q" + num); numLbl.getStyleClass().add("question-number-badge");
        Label typeLbl = new Label(type.getIcon() + " " + type.getDisplayLabel()); typeLbl.getStyleClass().add("type-badge");
        Label ptsLbl = new Label(q.getPoints() + " pts"); ptsLbl.getStyleClass().add("points-badge");
        Label diffLbl = new Label(diff.getDisplayLabel());
        diffLbl.getStyleClass().add("difficulty-badge-" + diff.getDbValue());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button editB = new Button("✏"); editB.getStyleClass().add("question-action-btn");
        editB.setOnAction(e -> startEditQuestion(q));
        Button dupB = new Button("⎘"); dupB.getStyleClass().add("question-action-btn");
        dupB.setOnAction(e -> duplicateQ(q));
        Button upB = new Button("▲"); upB.getStyleClass().add("question-action-btn");
        upB.setOnAction(e -> moveQ(q, all, -1));
        Button dnB = new Button("▼"); dnB.getStyleClass().add("question-action-btn");
        dnB.setOnAction(e -> moveQ(q, all, 1));
        Button delB = new Button("🗑"); delB.getStyleClass().addAll("question-action-btn","question-action-btn-danger");
        delB.setOnAction(e -> deleteQ(q));
        hdr.getChildren().addAll(numLbl, typeLbl, ptsLbl, diffLbl, sp, editB, dupB, upB, dnB, delB);

        // Question text
        Label qText = new Label(q.getQuestionText());
        qText.setWrapText(true);
        qText.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1F2937;");

        card.getChildren().addAll(hdr, qText);

        // Image preview
        if (q.getQuestionImageUrl() != null && !q.getQuestionImageUrl().isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(q.getQuestionImageUrl(), 200, 100, true, true));
                HBox imgBox = new HBox(iv); imgBox.getStyleClass().add("image-preview-card");
                card.getChildren().add(imgBox);
            } catch (Exception ignored) {}
        }

        // Options preview
        if (type.usesOptions() && q.getOptions() != null) {
            for (QuizOption opt : q.getOptions()) {
                Label optL = new Label((opt.isCorrect() ? "✅ " : "⬜ ") + opt.getOptionText());
                optL.setStyle(opt.isCorrect() ? "-fx-text-fill:#059669;-fx-font-weight:bold;-fx-padding:2 0 2 16;" : "-fx-text-fill:#6B7280;-fx-padding:2 0 2 16;");
                card.getChildren().add(optL);
            }
        }
        if (type.usesAcceptedAnswers() && q.getAcceptedAnswers() != null && !q.getAcceptedAnswers().isEmpty()) {
            Label al = new Label("Accepted: " + String.join(", ", q.getAcceptedAnswers().stream().map(QuestionAcceptedAnswer::getAnswerText).toList()));
            al.setStyle("-fx-text-fill:#059669;-fx-font-size:12px;-fx-padding:2 0 2 16;"); al.setWrapText(true);
            card.getChildren().add(al);
        }
        if (type.usesMatchPairs() && q.getMatchPairs() != null) {
            for (QuestionMatchPair p : q.getMatchPairs()) {
                Label pl = new Label("  " + p.getLeftItem() + " ⇔ " + p.getRightItem());
                pl.setStyle("-fx-text-fill:#4338CA;-fx-font-size:12px;");
                card.getChildren().add(pl);
            }
        }
        if (type.usesSequenceItems() && q.getSequenceItems() != null) {
            for (int i = 0; i < q.getSequenceItems().size(); i++) {
                Label sl = new Label("  " + (i+1) + ". " + q.getSequenceItems().get(i).getItemText());
                sl.setStyle("-fx-text-fill:#7C3AED;-fx-font-size:12px;");
                card.getChildren().add(sl);
            }
        }
        return card;
    }

    // ==================== QUESTION ACTIONS ====================

    private void deleteQ(QuizQuestion q) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Delete this question?", ButtonType.YES, ButtonType.NO);
        c.showAndWait().ifPresent(b -> { if (b == ButtonType.YES) {
            new Thread(() -> { questionService.deleteQuestion(q.getId()); Platform.runLater(this::loadQuestions); }).start();
        }});
    }

    private void duplicateQ(QuizQuestion q) {
        new Thread(() -> { questionService.duplicateQuestion(q); Platform.runLater(this::loadQuestions); }).start();
        showToast("Question duplicated!", ToastNotification.Type.SUCCESS);
    }

    private void moveQ(QuizQuestion q, List<QuizQuestion> all, int dir) {
        int idx = -1;
        for (int i = 0; i < all.size(); i++) if (all.get(i).getId().equals(q.getId())) { idx = i; break; }
        if (idx < 0) return;
        int ni = idx + dir;
        if (ni < 0 || ni >= all.size()) return;
        java.util.Collections.swap(all, idx, ni);
        List<String> ids = all.stream().map(QuizQuestion::getId).toList();
        new Thread(() -> { questionService.reorderQuestions(ids); Platform.runLater(this::loadQuestions); }).start();
    }

    // ==================== IMAGE UPLOAD ====================

    private void uploadQuestionImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Question Image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.webp"));
        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file == null) return;
        if (editingQuestion.getId() != null) {
            new Thread(() -> {
                String url = questionService.uploadQuestionImage(file, editingQuestion.getId());
                Platform.runLater(() -> {
                    if (url != null) { editingQuestion.setQuestionImageUrl(url); showQuestionEditor(); showToast("Image uploaded!", ToastNotification.Type.SUCCESS); }
                    else showToast("Upload failed", ToastNotification.Type.ERROR);
                });
            }).start();
        } else {
            editingQuestion.setQuestionImageUrl(file.toURI().toString());
            showQuestionEditor();
        }
    }

    // ==================== PREVIEW ====================

    @javafx.fxml.FXML
    private void handlePreviewQuiz() {
        if (quiz == null) return;
        new Thread(() -> {
            List<QuizQuestion> qs = questionService.loadAllFullQuestions(quiz.getId());
            Platform.runLater(() -> showPreviewDialog(qs));
        }).start();
    }

    private void showPreviewDialog(List<QuizQuestion> questions) {
        Dialog<Void> dlg = new Dialog<>(); dlg.setTitle("Quiz Preview — " + quiz.getTitle());
        dlg.setHeaderText(null);
        ScrollPane sp = new ScrollPane(); sp.setFitToWidth(true); sp.setPrefSize(700, 500);
        VBox content = new VBox(16); content.setPadding(new Insets(20));
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            QuestionType type = QuestionType.fromDbValue(q.getQuestionType());
            VBox qCard = new VBox(8); qCard.getStyleClass().add("card"); qCard.setPadding(new Insets(16));
            Label num = new Label("Q" + (i+1) + " — " + type.getDisplayLabel() + " (" + q.getPoints() + " pts)");
            num.setStyle("-fx-font-size:12px;-fx-text-fill:#7C3AED;-fx-font-weight:bold;");
            Label txt = new Label(q.getQuestionText()); txt.setWrapText(true);
            txt.setStyle("-fx-font-size:15px;-fx-font-weight:bold;");
            qCard.getChildren().addAll(num, txt);
            if (type.usesOptions()) {
                for (QuizOption o : q.getOptions()) {
                    Label ol = new Label("  ○ " + o.getOptionText());
                    ol.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-padding:2 0 2 12;");
                    qCard.getChildren().add(ol);
                }
            } else if (type == QuestionType.SHORT_ANSWER || type == QuestionType.ESSAY) {
                TextField tf = new TextField(); tf.setPromptText("Student answer..."); tf.setDisable(true);
                qCard.getChildren().add(tf);
            } else if (type == QuestionType.FILL_BLANK && q.getBlankTemplate() != null) {
                Label tl = new Label(q.getBlankTemplate()); tl.setWrapText(true);
                tl.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;");
                qCard.getChildren().add(tl);
            }
            content.getChildren().add(qCard);
        }
        sp.setContent(content);
        dlg.getDialogPane().setContent(sp);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    // ==================== SAVE / PUBLISH ====================

    @javafx.fxml.FXML
    private void handleSaveQuiz() {
        if (titleField.getText().isBlank()) {
            showToast("Quiz title is required", ToastNotification.Type.WARNING); return;
        }
        if (quiz == null) { quiz = new Quiz(); quiz.setCourseId(courseId); }
        quiz.setTitle(titleField.getText().trim());
        quiz.setDescription(descField.getText().trim());
        try { quiz.setTimeLimitMinutes(Integer.parseInt(timeLimitField.getText().trim())); } catch (Exception e) { quiz.setTimeLimitMinutes(null); }
        try { quiz.setPassingScore(Integer.parseInt(passingScoreField.getText().trim())); } catch (Exception e) { quiz.setPassingScore(70); }
        try { quiz.setAttemptsAllowed(Integer.parseInt(attemptsField.getText().trim())); } catch (Exception e) { quiz.setAttemptsAllowed(3); }
        quiz.setShuffleQuestions(shuffleCb.isSelected());
        quiz.setShowAnswersAfterSubmit(showAnswersCb.isSelected());
        new Thread(() -> {
            if (quiz.getId() == null) { String id = quizService.createQuiz(quiz); quiz.setId(id); }
            else quizService.updateQuiz(quiz);
            Platform.runLater(() -> {
                quizTitle.setText("📝 Quiz: " + quiz.getTitle());
                showToast("Quiz saved!", ToastNotification.Type.SUCCESS);
            });
        }).start();
    }

    @javafx.fxml.FXML
    private void handleTogglePublish() {
        if (quiz == null || quiz.getId() == null) return;
        boolean ns = !quiz.isPublished();
        new Thread(() -> {
            quizService.togglePublish(quiz.getId(), ns); quiz.setPublished(ns);
            Platform.runLater(() -> {
                btnPublish.setText(ns ? "📤 Unpublish" : "📤 Publish");
                showToast(ns ? "Quiz published!" : "Quiz unpublished.", ToastNotification.Type.SUCCESS);
                loadQuestions();
            });
        }).start();
    }

    // ==================== HELPERS ====================

    private VBox fg(String label, TextField field) {
        VBox g = new VBox(4);
        Label l = new Label(label); l.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        field.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(g, Priority.ALWAYS);
        g.getChildren().addAll(l, field); return g;
    }

    private void showToast(String msg, ToastNotification.Type type) {
        try { ToastNotification.show((Stage) rootPane.getScene().getWindow(), msg, type); } catch (Exception ignored) {}
    }
}
