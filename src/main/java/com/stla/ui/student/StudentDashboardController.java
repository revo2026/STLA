package com.stla.ui.student;

import com.stla.core.navigation.NavigationManager;
import com.stla.core.navigation.StudentNavigationContext;
import com.stla.core.session.SessionManager;
import com.stla.domain.models.*;
import com.stla.domain.models.Quiz;
import com.stla.services.CourseService;
import com.stla.services.DashboardService;
import com.stla.services.QuizService;
import com.stla.ui.components.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class StudentDashboardController {

    @javafx.fxml.FXML private BorderPane rootPane;
    @javafx.fxml.FXML private Label userNameLabel;
    @javafx.fxml.FXML private Label pageTitle;
    @javafx.fxml.FXML private Label welcomeLabel;
    @javafx.fxml.FXML private HBox statsRow;
    @javafx.fxml.FXML private HBox featuredCoursesRow;
    @javafx.fxml.FXML private VBox continueLearningBox;
    @javafx.fxml.FXML private VBox notificationsBox;
    @javafx.fxml.FXML private VBox contentArea;
    @javafx.fxml.FXML private ScrollPane contentScroll;
    @javafx.fxml.FXML private StackPane sidebarAvatarContainer;
    @javafx.fxml.FXML private StackPane headerAvatarContainer;

    private final DashboardService dashboardService = new DashboardService();
    private final CourseService courseService = new CourseService();
    private final QuizService quizService = new QuizService();
    private VBox dashboardContent;
    private CircularAvatar sidebarAvatar, headerAvatar;

    @javafx.fxml.FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        userNameLabel.setText(session.getCurrentUserName());
        welcomeLabel.setText("Welcome back, " + session.getCurrentUserName() + "! 👋");
        dashboardContent = contentArea;

        // Create circular avatars
        setupAvatars();

        // Subscribe to profile changes for reactive avatar updates
        session.addProfileChangeListener(this::refreshAvatars);

        StudentNavigationContext.setOpenCourseDetails(this::navigateToCourseDetails);
        StudentNavigationContext.setOpenCheckout(this::navigateToCheckout);
        StudentNavigationContext.setOpenCoursePlayer(this::navigateToPlayer);

        loadDashboardData();
    }

    private void setupAvatars() {
        SessionManager session = SessionManager.getInstance();
        String url = session.getCurrentUserAvatar();
        String name = session.getCurrentUserName();
        sidebarAvatar = new CircularAvatar(url, name, 18);
        headerAvatar = new CircularAvatar(url, name, 18);
        sidebarAvatarContainer.getChildren().setAll(sidebarAvatar);
        headerAvatarContainer.getChildren().setAll(headerAvatar);
        wireProfileNavigation();
    }

    private void wireProfileNavigation() {
        Runnable openProfile = this::handleNavProfile;
        sidebarAvatar.setOnProfileNavigate(openProfile);
        headerAvatar.setOnProfileNavigate(openProfile);
        sidebarAvatarContainer.setCursor(javafx.scene.Cursor.HAND);
        headerAvatarContainer.setCursor(javafx.scene.Cursor.HAND);
        sidebarAvatarContainer.setOnMouseClicked(e -> openProfile.run());
        headerAvatarContainer.setOnMouseClicked(e -> openProfile.run());
    }

    private void refreshAvatars() {
        SessionManager session = SessionManager.getInstance();
        String url = session.getCurrentUserAvatar();
        String name = session.getCurrentUserName();
        userNameLabel.setText(name);
        sidebarAvatar.setAvatar(url, name);
        headerAvatar.setAvatar(url, name);
    }

    private void loadDashboardData() {
        Task<Void> task = new Task<>() {
            List<Course> featured;
            List<Enrollment> enrollments;
            @Override protected Void call() {
                featured = dashboardService.getFeaturedCourses();
                String sid = SessionManager.getInstance().getCurrentStudent().getId();
                enrollments = courseService.getStudentEnrollments(sid);
                return null;
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    buildStatCards(enrollments);
                    buildFeaturedCourses(featured, enrollments);
                    buildContinueLearning(enrollments);
                    AnimationUtils.staggerFadeIn(contentArea, 80);
                });
            }
        };
        new Thread(task).start();
    }

    private void buildStatCards(List<Enrollment> enrollments) {
        int total = enrollments != null ? enrollments.size() : 0;
        long inProgress = enrollments != null ? enrollments.stream().filter(e -> e.getProgressPercent() > 0 && e.getProgressPercent() < 100).count() : 0;
        long completed = enrollments != null ? enrollments.stream().filter(e -> e.getProgressPercent() >= 100).count() : 0;
        statsRow.getChildren().clear();
        statsRow.getChildren().addAll(
            ComponentFactory.createStatCard("📚", "My Courses", String.valueOf(total), false),
            ComponentFactory.createStatCard("📈", "In Progress", String.valueOf(inProgress), false),
            ComponentFactory.createStatCard("✅", "Completed", String.valueOf(completed), false),
            ComponentFactory.createStatCard("🏆", "Certificates", "0", false)
        );
        AnimationUtils.staggerFadeIn(statsRow, 100);
    }

    private void buildFeaturedCourses(List<Course> courses, List<Enrollment> enrollments) {
        featuredCoursesRow.getChildren().clear();
        if (courses == null || courses.isEmpty()) {
            featuredCoursesRow.getChildren().add(new Label("No featured courses yet") {{ getStyleClass().add("text-secondary"); }});
            return;
        }
        java.util.Set<String> enrolledIds = enrollments != null
                ? enrollments.stream().map(Enrollment::getCourseId).collect(java.util.stream.Collectors.toSet())
                : java.util.Set.of();
        for (Course c : courses.subList(0, Math.min(4, courses.size()))) {
            String courseId = c.getId();
            if (courseId == null || courseId.isBlank()) continue;
            boolean enrolled = enrolledIds.contains(courseId);
            featuredCoursesRow.getChildren().add(
                    CourseCardFactory.createCard(
                            c,
                            enrolled,
                            quizService.countPublishedQuizzes(courseId),
                            () -> navigateToCourseDetails(courseId),
                            () -> {
                                if (enrolled) navigateToPlayer(courseId);
                                else navigateToCourseDetails(courseId);
                            }));
        }
    }

    private void buildContinueLearning(List<Enrollment> enrollments) {
        continueLearningBox.getChildren().clear();
        if (enrollments == null || enrollments.isEmpty()) {
            continueLearningBox.getChildren().add(ComponentFactory.createEmptyState("📚", "No courses in progress. Browse the catalog!"));
            return;
        }
        for (Enrollment en : enrollments.subList(0, Math.min(3, enrollments.size()))) {
            HBox row = new HBox(16);
            row.getStyleClass().add("card");
            row.setPadding(new Insets(16));
            row.setAlignment(Pos.CENTER_LEFT);
            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);
            info.getChildren().addAll(
                new Label(en.getCourseTitle()) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 15px;"); }},
                ComponentFactory.createProgressBar(en.getProgressPercent())
            );
            Button cont = new Button("Continue →");
            cont.getStyleClass().addAll("btn-primary", "btn-sm");
            String cid = en.getCourseId();
            cont.setOnAction(ev -> navigateToPlayer(cid));
            row.getChildren().addAll(info, cont);
            continueLearningBox.getChildren().add(row);
        }
    }

    private Object loadSubScreen(String fxmlPath, String title) {
        StudentNavigationContext.stopActiveVideo();
        try {
            pageTitle.setText(title);
            welcomeLabel.setText("");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            AnimationUtils.fadeIn(content, 300);
            contentScroll.setContent(content);
            return loader.getController();
        } catch (Exception e) {
            System.err.println("Error loading sub-screen: " + fxmlPath + " - " + e.getMessage());
            return null;
        }
    }

    private void showDashboard() {
        StudentNavigationContext.stopActiveVideo();
        pageTitle.setText("Dashboard");
        welcomeLabel.setText("Welcome back, " + SessionManager.getInstance().getCurrentUserName() + "! 👋");
        contentScroll.setContent(dashboardContent);
    }

    @javafx.fxml.FXML private void handleNavDashboard() { showDashboard(); }
    @javafx.fxml.FXML private void handleNavCatalog() {
        Object ctrl = loadSubScreen("/com/stla/views/student/course-catalog.fxml", "Course Catalog");
        if (ctrl instanceof CourseCatalogController catalog) {
            catalog.setOnCourseSelected(this::navigateToCourseDetails);
            catalog.setOnContinueLearning(this::navigateToPlayer);
        }
    }
    @javafx.fxml.FXML private void handleNavMyCourses() {
        Object ctrl = loadSubScreen("/com/stla/views/student/my-courses.fxml", "My Courses");
        if (ctrl instanceof MyCoursesController myCourses) {
            myCourses.setOnContinue(this::navigateToPlayer);
            myCourses.setOnRateInstructor(this::navigateToRateInstructor);
        }
    }

    private void navigateToRateInstructor(Enrollment enrollment) {
        Object ctrl = loadSubScreen("/com/stla/views/student/student-rate-instructor.fxml", "Rate Instructor");
        if (ctrl instanceof StudentRateInstructorController rate) {
            rate.setEnrollment(enrollment);
            rate.setOnBack(this::handleNavMyCourses);
            rate.setOnSubmitted(e -> handleNavMyCourses());
        }
    }

    public void navigateToCourseDetails(String courseId) {
        pageTitle.setText("Course Details");
        welcomeLabel.setText("");
        StudentFlowHelper.openCourseDetails(contentScroll, courseId, this::navigateToCheckout,
                this::navigateToPlayer, this::navigateToQuiz);
    }

    public void navigateToQuiz(String quizId) {
        StudentNavigationContext.stopActiveVideo();
        var student = SessionManager.getInstance().getCurrentStudent();
        if (student == null) return;
        Quiz quiz = quizService.getQuiz(quizId);
        if (quiz == null) return;

        var validation = quizService.validateCanStart(student.getId(), quizId);
        if (validation.isPresent()) {
            pageTitle.setText("Quiz");
            contentScroll.setContent(ComponentFactory.createEmptyState("🎯", validation.get()));
            return;
        }

        pageTitle.setText("Quiz — " + quiz.getTitle());
        welcomeLabel.setText("");
        StudentFlowHelper.openQuizTake(contentScroll, quiz, student.getId(),
                (q, attempt, questions, snapshots) ->
                        StudentFlowHelper.openQuizResult(contentScroll, q, attempt, questions, snapshots,
                                this::navigateToPlayer));
    }

    public void navigateToCheckout(String courseId) {
        pageTitle.setText("Checkout");
        StudentFlowHelper.openCheckout(contentScroll, courseId, result -> {
            if (result.success()) navigateToPaymentSuccess();
        });
    }

    public void navigateToPaymentSuccess() {
        pageTitle.setText("Payment Successful");
        StudentFlowHelper.openPaymentSuccess(contentScroll, this::navigateToPlayer);
    }

    public void navigateToPlayer(String courseId) {
        pageTitle.setText("Learning");
        welcomeLabel.setText("");
        StudentFlowHelper.openCoursePlayer(contentScroll, courseId, null, () -> handleNavMyCourses(),
                this::navigateToQuiz);
    }
    @javafx.fxml.FXML private void handleNavQuizzes() {
        StudentNavigationContext.stopActiveVideo();
        pageTitle.setText("Quizzes");
        welcomeLabel.setText("");
        Object ctrl = loadSubScreen("/com/stla/views/student/student-quizzes.fxml", "Quizzes");
        if (ctrl instanceof StudentQuizzesController quizzes) {
            quizzes.setOnStartQuiz(this::navigateToQuiz);
        }
    }
    @javafx.fxml.FXML private void handleNavCertificates() { loadSubScreen("/com/stla/views/student/certificates.fxml", "Certificates"); }
    @javafx.fxml.FXML private void handleNavReviews() {
        loadSubScreen("/com/stla/views/student/student-reviews.fxml", "My Reviews");
    }
    @javafx.fxml.FXML private void handleNavProfile() { loadSubScreen("/com/stla/views/student/student-profile.fxml", "Profile"); }
    @javafx.fxml.FXML private void handleNavNotifications() { loadSubScreen("/com/stla/views/student/notifications.fxml", "Notifications"); }
    @javafx.fxml.FXML private void handleLogout() {
        StudentNavigationContext.stopActiveVideo();
        NavigationManager.getInstance().navigateToLogin();
    }
}
