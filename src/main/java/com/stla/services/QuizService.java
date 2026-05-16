package com.stla.services;

import com.stla.data.repositories.CourseRepositoryImpl;
import com.stla.data.repositories.QuizAttemptRepositoryImpl;
import com.stla.data.repositories.QuizRepositoryImpl;
import com.stla.domain.enums.QuestionType;
import com.stla.domain.enums.QuizAccessStatus;
import com.stla.domain.models.*;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;
import com.stla.patterns.proxy.CourseAccessProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for quiz management and student quiz attempts.
 * Controllers call this service; SQL is in repositories only.
 */
public class QuizService {

    private final QuizRepositoryImpl quizRepo = new QuizRepositoryImpl();
    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();
    private final QuizAttemptRepositoryImpl attemptRepo = new QuizAttemptRepositoryImpl();
    private final QuestionService questionService = new QuestionService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final CourseAccessProxy accessProxy = new CourseAccessProxy();
    private final LearningProgressService progressService = new LearningProgressService();

    // ==================== QUIZ CRUD ====================

    public List<Quiz> getQuizzes(String courseId) { return quizRepo.findByCourseId(courseId); }
    public Quiz getQuiz(String quizId) { return quizRepo.findById(quizId).orElse(null); }
    public String createQuiz(Quiz quiz) {
        String id = quizRepo.saveQuiz(quiz);
        quiz.setId(id);
        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.NEW_QUIZ_ADDED, null, quiz.getCourseId(),
                "A new quiz \"" + quiz.getTitle() + "\" was added to your course."));
        return id;
    }
    public void updateQuiz(Quiz quiz) { quizRepo.updateQuiz(quiz); }
    public void deleteQuiz(String quizId) { quizRepo.deleteQuiz(quizId); }
    public void togglePublish(String quizId, boolean published) { quizRepo.togglePublish(quizId, published); }

    public int countPublishedQuizzes(String courseId) {
        return quizRepo.countPublishedByCourseId(courseId);
    }

    // ==================== QUESTIONS ====================

    public List<QuizQuestion> getQuestionsWithOptions(String quizId) {
        return questionService.loadAllFullQuestions(quizId);
    }

    public String addQuestion(QuizQuestion q) { return quizRepo.saveQuestion(q); }
    public void updateQuestion(QuizQuestion q) { quizRepo.updateQuestion(q); }
    public void deleteQuestion(String questionId) { quizRepo.deleteQuestion(questionId); }
    public String addOption(QuizOption o) { return quizRepo.saveOption(o); }
    public void updateOption(QuizOption o) { quizRepo.updateOption(o); }
    public void deleteOption(String optionId) { quizRepo.deleteOption(optionId); }

    // ==================== STUDENT QUIZ VIEWS ====================

    public List<StudentQuizSummary> getCourseQuizzes(String courseId, String studentId) {
        String courseTitle = courseRepo.findById(courseId).map(c -> c.getTitle()).orElse("Course");
        List<Quiz> quizzes = quizRepo.findPublishedByCourseId(courseId);
        boolean enrolled = studentId != null && accessProxy.canAccessQuizzes(studentId, courseId);
        List<StudentQuizSummary> result = new ArrayList<>();
        for (Quiz quiz : quizzes) {
            StudentQuizSummary s = buildSummary(quiz, studentId, enrolled);
            s.setCourseId(courseId);
            s.setCourseTitle(courseTitle);
            result.add(s);
        }
        return result;
    }

    /** All published quizzes from courses the student is enrolled in. */
    public List<StudentQuizSummary> getAllStudentQuizzes(String studentId) {
        if (studentId == null) return List.of();
        List<StudentQuizSummary> all = new ArrayList<>();
        for (String courseId : enrollmentService.getEnrolledCourseIds(studentId)) {
            all.addAll(getCourseQuizzes(courseId, studentId));
        }
        return all;
    }

    private StudentQuizSummary buildSummary(Quiz quiz, String studentId, boolean enrolled) {
        StudentQuizSummary s = new StudentQuizSummary();
        s.setQuiz(quiz);
        s.setCourseId(quiz.getCourseId());
        s.setQuestionCount(quizRepo.countQuestionsByQuizId(quiz.getId()));
        s.setLocked(!enrolled);
        if (studentId == null || !enrolled) {
            s.setStatus(QuizAccessStatus.LOCKED);
            return s;
        }
        int used = attemptRepo.countAttempts(studentId, quiz.getId());
        s.setAttemptsUsed(used);
        s.setBestScorePercent(attemptRepo.findBestScorePercent(studentId, quiz.getId()));
        boolean passed = attemptRepo.hasPassedAttempt(studentId, quiz.getId());
        s.setPassed(passed);
        s.setStatus(passed ? QuizAccessStatus.COMPLETED : QuizAccessStatus.AVAILABLE);
        return s;
    }

    public Optional<String> validateCanStart(String studentId, String quizId) {
        Quiz quiz = getQuiz(quizId);
        if (quiz == null) return Optional.of("Quiz not found.");
        if (!quiz.isPublished()) return Optional.of("This quiz is not available.");
        if (studentId == null) return Optional.of("Please sign in to take quizzes.");
        if (!accessProxy.canAccessQuizzes(studentId, quiz.getCourseId())) {
            return Optional.of("Enroll in this course to access quizzes.");
        }
        int used = attemptRepo.countAttempts(studentId, quizId);
        if (used >= quiz.getAttemptsAllowed()) {
            return Optional.of("You have used all allowed attempts.");
        }
        if (quizRepo.countQuestionsByQuizId(quizId) == 0) {
            return Optional.of("This quiz has no questions yet.");
        }
        return Optional.empty();
    }

    public double getBestScore(String courseId, String studentId) {
        if (studentId == null) return 0;
        return quizRepo.findPublishedByCourseId(courseId).stream()
                .mapToDouble(q -> attemptRepo.findBestScorePercent(studentId, q.getId()))
                .max().orElse(0);
    }

    public List<QuizAttempt> getStudentQuizAttempts(String quizId, String studentId) {
        if (studentId == null) return List.of();
        return attemptRepo.findByStudentAndQuiz(studentId, quizId);
    }

    // ==================== SUBMIT ====================

    public QuizAttempt submitAttempt(String studentId, String quizId, List<QuizResponse> responses) {
        Optional<String> block = validateCanStart(studentId, quizId);
        if (block.isPresent()) throw new IllegalStateException(block.get());

        Quiz quiz = getQuiz(quizId);
        List<QuizQuestion> questions = getQuestionsWithOptions(quizId);
        int totalPoints = 0;
        double earnedPoints = 0;
        int correctCount = 0;

        for (QuizQuestion question : questions) {
            totalPoints += question.getPoints();
            QuizResponse resp = responses.stream()
                    .filter(r -> r.getQuestionId().equals(question.getId()))
                    .findFirst().orElse(null);
            if (resp == null) continue;

            double awarded = scoreQuestion(question, resp);
            resp.setPointsAwarded(awarded);
            resp.setCorrect(awarded >= question.getPoints());
            if (resp.isCorrect()) correctCount++;
            earnedPoints += awarded;
        }

        double scorePercent = totalPoints > 0 ? (earnedPoints * 100.0 / totalPoints) : 0;
        boolean passed = scorePercent >= quiz.getPassingScore();
        int attemptNumber = attemptRepo.countAttempts(studentId, quizId) + 1;

        String attemptId = attemptRepo.createAttempt(
                quizId, studentId, attemptNumber, scorePercent, passed,
                (int) Math.round(earnedPoints), totalPoints);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(attemptId);
        attempt.setQuizId(quizId);
        attempt.setStudentId(studentId);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setScorePercent(scorePercent);
        attempt.setScore(correctCount);
        attempt.setTotalPoints(questions.size());
        attempt.setPassed(passed);

        for (QuizResponse resp : responses) {
            resp.setAttemptId(attemptId);
            attemptRepo.saveAnswer(resp);
        }

        EventBus.getInstance().publish(new AppEvent(
                AppEvent.EventType.QUIZ_SUBMITTED, studentId, quizId,
                "Quiz attempt submitted for \"" + quiz.getTitle() + "\"."));

        if (passed) {
            progressService.markQuizPassed(studentId, quiz.getCourseId());
            EventBus.getInstance().publish(new AppEvent(
                    AppEvent.EventType.QUIZ_PASSED, studentId, quizId,
                    "You passed the quiz: " + quiz.getTitle()));
        } else {
            EventBus.getInstance().publish(new AppEvent(
                    AppEvent.EventType.QUIZ_FAILED, studentId, quizId,
                    "Quiz attempt failed for \"" + quiz.getTitle() + "\". Try again if attempts remain."));
        }
        return attempt;
    }

    private double scoreQuestion(QuizQuestion question, QuizResponse resp) {
        QuestionType type = QuestionType.fromDbValue(question.getQuestionType());
        int pts = question.getPoints();

        return switch (type) {
            case SINGLE_CHOICE, TRUE_FALSE, IMAGE_QUESTION -> {
                List<String> correctIds = question.getOptions().stream()
                        .filter(QuizOption::isCorrect).map(QuizOption::getId).toList();
                String[] selected = resp.getSelectedOptionIds();
                boolean ok = selected != null && selected.length == 1 && correctIds.contains(selected[0]);
                yield ok ? pts : 0;
            }
            case MULTIPLE_CHOICE -> {
                List<String> correctIds = question.getOptions().stream()
                        .filter(QuizOption::isCorrect).map(QuizOption::getId).toList();
                String[] selected = resp.getSelectedOptionIds();
                boolean ok = selected != null
                        && selected.length == correctIds.size()
                        && List.of(selected).containsAll(correctIds);
                yield ok ? pts : 0;
            }
            case SHORT_ANSWER, FILL_BLANK -> {
                String answer = resp.getAnswerText();
                if (answer == null) yield 0;
                boolean ok = question.getAcceptedAnswers().stream()
                        .anyMatch(a -> a.getAnswerText().equalsIgnoreCase(answer.trim()));
                yield ok ? pts : 0;
            }
            case MATCHING -> scoreMatching(question, resp.getMatchingAnswers()) ? pts : 0;
            case ORDERING -> {
                String[] order = resp.getOrderingAnswer();
                yield (order != null && scoreOrdering(question, order)) ? pts : 0;
            }
            case ESSAY -> 0;
        };
    }

    private boolean scoreMatching(QuizQuestion question, String matchingJson) {
        if (matchingJson == null || matchingJson.isBlank()) return false;
        try {
            for (QuestionMatchPair pair : question.getMatchPairs()) {
                if (!matchingJson.contains(pair.getLeftItem()) || !matchingJson.contains(pair.getRightItem())) {
                    return false;
                }
            }
            return !question.getMatchPairs().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean scoreOrdering(QuizQuestion question, String[] submittedOrder) {
        List<QuestionSequenceItem> items = question.getSequenceItems();
        if (submittedOrder == null || submittedOrder.length != items.size()) return false;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).getId().equals(submittedOrder[i])) return false;
        }
        return true;
    }
}
