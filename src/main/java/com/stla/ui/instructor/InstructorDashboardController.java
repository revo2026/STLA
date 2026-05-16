package com.stla.ui.instructor;

import com.stla.core.navigation.NavigationManager;
import com.stla.core.session.SessionManager;
import com.stla.domain.enums.CourseStatus;
import com.stla.domain.models.Course;
import com.stla.services.CourseService;
import com.stla.services.DashboardService;
import com.stla.services.WalletService;
import com.stla.ui.components.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;

public class InstructorDashboardController {

    @javafx.fxml.FXML private BorderPane rootPane;
    @javafx.fxml.FXML private Label userNameLabel;
    @javafx.fxml.FXML private Label pageTitle;
    @javafx.fxml.FXML private Label welcomeLabel;
    @javafx.fxml.FXML private HBox statsRow;
    @javafx.fxml.FXML private VBox coursesBox;
    @javafx.fxml.FXML private VBox contentArea;
    @javafx.fxml.FXML private ScrollPane contentScroll;
    @javafx.fxml.FXML private StackPane sidebarAvatarContainer;
    @javafx.fxml.FXML private StackPane headerAvatarContainer;
    @javafx.fxml.FXML private VBox verificationBanner;
    @javafx.fxml.FXML private StackPane notificationBellHost;

    private final DashboardService service = new DashboardService();
    private final CourseService courseService = new CourseService();
    private final WalletService walletService = new WalletService();
    private VBox dashboardContent;
    private CircularAvatar sidebarAvatar, headerAvatar;

    @javafx.fxml.FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        userNameLabel.setText(session.getCurrentUserName());
        welcomeLabel.setText("Welcome back, " + session.getCurrentUserName() + "!");
        dashboardContent = contentArea;
        NotificationBellHelper.install(notificationBellHost, this::handleNavNotifications);

        // Avatars
        String url = session.getCurrentUserAvatar();
        String name = session.getCurrentUserName();
        sidebarAvatar = new CircularAvatar(url, name, 18);
        headerAvatar = new CircularAvatar(url, name, 18);
        sidebarAvatarContainer.getChildren().setAll(sidebarAvatar);
        headerAvatarContainer.getChildren().setAll(headerAvatar);
        wireProfileNavigation();

        // Subscribe to profile changes
        session.addProfileChangeListener(() -> {
            String u = session.getCurrentUserAvatar();
            String n = session.getCurrentUserName();
            userNameLabel.setText(n);
            sidebarAvatar.setAvatar(u, n);
            headerAvatar.setAvatar(u, n);
        });

        buildVerificationBanner();
        loadData();
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

