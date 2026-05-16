package com.stla.domain.models;

public class QuizOption {
    private String id;
    private String questionId;
    private String optionText;
    private boolean isCorrect;
    private int optionOrder;

    public QuizOption() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String qid) { this.questionId = qid; }
    public String getOptionText() { return optionText; }
    public void setOptionText(String t) { this.optionText = t; }
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean c) { isCorrect = c; }
    public int getOptionOrder() { return optionOrder; }
    public void setOptionOrder(int o) { this.optionOrder = o; }
}
