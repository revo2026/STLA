package com.stla.ui.student;

import com.stla.core.navigation.StudentNavigationContext;
import com.stla.domain.models.Quiz;
import com.stla.domain.models.QuizAttempt;
import com.stla.domain.models.QuizQuestion;
import com.stla.patterns.facade.EnrollmentFacade;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Loads student enrollment flow screens into a dashboard scroll pane. */
public final class StudentFlowHelper {

    private StudentFlowHelper() {}

    public static void openCourseDetails(ScrollPane scrollPane, String courseId,
                                         Consumer<String> onEnroll, Consumer<String> onContinue) {
        openCourseDetails(scrollPane, courseId, onEnroll, onContinue, null);
    }

    public static void openCourseDetails(ScrollPane scrollPane, String courseId,
                                         Consumer<String> onEnroll, Consumer<String> onContinue,
                                         Consumer<String> onStartQuiz) {
        StudentNavigationContext.stopActiveVideo();
        StudentNavigationContext.setSelectedCourseId(courseId);
        try {
            FXMLLoader loader = new FXMLLoader(StudentFlowHelper.class.getResource("/com/stla/views/student/course-details.fxml"));
            Parent root = loader.load();
            CourseDetailsController ctrl = loader.getController();
            ctrl.setOnEnrollNow(onEnroll != null ? onEnroll : StudentNavigationContext::goToCheckout);
            ctrl.setOnContinueLearning(onContinue);
            ctrl.setOnStartQuiz(onStartQuiz);
            ctrl.setCourseId(courseId);
            applyCheckoutStyles(root);
            scrollPane.setContent(root);
        } catch (Exception e) {
            System.err.println("Course details load error: " + e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) System.err.println("  Caused by: " + cause.getMessage());
            e.printStackTrace();
        }
    }

    public static void openCheckout(ScrollPane scrollPane, String courseId, Consumer<EnrollmentFacade.PurchaseResult> onSuccess) {
        StudentNavigationContext.stopActiveVideo();
        StudentNavigationContext.setSelectedCourseId(courseId);
        try {
            FXMLLoader loader = new FXMLLoader(StudentFlowHelper.class.getResource("/com/stla/views/student/checkout.fxml"));
            Parent root = loader.load();
            CheckoutController ctrl = loader.getController();
            ctrl.setCourseId(courseId);
            ctrl.setOnSuccess(onSuccess);
            applyCheckoutStyles(root);
            scrollPane.setContent(root);
        } catch (Exception e) {
            System.err.println("Checkout load error: " + e.getMessage());
            if (e.getCause() != null) System.err.println("  Caused by: " + e.getCause().getMessage());
            e.printStackTrace();
        }
    }

    public static void openPaymentSuccess(ScrollPane scrollPane, Consumer<String> onContinue) {
        StudentNavigationContext.stopActiveVideo();
        try {
            FXMLLoader loader = new FXMLLoader(StudentFlowHelper.class.getResource("/com/stla/views/student/payment-success.fxml"));
            Parent root = loader.load();
            PaymentSuccessController ctrl = loader.getController();
            ctrl.setOnContinueLearning(onContinue);
            applyCheckoutStyles(root);
            scrollPane.setContent(root);
        } catch (IOException e) {
            System.err.println("Payment success load error: " + e.getMessage());
        }
    }

    public static void openCoursePlayer(ScrollPane scrollPane, String courseId, String courseTitle) {
        openCoursePlayer(scrollPane, courseId, courseTitle, null);
    }

    public static void openCoursePlayer(ScrollPane scrollPane, String courseId, String courseTitle,
                                        Runnable onBack) {
        openCoursePlayer(scrollPane, courseId, courseTitle, onBack, null);
    }