    private void loadData() {
        Task<Void> task = new Task<>() {
            List<Course> courses;
            BigDecimal revenue;
            int totalStudents;
            double avgRating;
            java.util.Map<String, java.math.BigDecimal> monthlyEarnings;
            @Override protected Void call() {
                String iid = SessionManager.getInstance().getCurrentInstructor().getId();
                courses = service.getInstructorCourses(iid);
                revenue = courseService.getInstructorRevenue(iid);
                totalStudents = courseService.getInstructorTotalStudents(iid);
                avgRating = courseService.getInstructorAverageRating(iid);
                monthlyEarnings = walletService.getMonthlyEarnings(iid);
                return null;
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    buildStats(courses, revenue, totalStudents, avgRating);
                    buildCourseList(courses);
                    addCharts(monthlyEarnings, courses);
                    AnimationUtils.staggerFadeIn(contentArea, 80);
                });
            }
        };
        new Thread(task).start();
    }

    private void buildStats(List<Course> courses, BigDecimal revenue, int students, double avgRating) {
        statsRow.getChildren().clear();
        int totalCourses = courses != null ? courses.size() : 0;
        String ratingLabel = avgRating > 0 ? String.format("%.1f", avgRating) : "—";
        statsRow.getChildren().addAll(
            ComponentFactory.createStatCard("📚", "Total Courses", String.valueOf(totalCourses), false),
            ComponentFactory.createStatCard("👥", "Total Students", String.valueOf(students), false),
            ComponentFactory.createStatCard("💰", "Your Earnings", formatMoney(revenue), false),
            ComponentFactory.createStatCard("⭐", "Avg Rating", ratingLabel, false)
        );
        AnimationUtils.staggerFadeIn(statsRow, 100);
    }

    private void addCharts(java.util.Map<String, java.math.BigDecimal> monthlyEarnings, List<Course> courses) {
        HBox chartsRow = new HBox(20);

        if (monthlyEarnings != null && !monthlyEarnings.isEmpty()) {
            String[] labels = monthlyEarnings.keySet().toArray(new String[0]);
            double[] vals = monthlyEarnings.values().stream().mapToDouble(java.math.BigDecimal::doubleValue).toArray();
            VBox revenueChart = ChartFactory.createAreaChart("Monthly Earnings", vals, labels);
            HBox.setHgrow(revenueChart, Priority.ALWAYS);
            chartsRow.getChildren().add(revenueChart);
        }

        if (courses != null && !courses.isEmpty()) {
            var top = courses.stream()
                    .sorted((a, b) -> Integer.compare(b.getEnrollmentCount(), a.getEnrollmentCount()))
                    .limit(5)
                    .toList();
            String[] labels = top.stream()
                    .map(c -> {
                        String t = c.getTitle();
                        return t.length() > 12 ? t.substring(0, 12) + "…" : t;
                    })
                    .toArray(String[]::new);
            double[] vals = top.stream().mapToDouble(Course::getEnrollmentCount).toArray();
            VBox enrollChart = ChartFactory.createBarChart("Enrollment by Course", labels, vals);
            HBox.setHgrow(enrollChart, Priority.ALWAYS);
            chartsRow.getChildren().add(enrollChart);
        }

        if (!chartsRow.getChildren().isEmpty()) {
            contentArea.getChildren().add(chartsRow);
        }
    }

    private static String formatMoney(BigDecimal v) {
        if (v == null) return "$0.00";
        return "$" + v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private void buildCourseList(List<Course> courses) {
        coursesBox.getChildren().clear();
        if (courses == null || courses.isEmpty()) {
            coursesBox.getChildren().add(ComponentFactory.createEmptyState("📚", "You haven't created any courses yet"));
            return;
        }
        for (Course c : courses) {
            HBox row = new HBox(16);
            row.getStyleClass().add("card");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(16));
            Label title = new Label(c.getTitle());
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-pref-width: 300;");
            row.getChildren().addAll(
                title,
                ComponentFactory.createStatusBadge(c.getStatus().getValue(), c.getStatus().getValue()),
                new Label("$" + c.getPrice()) {{ setStyle("-fx-text-fill: #D44304; -fx-font-weight: bold;"); }},
                new Label("👥 " + c.getEnrollmentCount()) {{ getStyleClass().add("text-secondary"); }}
            );
            // Click to manage sections if approved
            if (c.getStatus() == CourseStatus.APPROVED) {
                row.setStyle(row.getStyle() + "-fx-cursor: hand;");
                row.setOnMouseClicked(e -> openCourseSections(c));
            }
            coursesBox.getChildren().add(row);
        }
    }

    private void loadSubScreen(String fxmlPath, String title) {
        try {
            pageTitle.setText(title);
            welcomeLabel.setText("");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            AnimationUtils.fadeIn(content, 300);
            contentScroll.setContent(content);
        } catch (Exception e) {
            System.err.println("Error loading: " + fxmlPath + " - " + e.getMessage());
        }
    }

    private void showDashboard() {
        pageTitle.setText("Instructor Dashboard");
        welcomeLabel.setText("Welcome back, " + SessionManager.getInstance().getCurrentUserName() + "!");
        contentScroll.setContent(dashboardContent);
    }

    @javafx.fxml.FXML private void handleNavDashboard() { showDashboard(); }
    @javafx.fxml.FXML private void handleNavCourses() {
        try {
            pageTitle.setText("My Courses");
            welcomeLabel.setText("Click a course to manage sections, lessons, and content");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/stla/views/instructor/instructor-courses.fxml"));
            Parent content = loader.load();
            InstructorCoursesController ctrl = loader.getController();
            ctrl.setOnManageCourse(this::openCourseSections);
            AnimationUtils.fadeIn(content, 300);
            contentScroll.setContent(content);
        } catch (Exception e) {
            System.err.println("Error loading instructor courses: " + e.getMessage());
        }
    }
    @javafx.fxml.FXML private void handleNavStudents() { loadSubScreen("/com/stla/views/instructor/enrolled-students.fxml", "Enrolled Students"); }
    @javafx.fxml.FXML private void handleNavWallet() {
        Object ctrl = loadSubScreenWithController("/com/stla/views/instructor/wallet.fxml", "Wallet & Earnings");
        if (ctrl instanceof WalletController wallet) {
            wallet.setOnOpenWithdraw(this::handleNavWithdraw);
        }
    }

    @javafx.fxml.FXML private void handleNavWithdraw() {
        loadSubScreen("/com/stla/views/instructor/instructor-withdraw.fxml", "Withdraw Earnings");
    }

    private Object loadSubScreenWithController(String fxmlPath, String title) {
        try {
            pageTitle.setText(title);
            welcomeLabel.setText("");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            AnimationUtils.fadeIn(content, 300);
            contentScroll.setContent(content);
            return loader.getController();
        } catch (Exception e) {
            System.err.println("Error loading: " + fxmlPath + " - " + e.getMessage());
            return null;
        }
    }
    @javafx.fxml.FXML private void handleNavProfile() { loadSubScreen("/com/stla/views/instructor/instructor-profile.fxml", "My Profile"); }
    @javafx.fxml.FXML private void handleNavNotifications() {
        loadSubScreen("/com/stla/views/instructor/instructor-notifications.fxml", "Notifications");
    }
    @javafx.fxml.FXML private void handleNavAddCourse() {
        Object ctrl = loadSubScreenWithController("/com/stla/views/instructor/add-course.fxml", "Create New Course");
        if (ctrl instanceof AddCourseController addCourse) {
            addCourse.setOnGoToMyCourses(this::handleNavCourses);
        }
    }
    @javafx.fxml.FXML private void handleLogout() { NavigationManager.getInstance().navigateToLogin(); }

    private void openCourseSections(Course course) {
        try {
            pageTitle.setText("Manage: " + course.getTitle());
            welcomeLabel.setText("");
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/stla/views/instructor/course-player.fxml"));
            javafx.scene.Parent content = loader.load();
            CoursePlayerController ctrl = loader.getController();
            ctrl.setCourse(course);
            AnimationUtils.fadeIn(content, 300);
            contentScroll.setContent(content);
        } catch (Exception e) {
            System.err.println("Error loading course player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildVerificationBanner() {
        com.stla.domain.models.Instructor inst = SessionManager.getInstance().getCurrentInstructor();
        if (inst == null) return;
        String status = inst.getVerificationStatus() != null ? inst.getVerificationStatus() : "PENDING";

        if (verificationBanner == null) {
            verificationBanner = new javafx.scene.layout.VBox(4);
            verificationBanner.setPadding(new javafx.geometry.Insets(12, 16, 12, 16));
            verificationBanner.getStyleClass().add("card");
            contentArea.getChildren().add(0, verificationBanner);
        }
        verificationBanner.getChildren().clear();

        switch (status.toUpperCase()) {
            case "VERIFIED" -> {
                verificationBanner.setStyle("-fx-background-color: #ECFDF5; -fx-border-color: #10B981; -fx-border-width: 0 0 0 4; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 12 16;");
                verificationBanner.getChildren().addAll(
                    new javafx.scene.control.Label("✅ Your account is verified!") {{ setStyle("-fx-font-weight: bold; -fx-text-fill: #059669;"); }},
                    new javafx.scene.control.Label("You can create and publish courses.") {{ setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;"); }}
                );
            }
            case "REJECTED" -> {
                verificationBanner.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #EF4444; -fx-border-width: 0 0 0 4; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 12 16;");
                verificationBanner.getChildren().add(
                    new javafx.scene.control.Label("❌ Your verification was rejected.") {{ setStyle("-fx-font-weight: bold; -fx-text-fill: #DC2626;"); }}
                );
                if (inst.getRejectionReason() != null && !inst.getRejectionReason().isBlank()) {
                    verificationBanner.getChildren().add(
                        new javafx.scene.control.Label("Reason: " + inst.getRejectionReason()) {{ setStyle("-fx-font-size: 12px; -fx-text-fill: #991B1B;"); setWrapText(true); }}
                    );
                }
                javafx.scene.control.Button resubmit = new javafx.scene.control.Button("📤 Go to Profile to Resubmit");
                resubmit.getStyleClass().addAll("btn-outline", "btn-sm");
                resubmit.setOnAction(e -> handleNavProfile());
                verificationBanner.getChildren().add(resubmit);
            }
            default -> { // PENDING
                verificationBanner.setStyle("-fx-background-color: #FFFBEB; -fx-border-color: #F59E0B; -fx-border-width: 0 0 0 4; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 12 16;");
                verificationBanner.getChildren().addAll(
                    new javafx.scene.control.Label("⏳ Verification Pending") {{ setStyle("-fx-font-weight: bold; -fx-text-fill: #D97706;"); }},
                    new javafx.scene.control.Label("Your documents are under review. You'll be notified once approved.") {{ setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;"); }}
                );
            }
        }
    }
}
