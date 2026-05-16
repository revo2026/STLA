package com.stla.services;

import com.stla.data.repositories.QuestionRepositoryImpl;
import com.stla.data.repositories.QuizRepositoryImpl;
import com.stla.domain.enums.QuestionType;
import com.stla.domain.models.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for advanced question management.
 * Handles saving/updating full questions with all sub-entities.
 * Controllers call this service; SQL is in repositories only.
 */
public class QuestionService {

    private final QuizRepositoryImpl quizRepo = new QuizRepositoryImpl();
    private final QuestionRepositoryImpl questionRepo = new QuestionRepositoryImpl();
    private final SupabaseStorageAdapter storageAdapter = new SupabaseStorageAdapter();

    // ==================== FULL QUESTION SAVE ====================

    /**
     * Save a complete question with all sub-entities (options, accepted answers, etc).
     * Sets the question order automatically if not set.
     */
    public String saveFullQuestion(QuizQuestion q) {
        if (q.getQuestionOrder() <= 0) {
            q.setQuestionOrder(quizRepo.countQuestionsByQuizId(q.getQuizId()) + 1);
        }
        String qId = quizRepo.saveQuestion(q);
        if (qId == null) return null;
        q.setId(qId);
        saveSubEntities(q);
        return qId;
    }

    /**
     * Update a question and replace all sub-entities.
     */
    public void updateFullQuestion(QuizQuestion q) {
        quizRepo.updateQuestion(q);
        // Delete old sub-entities and re-create
        QuestionType type = QuestionType.fromDbValue(q.getQuestionType());
        if (type.usesOptions()) {
            quizRepo.deleteOptionsByQuestion(q.getId());
            for (QuizOption opt : q.getOptions()) {
                opt.setQuestionId(q.getId());
                quizRepo.saveOption(opt);
            }
        }
        if (type.usesAcceptedAnswers()) {
            questionRepo.deleteAcceptedAnswersByQuestion(q.getId());
            for (QuestionAcceptedAnswer a : q.getAcceptedAnswers()) {
                a.setQuestionId(q.getId());
                questionRepo.saveAcceptedAnswer(a);
            }
        }
        if (type.usesMatchPairs()) {
            questionRepo.deleteMatchPairsByQuestion(q.getId());
            for (QuestionMatchPair p : q.getMatchPairs()) {
                p.setQuestionId(q.getId());
                questionRepo.saveMatchPair(p);
            }
        }
        if (type.usesSequenceItems()) {
            questionRepo.deleteSequenceItemsByQuestion(q.getId());
            for (QuestionSequenceItem s : q.getSequenceItems()) {
                s.setQuestionId(q.getId());
                questionRepo.saveSequenceItem(s);
            }
        }
    }

    /**
     * Load a question with all its sub-entities populated.
     */
    public QuizQuestion loadFullQuestion(QuizQuestion q) {
        QuestionType type = QuestionType.fromDbValue(q.getQuestionType());
        if (type.usesOptions()) {
            q.setOptions(quizRepo.findOptionsByQuestionId(q.getId()));
        }
        if (type.usesAcceptedAnswers()) {
            q.setAcceptedAnswers(questionRepo.findAcceptedAnswers(q.getId()));
        }
        if (type.usesMatchPairs()) {
            q.setMatchPairs(questionRepo.findMatchPairs(q.getId()));
        }
        if (type.usesSequenceItems()) {
            q.setSequenceItems(questionRepo.findSequenceItems(q.getId()));
        }
        return q;
    }

    /**
     * Load all questions for a quiz with full sub-entities.
     */
    public List<QuizQuestion> loadAllFullQuestions(String quizId) {
        List<QuizQuestion> questions = quizRepo.findQuestionsByQuizId(quizId);
        for (QuizQuestion q : questions) {
            loadFullQuestion(q);
        }
        return questions;
    }

    // ==================== QUESTION ACTIONS ====================

    public void deleteQuestion(String questionId) {
        quizRepo.deleteQuestion(questionId);
    }

