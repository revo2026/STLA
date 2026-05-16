package com.stla.domain.models;

/**
 * Match pair for matching question type.
 * Maps to public.question_match_pairs table.
 */
public class QuestionMatchPair {
    private String id;
    private String questionId;
    private String leftItem;
    private String rightItem;
    private int pairOrder;

    public QuestionMatchPair() {}

    public QuestionMatchPair(String leftItem, String rightItem, int order) {
        this.leftItem = leftItem;
        this.rightItem = rightItem;
        this.pairOrder = order;
    }

    public String getId()         { return id; }
    public void setId(String id)  { this.id = id; }
    public String getQuestionId()            { return questionId; }
    public void setQuestionId(String qid)    { this.questionId = qid; }
    public String getLeftItem()              { return leftItem; }
    public void setLeftItem(String item)     { this.leftItem = item; }
    public String getRightItem()             { return rightItem; }
    public void setRightItem(String item)    { this.rightItem = item; }
    public int getPairOrder()                { return pairOrder; }
    public void setPairOrder(int order)      { this.pairOrder = order; }
}