    public static void openCoursePlayer(ScrollPane scrollPane, String courseId, String courseTitle,
                                        Runnable onBack, Consumer<String> onStartQuiz) {
        StudentNavigationContext.stopActiveVideo();
        StudentNavigationContext.setSelectedCourseId(courseId);
        try {
            FXMLLoader loader = new FXMLLoader(StudentFlowHelper.class.getResource("/com/stla/views/student/student-course-player.fxml"));
            Parent root = loader.load();
            StudentCoursePlayerController ctrl = loader.getController();
            ctrl.setCourseId(courseId);
            ctrl.setCourseTitle(courseTitle);
            ctrl.setOnStartQuiz(onStartQuiz);
            if (onBack != null) {
                ctrl.setOnBack(v -> onBack.run());
            }
            applyPlayerStyles(root);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setContent(root);
            ctrl.start();
        } catch (Exception e) {
            System.err.println("Player load error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void applyPlayerStyles(Parent root) {
        if (root.getScene() != null) {
            addPlayerStyles(root.getScene().getStylesheets());
        } else {
            root.sceneProperty().addListener((o, ov, scene) -> {
                if (scene != null) addPlayerStyles(scene.getStylesheets());
            });
        }
    }

    private static void addPlayerStyles(javafx.collections.ObservableList<String> sheets) {
        String player = StudentFlowHelper.class.getResource("/com/stla/css/player.css").toExternalForm();
        if (!sheets.contains(player)) sheets.add(player);
    }

    private static void applyCheckoutStyles(Parent root) {
        if (root.getScene() != null) {
            addStyles(root.getScene().getStylesheets());
        } else {
            root.sceneProperty().addListener((o, ov, scene) -> {
                if (scene != null) addStyles(scene.getStylesheets());
            });
        }
    }

    private static void addStyles(javafx.collections.ObservableList<String> sheets) {
        String checkout = StudentFlowHelper.class.getResource("/com/stla/css/checkout.css").toExternalForm();
        String payment = StudentFlowHelper.class.getResource("/com/stla/css/payment.css").toExternalForm();
        if (!sheets.contains(checkout)) sheets.add(checkout);
        if (!sheets.contains(payment)) sheets.add(payment);
    }

    public static void openQuizTake(ScrollPane scrollPane, Quiz quiz, String studentId,
                                    QuizTakeController.ResultCallback onFinished) {
        StudentNavigationContext.stopActiveVideo();
        try {
            FXMLLoader loader = new FXMLLoader(
                    StudentFlowHelper.class.getResource("/com/stla/views/student/quiz-take.fxml"));
            Parent root = loader.load();
            QuizTakeController ctrl = loader.getController();
            ctrl.setResultCallback(onFinished);
            ctrl.setQuiz(quiz, studentId);
            applyQuizStyles(root);
            scrollPane.setFitToWidth(true);
            scrollPane.setContent(root);
        } catch (Exception e) {
            System.err.println("Quiz take load error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void openQuizResult(ScrollPane scrollPane, Quiz quiz, QuizAttempt attempt,
                                      List<QuizQuestion> questions,
                                      Map<String, QuizResultController.QuizResponseSnapshot> snapshots,
                                      Consumer<String> onContinue) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    StudentFlowHelper.class.getResource("/com/stla/views/student/quiz-result.fxml"));
            Parent root = loader.load();
            QuizResultController ctrl = loader.getController();
            ctrl.setOnContinue(onContinue);
            ctrl.showResult(quiz, attempt, questions, snapshots);
            applyQuizStyles(root);
            scrollPane.setContent(root);
        } catch (Exception e) {
            System.err.println("Quiz result load error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void applyQuizStyles(Parent root) {
        if (root.getScene() != null) {
            addQuizStyles(root.getScene().getStylesheets());
        } else {
            root.sceneProperty().addListener((o, ov, scene) -> {
                if (scene != null) addQuizStyles(scene.getStylesheets());
            });
        }
    }

    private static void addQuizStyles(javafx.collections.ObservableList<String> sheets) {
        String quiz = StudentFlowHelper.class.getResource("/com/stla/css/quiz.css").toExternalForm();
        if (!sheets.contains(quiz)) sheets.add(quiz);
    }
}
