package com.stla.core.navigation;

import com.stla.core.session.SessionManager;
import com.stla.domain.enums.AppRole;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Manages scene navigation and view loading.
 * Handles sidebar-based navigation for role dashboards.
 */
public class NavigationManager {

    private static NavigationManager instance;
    private Stage primaryStage;
    private BorderPane mainLayout;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) instance = new NavigationManager();
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setMainLayout(BorderPane layout) {
        this.mainLayout = layout;
    }

    /**
     * Navigate to a full-screen view (e.g., login).
     */
    public void navigateTo(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("FXML not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Add CSS stylesheets
            addStylesheets(scene);

            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load a view into the center of the main dashboard layout.
     */
    public void loadContent(String fxmlPath) {
        if (mainLayout == null) {
            System.err.println("Main layout not set. Cannot load content.");
            return;
        }
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("FXML not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent content = loader.load();
            mainLayout.setCenter(content);
        } catch (IOException e) {
            System.err.println("Error loading content: " + fxmlPath + " - " + e.getMessage());
        }
    }

    /**
     * Navigate to the appropriate dashboard based on user role.
     */
    public void navigateToDashboard() {
        AppRole role = SessionManager.getInstance().getCurrentRole();
        if (role == null) {
            navigateTo("/com/stla/views/auth/login.fxml");
            return;
        }
        String fxml = switch (role) {
            case STUDENT -> "/com/stla/views/student/student-dashboard.fxml";
            case INSTRUCTOR -> "/com/stla/views/instructor/instructor-dashboard.fxml";
            case ADMIN -> "/com/stla/views/admin/admin-dashboard.fxml";
        };
        navigateTo(fxml);
    }

    /**
     * Navigate back to login screen.
     */
    public void navigateToLogin() {
        SessionManager.getInstance().logout();
        navigateTo("/com/stla/views/auth/login.fxml");
    }

    private void addStylesheets(Scene scene) {
        String[] cssFiles = {
            "/com/stla/css/app.css",
            "/com/stla/css/buttons.css",
            "/com/stla/css/tables.css",
            "/com/stla/css/cards.css",
            "/com/stla/css/forms.css",
            "/com/stla/css/dashboard.css",
            "/com/stla/css/auth.css",
            "/com/stla/css/animations.css",
            "/com/stla/css/charts.css",
            "/com/stla/css/player.css",
            "/com/stla/css/sidebar.css",
            "/com/stla/css/upload.css",
            "/com/stla/css/checkout.css",
            "/com/stla/css/payment.css",
            "/com/stla/css/wallet.css",
            "/com/stla/css/catalog.css",
            "/com/stla/css/quiz.css",
            "/com/stla/css/progress.css",
            "/com/stla/css/notifications.css",
            "/com/stla/css/reviews.css",
            "/com/stla/css/theme.css"
        };
        for (String css : cssFiles) {
            URL resource = getClass().getResource(css);
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
            }
        }
        // Register with ThemeManager for dark mode support
        com.stla.ui.components.ThemeManager.getInstance().setScene(scene);
    }
}