    /**
     * Duplicate a question with all sub-entities.
     */
    public String duplicateQuestion(QuizQuestion original) {
        QuizQuestion copy = new QuizQuestion();
        copy.setQuizId(original.getQuizId());
        copy.setQuestionText(original.getQuestionText() + " (Copy)");
        copy.setDescription(original.getDescription());
        copy.setExplanation(original.getExplanation());
        copy.setQuestionType(original.getQuestionType());
        copy.setQuestionImageUrl(original.getQuestionImageUrl());
        copy.setDifficulty(original.getDifficulty());
        copy.setPoints(original.getPoints());
        copy.setRequired(original.isRequired());
        copy.setMaxWordCount(original.getMaxWordCount());
        copy.setModelAnswer(original.getModelAnswer());
        copy.setManualGrading(original.isManualGrading());
        copy.setBlankTemplate(original.getBlankTemplate());

        // Copy sub-entities
        copy.setOptions(new ArrayList<>(original.getOptions().stream().map(o -> {
            QuizOption no = new QuizOption();
            no.setOptionText(o.getOptionText());
            no.setCorrect(o.isCorrect());
            no.setOptionOrder(o.getOptionOrder());
            return no;
        }).toList()));
        copy.setAcceptedAnswers(new ArrayList<>(original.getAcceptedAnswers().stream().map(a -> {
            QuestionAcceptedAnswer na = new QuestionAcceptedAnswer(a.getAnswerText());
            return na;
        }).toList()));
        copy.setMatchPairs(new ArrayList<>(original.getMatchPairs().stream().map(p ->
            new QuestionMatchPair(p.getLeftItem(), p.getRightItem(), p.getPairOrder())
        ).toList()));
        copy.setSequenceItems(new ArrayList<>(original.getSequenceItems().stream().map(s ->
            new QuestionSequenceItem(s.getItemText(), s.getCorrectPosition())
        ).toList()));

        return saveFullQuestion(copy);
    }

    /**
     * Reorder questions by their IDs.
     */
    public void reorderQuestions(List<String> questionIds) {
        questionRepo.reorderQuestions(questionIds);
    }

    // ==================== IMAGE UPLOAD ====================

    public String uploadQuestionImage(File file, String questionId) {
        String url = storageAdapter.uploadQuestionImage(file, questionId);
        if (url != null) {
            questionRepo.updateQuestionImageUrl(questionId, url);
        }
        return url;
    }

    public void removeQuestionImage(String questionId, String currentUrl) {
        questionRepo.updateQuestionImageUrl(questionId, null);
        if (currentUrl != null) {
            try {
                SupabaseStorageService.getInstance().deleteOldFile(currentUrl);
            } catch (Exception e) {
                System.err.println("Error deleting old question image: " + e.getMessage());
            }
        }
    }

    // ==================== VALIDATION ====================

    public List<String> validateQuestion(QuizQuestion q) {
        List<String> errors = new ArrayList<>();
        if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
            errors.add("Question text is required");
        }
        if (q.getPoints() <= 0) {
            errors.add("Points must be greater than 0");
        }
        QuestionType type = QuestionType.fromDbValue(q.getQuestionType());
        switch (type) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE, IMAGE_QUESTION -> {
                if (q.getOptions() == null || q.getOptions().size() < 2)
                    errors.add("At least 2 options are required");
                if (q.getOptions() != null && q.getOptions().stream().noneMatch(QuizOption::isCorrect))
                    errors.add("At least one correct answer is required");
            }
            case TRUE_FALSE -> {
                if (q.getOptions() == null || q.getOptions().size() != 2)
                    errors.add("True/False requires exactly 2 options");
                if (q.getOptions() != null && q.getOptions().stream().noneMatch(QuizOption::isCorrect))
                    errors.add("Select the correct answer (True or False)");
            }
            case SHORT_ANSWER -> {
                if (q.getAcceptedAnswers() == null || q.getAcceptedAnswers().isEmpty())
                    errors.add("At least one accepted answer is required");
            }
            case FILL_BLANK -> {
                if (q.getBlankTemplate() == null || !q.getBlankTemplate().contains("___"))
                    errors.add("Template must contain at least one blank (___)");
                if (q.getAcceptedAnswers() == null || q.getAcceptedAnswers().isEmpty())
                    errors.add("Blank answers are required");
            }
            case MATCHING -> {
                if (q.getMatchPairs() == null || q.getMatchPairs().size() < 2)
                    errors.add("At least 2 matching pairs are required");
            }
            case ORDERING -> {
                if (q.getSequenceItems() == null || q.getSequenceItems().size() < 2)
                    errors.add("At least 2 items are required for ordering");
            }
            case ESSAY -> { /* No specific validation needed */ }
        }
        return errors;
    }

    // ==================== PRIVATE ====================

    private void saveSubEntities(QuizQuestion q) {
        QuestionType type = QuestionType.fromDbValue(q.getQuestionType());
        if (type.usesOptions() && q.getOptions() != null) {
            for (QuizOption opt : q.getOptions()) {
                opt.setQuestionId(q.getId());
                quizRepo.saveOption(opt);
            }
        }
        if (type.usesAcceptedAnswers() && q.getAcceptedAnswers() != null) {
            for (QuestionAcceptedAnswer a : q.getAcceptedAnswers()) {
                a.setQuestionId(q.getId());
                questionRepo.saveAcceptedAnswer(a);
            }
        }
        if (type.usesMatchPairs() && q.getMatchPairs() != null) {
            for (QuestionMatchPair p : q.getMatchPairs()) {
                p.setQuestionId(q.getId());
                questionRepo.saveMatchPair(p);
            }
        }
        if (type.usesSequenceItems() && q.getSequenceItems() != null) {
            for (QuestionSequenceItem s : q.getSequenceItems()) {
                s.setQuestionId(q.getId());
                questionRepo.saveSequenceItem(s);
            }
        }
    }
}
