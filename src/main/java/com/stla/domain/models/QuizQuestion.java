package com.stla.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Extended quiz question model supporting all 9 question types.
 * Maps to public.quiz_questions table.
 */
public class QuizQuestion {
    private String id;
    private String quizId;
    private String questionText;
    private String description;
    private String explanation;
    private String questionType;
    private String questionImageUrl;
    private String difficulty;
    private int points;
    private int questionOrder;
    private boolean isRequired;
    private Integer maxWordCount;
    private String modelAnswer;
    private boolean manualGrading;
    private String blankTemplate;
    private LocalDateTime createdAt;

    // Transient: loaded sub-entities
    private List<QuizOption> options = new ArrayList<>();
    private List<QuestionAcceptedAnswer> acceptedAnswers = new ArrayList<>();
    private List<QuestionMatchPair> matchPairs = new ArrayList<>();
    private List<QuestionSequenceItem> sequenceItems = new ArrayList<>();

    public QuizQuestion() {
        this.difficulty = "medium";
        this.isRequired = true;
        this.points = 1;
    }

    // --- Core fields ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuizId() { return quizId; }
    public void setQuizId(String qid) { this.quizId = qid; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String t) { this.questionText = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String e) { this.explanation = e; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String t) { this.questionType = t; }
    public int getPoints() { return points; }
    public void setPoints(int p) { this.points = p; }
    public int getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(int o) { this.questionOrder = o; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }

    // --- Extended fields ---
    public String getQuestionImageUrl() { return questionImageUrl; }
    public void setQuestionImageUrl(String url) { this.questionImageUrl = url; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String d) { this.difficulty = d; }
    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean r) { this.isRequired = r; }
    public Integer getMaxWordCount() { return maxWordCount; }
    public void setMaxWordCount(Integer c) { this.maxWordCount = c; }
    public String getModelAnswer() { return modelAnswer; }
    public void setModelAnswer(String a) { this.modelAnswer = a; }
    public boolean isManualGrading() { return manualGrading; }
    public void setManualGrading(boolean m) { this.manualGrading = m; }
    public String getBlankTemplate() { return blankTemplate; }
    public void setBlankTemplate(String t) { this.blankTemplate = t; }

    // --- Transient sub-entities ---
    public List<QuizOption> getOptions() { return options; }
    public void setOptions(List<QuizOption> options) { this.options = options; }
    public List<QuestionAcceptedAnswer> getAcceptedAnswers() { return acceptedAnswers; }
    public void setAcceptedAnswers(List<QuestionAcceptedAnswer> a) { this.acceptedAnswers = a; }
    public List<QuestionMatchPair> getMatchPairs() { return matchPairs; }
    public void setMatchPairs(List<QuestionMatchPair> m) { this.matchPairs = m; }
    public List<QuestionSequenceItem> getSequenceItems() { return sequenceItems; }
    public void setSequenceItems(List<QuestionSequenceItem> s) { this.sequenceItems = s; }
}
