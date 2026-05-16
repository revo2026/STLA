package com.stla.app;

import com.stla.core.database.DatabaseConnection;
import com.stla.core.navigation.NavigationManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

/**
 * STLA Desktop Application entry point.
 * Initializes the database connection and launches the login screen.
 */
public class StlaApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load application config
            AppConfig config = AppConfig.getInstance();

            // Initialize navigation manager
            NavigationManager navManager = NavigationManager.getInstance();
            navManager.setPrimaryStage(primaryStage);

            // Configure primary stage
            primaryStage.setTitle(config.getAppTitle());
            primaryStage.setWidth(config.getAppWidth());
            primaryStage.setHeight(config.getAppHeight());
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(700);

            // Test database connection (non-blocking — app still launches)
            System.out.println("=== STLA Desktop Application ===");
            System.out.println("JDBC URL: " + config.getDbUrl());
            System.out.println("Connecting to Supabase PostgreSQL...");
            try {
                boolean dbConnected = DatabaseConnection.getInstance().testConnection();
                System.out.println("Database connection: " + (dbConnected ? "✅ SUCCESS" : "❌ FAILED"));
                if (dbConnected) {
                    com.stla.patterns.observer.NotificationObserver.register();
                }
            } catch (Exception dbEx) {
                System.err.println("⚠ Database unavailable: " + dbEx.getMessage());
                System.err.println("⚠ App will launch but data features won't work.");
            }

            // Load login screen
            URL loginFxml = getClass().getResource("/com/stla/views/auth/login.fxml");
            if (loginFxml == null) {
                System.err.println("❌ login.fxml not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(loginFxml);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Add all stylesheets
            addStylesheets(scene);

            primaryStage.setScene(scene);
            primaryStage.show();

            System.out.println("✅ Application started successfully!");

        } catch (Exception e) {
            System.err.println("❌ Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Cleanup: close DB connection pool
        System.out.println("Shutting down STLA Desktop...");
        DatabaseConnection.getInstance().shutdown();
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
            "/com/stla/css/theme.css"
        };
        for (String css : cssFiles) {
            URL resource = getClass().getResource(css);
            if (resource != null) {
                scene.getStylesheets().add(resource.toExternalForm());
            } else {
                System.err.println("⚠ CSS not found: " + css);
            }
        }

        // Initialize theme manager with this scene
        com.stla.ui.components.ThemeManager.getInstance().setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
