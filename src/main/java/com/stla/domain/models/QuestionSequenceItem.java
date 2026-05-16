package com.stla.domain.models;

/**
 * Sequence item for ordering question type.
 * Maps to public.question_sequence_items table.
 */
public class QuestionSequenceItem {
    private String id;
    private String questionId;
    private String itemText;
    private int correctPosition;

    public QuestionSequenceItem() {}

    public QuestionSequenceItem(String itemText, int correctPosition) {
        this.itemText = itemText;
        this.correctPosition = correctPosition;
    }

    public String getId()         { return id; }
    public void setId(String id)  { this.id = id; }
    public String getQuestionId()              { return questionId; }
    public void setQuestionId(String qid)      { this.questionId = qid; }
    public String getItemText()                { return itemText; }
    public void setItemText(String text)       { this.itemText = text; }
    public int getCorrectPosition()            { return correctPosition; }
    public void setCorrectPosition(int pos)    { this.correctPosition = pos; }
}
