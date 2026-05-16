package com.stla.ui.admin;

import com.stla.core.navigation.NavigationManager;
import com.stla.core.session.SessionManager;
import com.stla.domain.models.ChartSeries;
import com.stla.domain.models.Profile;
import com.stla.services.DashboardService;
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
import java.math.RoundingMode;
import java.util.List;

public class AdminDashboardController {

    @javafx.fxml.FXML private BorderPane rootPane;
    @javafx.fxml.FXML private Label userNameLabel;
    @javafx.fxml.FXML private Label pageTitle;
    @javafx.fxml.FXML private Label welcomeLabel;
    @javafx.fxml.FXML private HBox statsRow;
    @javafx.fxml.FXML private VBox recentUsersBox;
    @javafx.fxml.FXML private VBox contentArea;
    @javafx.fxml.FXML private ScrollPane contentScroll;
    @javafx.fxml.FXML private StackPane notificationBellHost;
    @javafx.fxml.FXML private StackPane sidebarAvatarContainer;
    @javafx.fxml.FXML private StackPane headerAvatarContainer;

    private final DashboardService service = new DashboardService();
    private VBox dashboardContent;
    private CircularAvatar sidebarAvatar, headerAvatar;

    @javafx.fxml.FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        userNameLabel.setText(session.getCurrentUserName());
        dashboardContent = contentArea;
        NotificationBellHelper.install(notificationBellHost, this::handleNavNotifications);
        setupAvatars(session);
        loadData();
    }

    private void setupAvatars(SessionManager session) {
        String url = session.getCurrentUserAvatar();
        String name = session.getCurrentUserName();
        sidebarAvatar = new CircularAvatar(url, name, 18);
        headerAvatar = new CircularAvatar(url, name, 18);
        sidebarAvatarContainer.getChildren().setAll(sidebarAvatar);
        headerAvatarContainer.getChildren().setAll(headerAvatar);
        wireProfileNavigation();
        session.addProfileChangeListener(() -> {
            String u = session.getCurrentUserAvatar();
            String n = session.getCurrentUserName();
            userNameLabel.setText(n);
            sidebarAvatar.setAvatar(u, n);
            headerAvatar.setAvatar(u, n);
        });
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
            int totalUsers, totalStudents, totalInstructors, totalAdmins, totalCourses, paidPayments;
            BigDecimal revenue;
            List<Profile> users;
            ChartSeries enrollmentsByMonth;
            ChartSeries monthlyRevenue;
            @Override protected Void call() {
                totalUsers = service.getTotalUsers();
                totalStudents = service.getTotalStudents();
                totalInstructors = service.getTotalInstructors();
                totalAdmins = service.getTotalAdmins();
                totalCourses = service.getTotalCourses();
                paidPayments = service.getPaidPaymentsCount();
                revenue = service.getPlatformRevenue();
                users = service.getRecentUsers(8);
                enrollmentsByMonth = service.getEnrollmentsByMonth(6);
                monthlyRevenue = service.getMonthlyRevenue(6);
                return null;
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    buildStats(totalUsers, totalStudents, totalInstructors, totalCourses, paidPayments, revenue);
                    buildRecentUsers(users);
                    addCharts(totalStudents, totalInstructors, totalAdmins, enrollmentsByMonth, monthlyRevenue);
                    AnimationUtils.staggerFadeIn(contentArea, 80);
                });
            }
        };
        new Thread(task).start();
    }

    private void buildStats(int users, int students, int instructors, int courses, int paidPayments, BigDecimal revenue) {
        statsRow.getChildren().clear();
        statsRow.getChildren().addAll(
            ComponentFactory.createStatCard("👥", "Total Users", String.valueOf(users), false),
            ComponentFactory.createStatCard("🎓", "Students", String.valueOf(students), false),
            ComponentFactory.createStatCard("👨‍🏫", "Instructors", String.valueOf(instructors), false),
            ComponentFactory.createStatCard("📚", "Courses", String.valueOf(courses), false),
            ComponentFactory.createStatCard("💳", "Paid Orders", String.valueOf(paidPayments), false),
            ComponentFactory.createStatCard("💰", "Gross Revenue", formatMoney(revenue), false)
        );
        AnimationUtils.staggerFadeIn(statsRow, 100);
    }

    private void addCharts(int students, int instructors, int admins,
                           ChartSeries enrollments, ChartSeries revenue) {
        HBox chartsRow = new HBox(20);

        if (enrollments != null && !enrollments.isEmpty()) {
            chartsRow.getChildren().add(ChartFactory.createBarChart(
                    "Enrollments by Month", enrollments.getLabels(), enrollments.getValues()));
        } else {
            chartsRow.getChildren().add(emptyChart("Enrollments by Month", "No enrollment data yet"));
        }

        double adminCount = Math.max(admins, 0);
        if (students + instructors + adminCount > 0) {
            chartsRow.getChildren().add(ChartFactory.createPieChart(
                    "User Distribution",
                    new String[]{"Students", "Instructors", "Admins"},
                    new double[]{students, instructors, adminCount}));
        } else {
            chartsRow.getChildren().add(emptyChart("User Distribution", "No users yet"));
        }

        for (var node : chartsRow.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        if (revenue != null && !revenue.isEmpty()) {
            contentArea.getChildren().addAll(chartsRow,
                    ChartFactory.createRevenueChart("Revenue Trend", revenue.getValues(), revenue.getLabels()));
        } else {
            contentArea.getChildren().addAll(chartsRow, emptyChart("Revenue Trend", "No payment data yet"));
        }
    }

    private VBox emptyChart(String title, String message) {
        VBox box = new VBox(8);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
                new Label(title) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); }},
                new Label(message) {{ getStyleClass().add("text-secondary"); }}
        );
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private void buildRecentUsers(List<Profile> users) {
        recentUsersBox.getChildren().clear();
        if (users == null || users.isEmpty()) {
            recentUsersBox.getChildren().add(ComponentFactory.createEmptyState("👥", "No users found"));
            return;
        }
        for (Profile p : users) {
            HBox row = new HBox(16);
            row.getStyleClass().add("card");
            row.setStyle("-fx-padding: 12 16;");
            row.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(p.getFullName());
            name.setStyle("-fx-font-weight: bold; -fx-pref-width: 200;");
            Label email = new Label(p.getEmail());
            email.getStyleClass().add("text-secondary");
            email.setStyle("-fx-pref-width: 250;");
            row.getChildren().addAll(
                name, email,
                ComponentFactory.createStatusBadge(p.getRole().getValue(), p.getRole().getValue()),
                ComponentFactory.createStatusBadge(p.isActive() ? "Active" : "Inactive", p.isActive() ? "active" : "danger")
            );
            recentUsersBox.getChildren().add(row);
        }
    }

    private static String formatMoney(BigDecimal v) {
        if (v == null) return "$0.00";
        return "$" + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
        pageTitle.setText("Admin Dashboard");
        welcomeLabel.setText("Platform overview and management");
        contentScroll.setContent(dashboardContent);
    }

    @javafx.fxml.FXML private void handleNavDashboard() { showDashboard(); }
    @javafx.fxml.FXML private void handleNavUsers() { loadSubScreen("/com/stla/views/admin/users-management.fxml", "Users Management"); }
    @javafx.fxml.FXML private void handleNavCourses() { loadSubScreen("/com/stla/views/admin/courses-management.fxml", "Courses Management"); }
    @javafx.fxml.FXML private void handleNavCategories() { loadSubScreen("/com/stla/views/admin/categories-management.fxml", "Categories"); }
    @javafx.fxml.FXML private void handleNavVerifications() { loadSubScreen("/com/stla/views/admin/instructor-verification.fxml", "Instructor Verifications"); }
    @javafx.fxml.FXML private void handleNavPayments() { loadSubScreen("/com/stla/views/admin/payments-management.fxml", "Payments"); }
    @javafx.fxml.FXML private void handleNavAdminWallet() { loadSubScreen("/com/stla/views/admin/admin-wallet.fxml", "Admin Wallet"); }
    @javafx.fxml.FXML private void handleNavWithdrawals() { loadSubScreen("/com/stla/views/admin/withdrawals.fxml", "Withdrawal History"); }
    @javafx.fxml.FXML private void handleNavCourseReview() { loadSubScreen("/com/stla/views/admin/course-review.fxml", "Course Reviews"); }
    @javafx.fxml.FXML private void handleNavNotifications() {
        loadSubScreen("/com/stla/views/admin/admin-notifications.fxml", "Notifications");
    }
    @javafx.fxml.FXML private void handleNavProfile() {
        loadSubScreen("/com/stla/views/admin/admin-profile.fxml", "My Profile");
    }
    @javafx.fxml.FXML private void handleToggleDarkMode() { ThemeManager.getInstance().toggleTheme(); }
    @javafx.fxml.FXML private void handleLogout() { NavigationManager.getInstance().navigateToLogin(); }
}
