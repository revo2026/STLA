package com.stla.ui.instructor;

import com.stla.domain.enums.QuestionDifficulty;
import com.stla.domain.enums.QuestionType;
import com.stla.domain.models.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds dynamic question editor UI for each question type.
 */
public class QuestionEditorHelper {

    public static VBox buildTypeSelector(QuestionType selected, Consumer<QuestionType> onSelect) {
        VBox box = new VBox(8);
        Label hdr = new Label("Select Question Type");
        hdr.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        FlowPane grid = new FlowPane(10, 10);
        for (QuestionType t : QuestionType.values()) {
            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("question-type-card");
            if (t == selected) card.getStyleClass().add("question-type-card-active");
            Label icon = new Label(t.getIcon());
            icon.getStyleClass().add("question-type-icon");
            Label lbl = new Label(t.getDisplayLabel());
            lbl.getStyleClass().add("question-type-label");
            card.getChildren().addAll(icon, lbl);
            card.setOnMouseClicked(e -> onSelect.accept(t));
            grid.getChildren().add(card);
        }
        box.getChildren().addAll(hdr, grid);
        return box;
    }

    public static VBox buildEditor(QuizQuestion q, QuestionType type) {
        VBox editor = new VBox(14);
        editor.getStyleClass().add("question-editor-card");
        // Common fields
        editor.getChildren().addAll(buildCommonFields(q, type));
        // Type-specific
        switch (type) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> editor.getChildren().add(buildChoiceEditor(q, type == QuestionType.MULTIPLE_CHOICE));
            case TRUE_FALSE -> editor.getChildren().add(buildTrueFalseEditor(q));
            case SHORT_ANSWER -> editor.getChildren().add(buildShortAnswerEditor(q));
            case ESSAY -> editor.getChildren().add(buildEssayEditor(q));
            case FILL_BLANK -> editor.getChildren().add(buildFillBlankEditor(q));
            case MATCHING -> editor.getChildren().add(buildMatchingEditor(q));
            case ORDERING -> editor.getChildren().add(buildOrderingEditor(q));
            case IMAGE_QUESTION -> editor.getChildren().add(buildChoiceEditor(q, false));
        }
        // Explanation
        Label expLbl = new Label("Explanation (shown after submit)");
        expLbl.getStyleClass().add("form-label");
        TextArea expF = new TextArea(q.getExplanation() != null ? q.getExplanation() : "");
        expF.setPromptText("Explain the correct answer...");
        expF.setPrefRowCount(2);
        expF.textProperty().addListener((o,ov,nv) -> q.setExplanation(nv));
        editor.getChildren().addAll(expLbl, expF);
        return editor;
    }

    private static List<javafx.scene.Node> buildCommonFields(QuizQuestion q, QuestionType type) {
        List<javafx.scene.Node> nodes = new ArrayList<>();
        Label typeBadge = new Label(type.getIcon() + " " + type.getDisplayLabel());
        typeBadge.getStyleClass().add("type-badge");
        nodes.add(typeBadge);
        // Title
        Label tLbl = new Label("Question Text *");
        tLbl.getStyleClass().add("form-label");
        TextArea tF = new TextArea(q.getQuestionText() != null ? q.getQuestionText() : "");
        tF.setPromptText("Enter your question...");
        tF.setPrefRowCount(2);
        tF.textProperty().addListener((o,ov,nv) -> q.setQuestionText(nv));
        nodes.add(tLbl); nodes.add(tF);
        // Description
        TextField dF = new TextField(q.getDescription() != null ? q.getDescription() : "");
        dF.setPromptText("Optional description or hint");
        dF.textProperty().addListener((o,ov,nv) -> q.setDescription(nv));
        nodes.add(dF);
        // Row: Points + Difficulty + Required
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        Spinner<Integer> ptsSpin = new Spinner<>(1, 100, q.getPoints());
        ptsSpin.setPrefWidth(90);
        ptsSpin.setEditable(true);
        ptsSpin.valueProperty().addListener((o,ov,nv) -> q.setPoints(nv));
        ComboBox<String> diffCb = new ComboBox<>();
        for (QuestionDifficulty d : QuestionDifficulty.values()) diffCb.getItems().add(d.getDisplayLabel());
        diffCb.setValue(QuestionDifficulty.fromDbValue(q.getDifficulty()).getDisplayLabel());
        diffCb.setOnAction(e -> {
            for (QuestionDifficulty d : QuestionDifficulty.values())
                if (d.getDisplayLabel().equals(diffCb.getValue())) q.setDifficulty(d.getDbValue());
        });
        CheckBox reqCb = new CheckBox("Required");
        reqCb.setSelected(q.isRequired());
        reqCb.selectedProperty().addListener((o,ov,nv) -> q.setRequired(nv));
        row.getChildren().addAll(new Label("Points:"), ptsSpin, new Label("Difficulty:"), diffCb, reqCb);
        nodes.add(row);
        return nodes;
    }

    private static VBox buildChoiceEditor(QuizQuestion q, boolean multi) {
        VBox box = new VBox(8);
        Label hdr = new Label(multi ? "☑ Options (select multiple correct)" : "○ Options (select one correct)");
        hdr.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");
        box.getChildren().add(hdr);
        VBox optList = new VBox(6);
        Runnable rebuild = () -> {
            optList.getChildren().clear();
            for (int i = 0; i < q.getOptions().size(); i++) {
                QuizOption opt = q.getOptions().get(i);
                int idx = i;
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("option-row");
                if (opt.isCorrect()) row.getStyleClass().add("option-row-correct");
                CheckBox cb = new CheckBox();
                cb.setSelected(opt.isCorrect());
                cb.selectedProperty().addListener((o,ov,nv) -> {
                    if (!multi) q.getOptions().forEach(x -> x.setCorrect(false));
                    opt.setCorrect(nv);
                });
                TextField tf = new TextField(opt.getOptionText());
                tf.setPromptText("Option " + (idx+1));
                HBox.setHgrow(tf, Priority.ALWAYS);
                tf.textProperty().addListener((o,ov,nv) -> opt.setOptionText(nv));
                Button del = new Button("✕");
                del.getStyleClass().add("question-action-btn");
                del.setOnAction(e -> { q.getOptions().remove(opt); rebuildOptList(optList,q,multi); });
                row.getChildren().addAll(cb, tf, del);
                optList.getChildren().add(row);
            }
        };
        rebuild.run();
        Button addBtn = new Button("➕ Add Option");
        addBtn.getStyleClass().add("add-item-btn");
        addBtn.setOnAction(e -> {
            QuizOption no = new QuizOption();
            no.setOptionText("");
            no.setOptionOrder(q.getOptions().size()+1);
            q.getOptions().add(no);
            rebuildOptList(optList,q,multi);
        });
        box.getChildren().addAll(optList, addBtn);
        return box;
    }

    private static void rebuildOptList(VBox optList, QuizQuestion q, boolean multi) {
        optList.getChildren().clear();
        for (int i = 0; i < q.getOptions().size(); i++) {
            QuizOption opt = q.getOptions().get(i);
            int idx = i;
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("option-row");
            if (opt.isCorrect()) row.getStyleClass().add("option-row-correct");
            CheckBox cb = new CheckBox();
            cb.setSelected(opt.isCorrect());
            cb.selectedProperty().addListener((o,ov,nv) -> {
                if (!multi) q.getOptions().forEach(x -> x.setCorrect(false));
                opt.setCorrect(nv);
            });
            TextField tf = new TextField(opt.getOptionText());
            tf.setPromptText("Option " + (idx+1));
            HBox.setHgrow(tf, Priority.ALWAYS);
            tf.textProperty().addListener((o,ov,nv) -> opt.setOptionText(nv));
            Button del = new Button("✕");
            del.getStyleClass().add("question-action-btn");
            del.setOnAction(e -> { q.getOptions().remove(opt); rebuildOptList(optList,q,multi); });
            row.getChildren().addAll(cb, tf, del);
            optList.getChildren().add(row);
        }
    }

    private static VBox buildTrueFalseEditor(QuizQuestion q) {
        VBox box = new VBox(8);
        if (q.getOptions().isEmpty()) {
            QuizOption t = new QuizOption(); t.setOptionText("True"); t.setCorrect(true); t.setOptionOrder(1);
            QuizOption f = new QuizOption(); f.setOptionText("False"); f.setCorrect(false); f.setOptionOrder(2);
            q.getOptions().addAll(List.of(t, f));
        }
        Label hdr = new Label("✓ Select the correct answer:");
        hdr.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        ToggleGroup tg = new ToggleGroup();
        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_LEFT);
        for (QuizOption opt : q.getOptions()) {
            RadioButton rb = new RadioButton(opt.getOptionText());
            rb.setToggleGroup(tg);
            rb.setSelected(opt.isCorrect());
            rb.setStyle("-fx-font-size:15px;-fx-padding:8 20;");
            rb.selectedProperty().addListener((o,ov,nv) -> {
                q.getOptions().forEach(x -> x.setCorrect(false));
                opt.setCorrect(nv);
            });
            btns.getChildren().add(rb);
        }
        box.getChildren().addAll(hdr, btns);
        return box;
    }

    private static VBox buildShortAnswerEditor(QuizQuestion q) {
        VBox box = new VBox(8);
        Label hdr = new Label("✎ Accepted Answers (case-insensitive):");
        hdr.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        box.getChildren().add(hdr);
        VBox list = new VBox(6);
        Runnable rebuild = () -> {
            list.getChildren().clear();
            for (QuestionAcceptedAnswer a : q.getAcceptedAnswers()) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("option-row");
                TextField tf = new TextField(a.getAnswerText());
                tf.setPromptText("Accepted answer");
                HBox.setHgrow(tf, Priority.ALWAYS);
                tf.textProperty().addListener((o,ov,nv) -> a.setAnswerText(nv));
                Button del = new Button("✕");
                del.getStyleClass().add("question-action-btn");
                del.setOnAction(e -> { q.getAcceptedAnswers().remove(a); rebuildAccepted(list,q); });
                row.getChildren().addAll(tf, del);
                list.getChildren().add(row);
            }
        };
        rebuild.run();
        Button addBtn = new Button("➕ Add Accepted Answer");
        addBtn.getStyleClass().add("add-item-btn");
        addBtn.setOnAction(e -> { q.getAcceptedAnswers().add(new QuestionAcceptedAnswer("")); rebuildAccepted(list,q); });
        box.getChildren().addAll(list, addBtn);
        return box;
    }

    private static void rebuildAccepted(VBox list, QuizQuestion q) {
        list.getChildren().clear();
        for (QuestionAcceptedAnswer a : q.getAcceptedAnswers()) {
            HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("option-row");
            TextField tf = new TextField(a.getAnswerText()); tf.setPromptText("Accepted answer");
            HBox.setHgrow(tf, Priority.ALWAYS); tf.textProperty().addListener((o,ov,nv) -> a.setAnswerText(nv));
            Button del = new Button("✕"); del.getStyleClass().add("question-action-btn");
            del.setOnAction(e -> { q.getAcceptedAnswers().remove(a); rebuildAccepted(list,q); });
            row.getChildren().addAll(tf, del); list.getChildren().add(row);
        }
    }

    private static VBox buildEssayEditor(QuizQuestion q) {
        VBox box = new VBox(8);
        Label hdr = new Label("📝 Essay Settings:");
        hdr.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        TextField modelF = new TextField(q.getModelAnswer() != null ? q.getModelAnswer() : "");
        modelF.setPromptText("Model answer (reference for grading)");
        modelF.textProperty().addListener((o,ov,nv) -> q.setModelAnswer(nv));
        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        Spinner<Integer> wcSpin = new Spinner<>(0, 10000, q.getMaxWordCount() != null ? q.getMaxWordCount() : 500, 50);
        wcSpin.setPrefWidth(100); wcSpin.setEditable(true);
        wcSpin.valueProperty().addListener((o,ov,nv) -> q.setMaxWordCount(nv));
        CheckBox manualCb = new CheckBox("Requires Manual Grading");
        manualCb.setSelected(true); q.setManualGrading(true);
        manualCb.selectedProperty().addListener((o,ov,nv) -> q.setManualGrading(nv));
        row.getChildren().addAll(new Label("Max Words:"), wcSpin, manualCb);
        box.getChildren().addAll(hdr, new Label("Model Answer:"), modelF, row);
        return box;
    }

    private static VBox buildFillBlankEditor(QuizQuestion q) {
        VBox box = new VBox(8);
        Label hdr = new Label("▁ Fill in the Blank Template:");
        hdr.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        Label hint = new Label("Use ___ (three underscores) for blanks");
        hint.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
        TextArea tmpl = new TextArea(q.getBlankTemplate() != null ? q.getBlankTemplate() : "");
        tmpl.setPromptText("Java is a ___ programming language.");
        tmpl.setPrefRowCount(2);
        tmpl.textProperty().addListener((o,ov,nv) -> q.setBlankTemplate(nv));
        box.getChildren().addAll(hdr, hint, tmpl);
        box.getChildren().add(buildShortAnswerEditor(q)); // reuse accepted answers
        return box;
    }

    private static VBox buildMatchingEditor(QuizQuestion q) {
        VBox box = new VBox(8);
        Label hdr = new Label("⇔ Matching Pairs:");
        hdr.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        box.getChildren().add(hdr);
        VBox list = new VBox(6);
        Runnable rebuild = () -> rebuildMatchPairs(list, q);
        rebuild.run();
        Button addBtn = new Button("➕ Add Pair");
        addBtn.getStyleClass().add("add-item-btn");
        addBtn.setOnAction(e -> {
            q.getMatchPairs().add(new QuestionMatchPair("","",q.getMatchPairs().size()+1));
            rebuildMatchPairs(list, q);
        });
        box.getChildren().addAll(list, addBtn);
        return box;
    }

    private static void rebuildMatchPairs(VBox list, QuizQuestion q) {
        list.getChildren().clear();
        for (int i = 0; i < q.getMatchPairs().size(); i++) {
            QuestionMatchPair p = q.getMatchPairs().get(i);
            HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("match-pair-row");
            Label num = new Label((i+1)+"."); num.setStyle("-fx-font-weight:bold;-fx-text-fill:#7C3AED;");
            TextField lf = new TextField(p.getLeftItem()); lf.setPromptText("Left item");
            HBox.setHgrow(lf, Priority.ALWAYS); lf.textProperty().addListener((o,ov,nv) -> p.setLeftItem(nv));
            Label arrow = new Label("⇔"); arrow.setStyle("-fx-font-size:16px;-fx-text-fill:#7C3AED;");
            TextField rf = new TextField(p.getRightItem()); rf.setPromptText("Right item");
            HBox.setHgrow(rf, Priority.ALWAYS); rf.textProperty().addListener((o,ov,nv) -> p.setRightItem(nv));
            Button del = new Button("✕"); del.getStyleClass().add("question-action-btn");
            del.setOnAction(e -> { q.getMatchPairs().remove(p); rebuildMatchPairs(list,q); });
            row.getChildren().addAll(num,lf,arrow,rf,del); list.getChildren().add(row);
        }
    }

    private static VBox buildOrderingEditor(QuizQuestion q) {
        VBox box = new VBox(8);
        Label hdr = new Label("↕ Items in Correct Order:");
        hdr.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        Label hint = new Label("Enter items in the correct sequence. Students will reorder them.");
        hint.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
        box.getChildren().addAll(hdr, hint);
        VBox list = new VBox(6);
        Runnable rebuild = () -> rebuildSeqItems(list, q);
        rebuild.run();
        Button addBtn = new Button("➕ Add Item");
        addBtn.getStyleClass().add("add-item-btn");
        addBtn.setOnAction(e -> {
            q.getSequenceItems().add(new QuestionSequenceItem("", q.getSequenceItems().size()+1));
            rebuildSeqItems(list, q);
        });
        box.getChildren().addAll(list, addBtn);
        return box;
    }

    private static void rebuildSeqItems(VBox list, QuizQuestion q) {
        list.getChildren().clear();
        for (int i = 0; i < q.getSequenceItems().size(); i++) {
            QuestionSequenceItem s = q.getSequenceItems().get(i);
            s.setCorrectPosition(i + 1);
            HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("sequence-item");
            Label num = new Label(String.valueOf(i+1)); num.getStyleClass().add("question-number-badge");
            TextField tf = new TextField(s.getItemText()); tf.setPromptText("Step " + (i+1));
            HBox.setHgrow(tf, Priority.ALWAYS); tf.textProperty().addListener((o,ov,nv) -> s.setItemText(nv));
            Button up = new Button("▲"); up.getStyleClass().add("question-action-btn");
            Button dn = new Button("▼"); dn.getStyleClass().add("question-action-btn");
            int fi = i;
            up.setOnAction(e -> { if(fi>0){var tmp=q.getSequenceItems().remove(fi);q.getSequenceItems().add(fi-1,tmp);rebuildSeqItems(list,q);}});
            dn.setOnAction(e -> { if(fi<q.getSequenceItems().size()-1){var tmp=q.getSequenceItems().remove(fi);q.getSequenceItems().add(fi+1,tmp);rebuildSeqItems(list,q);}});
            Button del = new Button("✕"); del.getStyleClass().add("question-action-btn");
            del.setOnAction(e -> { q.getSequenceItems().remove(s); rebuildSeqItems(list,q); });
            row.getChildren().addAll(num,tf,up,dn,del); list.getChildren().add(row);
        }
    }
}
