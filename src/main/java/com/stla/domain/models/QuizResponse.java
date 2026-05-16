package com.stla.domain.models;

/**
 * Quiz response model matching public.quiz_responses / quiz_answers table.
 * Records a student's answer to a single question within an attempt.
 * Extended to support text-based, matching, and ordering answers.
 */
public class QuizResponse {

    private String id;
    private String attemptId;
    private String questionId;
    private String[] selectedOptionIds;
    private boolean correct;

    // Extended fields for advanced question types
    private String answerText;        // For short_answer, essay, fill_blank
    private String matchingAnswers;   // JSON string for matching answers
    private String[] orderingAnswer;  // Ordered item IDs for ordering questions
    private double pointsAwarded;     // Partial scoring support

    public QuizResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String[] getSelectedOptionIds() { return selectedOptionIds; }
    public void setSelectedOptionIds(String[] selectedOptionIds) { this.selectedOptionIds = selectedOptionIds; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public String getMatchingAnswers() { return matchingAnswers; }
    public void setMatchingAnswers(String matchingAnswers) { this.matchingAnswers = matchingAnswers; }

    public String[] getOrderingAnswer() { return orderingAnswer; }
    public void setOrderingAnswer(String[] orderingAnswer) { this.orderingAnswer = orderingAnswer; }

    public double getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(double pointsAwarded) { this.pointsAwarded = pointsAwarded; }
}
