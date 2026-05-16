package com.stla.ui.admin;

import com.stla.core.navigation.NavigationManager;
import com.stla.core.session.SessionManager;
import com.stla.domain.enums.AppRole;
import com.stla.domain.models.Profile;
import com.stla.services.ProfileService;
import com.stla.ui.components.CircularAvatar;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;

public class AdminProfileController {

    @FXML private StackPane avatarContainer;
    @FXML private Label nameLabel, emailLabel, memberSinceLabel;
    @FXML private TextField editNameField, editPhoneField, editCountryField, editTimezoneField;
    @FXML private TextArea editBioField;
    @FXML private Button saveBtn;
    @FXML private ProgressIndicator saveLoading;

    private final ProfileService profileService = new ProfileService();
    private CircularAvatar avatar;

    @FXML
    public void initialize() {
        loadProfile();
    }

    private void loadProfile() {
        Profile profile = SessionManager.getInstance().getCurrentProfile();
        if (profile == null) return;

        nameLabel.setText(profile.getFullName());
        emailLabel.setText(profile.getEmail());
        memberSinceLabel.setText("Member since "
                + (profile.getCreatedAt() != null ? profile.getCreatedAt().toLocalDate() : "N/A"));

        avatar = new CircularAvatar(profile.getAvatarUrl(), profile.getFullName(), 36);
        avatarContainer.getChildren().setAll(avatar);

        editNameField.setText(profile.getFullName());
        editPhoneField.setText(profile.getPhone() != null ? profile.getPhone() : "");
        editCountryField.setText(profile.getCountry() != null ? profile.getCountry() : "");
        editTimezoneField.setText(profile.getTimezone() != null ? profile.getTimezone() : "");
        editBioField.setText(profile.getBio() != null ? profile.getBio() : "");
    }

    @FXML
    private void handleAvatarChange() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = fc.showOpenDialog(avatarContainer.getScene().getWindow());
        if (file == null) return;

        saveLoading.setVisible(true);
        saveLoading.setManaged(true);
        saveBtn.setDisable(true);

        Task<String> task = new Task<>() {
            @Override protected String call() {
                return profileService.updateAvatar(file, AppRole.ADMIN);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            saveLoading.setVisible(false);
            saveLoading.setManaged(false);
            saveBtn.setDisable(false);
            String url = task.getValue();
            if (url != null) {
                avatar.setAvatar(url, editNameField.getText());
                ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(),
                        "Avatar updated!", ToastNotification.Type.SUCCESS);
            } else {
                ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(),
                        "Avatar upload failed.", ToastNotification.Type.ERROR);
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            saveLoading.setVisible(false);
            saveLoading.setManaged(false);
            saveBtn.setDisable(false);
        }));
        new Thread(task).start();
    }

    @FXML
    private void handleSave() {
        Profile profile = SessionManager.getInstance().getCurrentProfile();
        if (profile == null) return;

        profile.setFullName(editNameField.getText().trim());
        profile.setPhone(editPhoneField.getText().trim());
        profile.setCountry(editCountryField.getText().trim());
        profile.setTimezone(editTimezoneField.getText().trim());
        profile.setBio(editBioField.getText().trim());

        saveLoading.setVisible(true);
        saveLoading.setManaged(true);
        saveBtn.setDisable(true);

        Task<ProfileService.Result> task = new Task<>() {
            @Override protected ProfileService.Result call() {
                return profileService.updateAdminProfile(profile);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            saveLoading.setVisible(false);
            saveLoading.setManaged(false);
            saveBtn.setDisable(false);
            ProfileService.Result result = task.getValue();
            if (result != null && result.success()) {
                loadProfile();
                ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(),
                        result.message(), ToastNotification.Type.SUCCESS);
            } else {
                ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(),
                        result != null ? result.message() : "Update failed",
                        ToastNotification.Type.ERROR);
            }
        }));
        new Thread(task).start();
    }
}
