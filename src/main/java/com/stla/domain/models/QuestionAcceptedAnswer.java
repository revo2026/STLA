package com.stla.domain.models;

/**
 * Accepted text answer for short_answer and fill_blank question types.
 * Maps to public.question_accepted_answers table.
 */
public class QuestionAcceptedAnswer {
    private String id;
    private String questionId;
    private String answerText;

    public QuestionAcceptedAnswer() {}

    public QuestionAcceptedAnswer(String answerText) {
        this.answerText = answerText;
    }

    public String getId()         { return id; }
    public void setId(String id)  { this.id = id; }
    public String getQuestionId()            { return questionId; }
    public void setQuestionId(String qid)    { this.questionId = qid; }
    public String getAnswerText()            { return answerText; }
    public void setAnswerText(String text)   { this.answerText = text; }
}
