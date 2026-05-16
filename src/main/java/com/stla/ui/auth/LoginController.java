package com.stla.ui.auth;

import com.stla.core.navigation.NavigationManager;
import com.stla.domain.enums.AppRole;
import com.stla.services.AuthService;
import com.stla.ui.components.AnimationUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Enhanced Login controller with role selection, validation, animations.
 */
public class LoginController {

    @FXML private VBox loginCard;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private FlowPane roleSelector;
    @FXML private Region brandPanel;
    @FXML private HBox authSplit;
    @FXML private Button roleStudentBtn;
    @FXML private Button roleInstructorBtn;
    @FXML private Button roleAdminBtn;
    @FXML private Button registerLink;

    private final AuthService authService = new AuthService();
    private AppRole selectedRole = AppRole.STUDENT;

    @FXML
    public void initialize() {
        passwordField.setOnAction(e -> handleLogin());
        emailField.setOnAction(e -> passwordField.requestFocus());

        Platform.runLater(() -> {
            AnimationUtils.fadeInUp(loginCard, 400);
            AuthLayoutHelper.bindResponsive(loginCard, brandPanel, loginCard);
            bindRoleWrap();
        });
    }

    private void bindRoleWrap() {
        if (roleSelector == null) return;
        Runnable update = () -> {
            double w = loginCard.getWidth() > 0 ? loginCard.getWidth() - 48 : 420;
            roleSelector.setPrefWrapLength(Math.max(280, w));
        };
        loginCard.widthProperty().addListener((o, ov, nv) -> update.run());
        Platform.runLater(update);
    }

    // --- Role Selection ---
    @FXML private void handleSelectStudent() { selectRole(AppRole.STUDENT); }
    @FXML private void handleSelectInstructor() { selectRole(AppRole.INSTRUCTOR); }
    @FXML private void handleSelectAdmin() { selectRole(AppRole.ADMIN); }

    private void selectRole(AppRole role) {
        selectedRole = role;
        // Reset all buttons
        roleStudentBtn.getStyleClass().remove("role-btn-active");
        roleInstructorBtn.getStyleClass().remove("role-btn-active");
        roleAdminBtn.getStyleClass().remove("role-btn-active");

        // Activate selected
        Button active = switch (role) {
            case STUDENT -> roleStudentBtn;
            case INSTRUCTOR -> roleInstructorBtn;
            case ADMIN -> roleAdminBtn;
        };
        active.getStyleClass().add("role-btn-active");
        AnimationUtils.scaleBounce(active);
        hideError();
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        setLoading(true);
        hideError();

        Task<AuthService.AuthResult> loginTask = new Task<>() {
            @Override
            protected AuthService.AuthResult call() {
                return authService.login(email, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            setLoading(false);
            AuthService.AuthResult result = loginTask.getValue();
            if (result.isSuccess()) {
                // Verify role matches selection
                AppRole actualRole = result.getProfile().getRole();
                if (actualRole != selectedRole) {
                    showError("Selected role does not match account role. Your role is: " 
                              + actualRole.getValue().toUpperCase());
                    return;
                }
                NavigationManager.getInstance().navigateToDashboard();
            } else {
                showError(result.getMessage());
            }
        });

        loginTask.setOnFailed(event -> {
            setLoading(false);
            showError("Connection error. Please try again.");
            loginTask.getException().printStackTrace();
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleGoToRegister() {
        NavigationManager.getInstance().navigateTo("/com/stla/views/auth/register.fxml");
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            emailField.getStyleClass().remove("error");
            passwordField.getStyleClass().remove("error");
            emailField.getStyleClass().add("error");
            passwordField.getStyleClass().add("error");
            AnimationUtils.shake(loginCard);
        });
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        emailField.getStyleClass().remove("error");
        passwordField.getStyleClass().remove("error");
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            loginButton.setDisable(loading);
            loadingIndicator.setVisible(loading);
            loadingIndicator.setManaged(loading);
            loginButton.setText(loading ? "Signing in..." : "Sign In");
        });
    }
}
