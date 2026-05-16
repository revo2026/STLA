package com.stla.ui.student;

import com.stla.core.session.SessionManager;
import com.stla.domain.enums.AppRole;
import com.stla.domain.models.Profile;
import com.stla.domain.models.Student;
import com.stla.services.ProfileService;
import com.stla.ui.components.AnimationUtils;
import com.stla.ui.components.CircularAvatar;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Arrays;

/**
 * Student profile controller with view/edit modes, circular avatar, Supabase upload.
 */
public class StudentProfileController {

    // View mode
    @FXML private VBox viewPane;
    @FXML private StackPane avatarContainer;
    @FXML private Label nameLabel, emailLabel, roleLabel;
    @FXML private Label headlineLabel, bioLabel, countryLabel, phoneLabel, goalsLabel, interestsLabel;
    @FXML private Label skillLabel, langLabel, dailyGoalLabel;
    @FXML private Button editBtn;

    // Edit mode
    @FXML private VBox editPane;
    @FXML private StackPane editAvatarContainer;
    @FXML private TextField editNameField, editPhoneField, editCountryField, editHeadlineField;
    @FXML private TextField editInterestsField, editGoalsField;
    @FXML private TextArea editBioField;
    @FXML private ComboBox<String> editSkillCombo, editLangCombo, editGoalMinCombo;
    @FXML private Label editError;
    @FXML private Button saveBtn;
    @FXML private ProgressIndicator saveLoading;

    private final ProfileService profileService = new ProfileService();
    private CircularAvatar viewAvatar, editAvatar;
    private File pendingAvatarFile = null;

    @FXML
    public void initialize() {
        // Setup combo boxes
        editSkillCombo.setItems(FXCollections.observableArrayList("beginner", "intermediate", "advanced"));
        editLangCombo.setItems(FXCollections.observableArrayList("english", "arabic", "french", "german", "spanish", "japanese", "chinese"));
        editGoalMinCombo.setItems(FXCollections.observableArrayList("15", "30", "45", "60", "90", "120"));

        populateView();
    }

    private void populateView() {
        SessionManager session = SessionManager.getInstance();
        Profile p = session.getCurrentProfile();
        Student s = session.getCurrentStudent();

        // Avatar
        viewAvatar = new CircularAvatar(p.getAvatarUrl(), p.getFullName(), 40);
        avatarContainer.getChildren().setAll(viewAvatar);

        // Labels
        nameLabel.setText(p.getFullName());
        emailLabel.setText(p.getEmail());
        roleLabel.setText("STUDENT");
        headlineLabel.setText(orDash(s.getHeadline()));
        bioLabel.setText(orDash(p.getBio()));
        countryLabel.setText(orDash(p.getCountry()));
        phoneLabel.setText(orDash(p.getPhone()));
        goalsLabel.setText(orDash(s.getLearningGoals()));
        interestsLabel.setText(s.getInterests() != null ? String.join(", ", s.getInterests()) : "—");
        skillLabel.setText(orDash(s.getSkillLevel()));
        langLabel.setText(orDash(s.getPreferredLanguage()));
        dailyGoalLabel.setText(s.getDailyGoalMinutes() != null ? s.getDailyGoalMinutes() + " min" : "—");
    }

    // ======================== EDIT MODE ========================

    @FXML
    private void toggleEdit() {
        SessionManager session = SessionManager.getInstance();
        Profile p = session.getCurrentProfile();
        Student s = session.getCurrentStudent();

        // Populate edit fields
        editNameField.setText(p.getFullName());
        editPhoneField.setText(p.getPhone() != null ? p.getPhone() : "");
        editCountryField.setText(p.getCountry() != null ? p.getCountry() : "");
        editHeadlineField.setText(s.getHeadline() != null ? s.getHeadline() : "");
        editBioField.setText(p.getBio() != null ? p.getBio() : "");
        editInterestsField.setText(s.getInterests() != null ? String.join(", ", s.getInterests()) : "");
        editGoalsField.setText(s.getLearningGoals() != null ? s.getLearningGoals() : "");
        editSkillCombo.setValue(s.getSkillLevel() != null ? s.getSkillLevel() : "beginner");
        editLangCombo.setValue(s.getPreferredLanguage() != null ? s.getPreferredLanguage() : "english");
        editGoalMinCombo.setValue(s.getDailyGoalMinutes() != null ? String.valueOf(s.getDailyGoalMinutes()) : "30");

        // Avatar
        editAvatar = new CircularAvatar(p.getAvatarUrl(), p.getFullName(), 40);
        editAvatarContainer.getChildren().setAll(editAvatar);
        pendingAvatarFile = null;

        // Toggle visibility
        viewPane.setVisible(false); viewPane.setManaged(false);
        editPane.setVisible(true); editPane.setManaged(true);
        AnimationUtils.fadeIn(editPane, 250);
    }

    @FXML
    private void cancelEdit() {
        editPane.setVisible(false); editPane.setManaged(false);
        viewPane.setVisible(true); viewPane.setManaged(true);
        pendingAvatarFile = null;
        AnimationUtils.fadeIn(viewPane, 250);
    }

    @FXML
    private void handleChangeAvatar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile Photo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = fc.showOpenDialog(editPane.getScene().getWindow());
        if (file != null) {
            // Validate size
            if (file.length() > 5 * 1024 * 1024) {
                showEditError("Image too large. Maximum 5MB.");
                return;
            }
            pendingAvatarFile = file;
            editAvatar.setAvatar(file.toURI().toString(), editNameField.getText());
            AnimationUtils.scaleBounce(editAvatarContainer);
        }
    }

    @FXML
    private void saveProfile() {
        String name = editNameField.getText().trim();
        if (name.isEmpty()) { showEditError("Full name is required."); return; }

        setLoading(true);
        hideEditError();

        Task<ProfileService.Result> task = new Task<>() {
            @Override
            protected ProfileService.Result call() {
                SessionManager session = SessionManager.getInstance();
                Profile p = session.getCurrentProfile();
                Student s = session.getCurrentStudent();

                // Upload avatar if changed
                if (pendingAvatarFile != null) {
                    String newUrl = profileService.updateAvatar(pendingAvatarFile, AppRole.STUDENT);
                    if (newUrl != null) p.setAvatarUrl(newUrl);
                }

                // Update profile fields
                p.setFullName(name);
                p.setPhone(editPhoneField.getText().trim());
                p.setCountry(editCountryField.getText().trim());
                p.setBio(editBioField.getText().trim());

                // Update student fields
                s.setHeadline(editHeadlineField.getText().trim());
                String interests = editInterestsField.getText().trim();
                s.setInterests(interests.isEmpty() ? new String[0] : interests.split(",\\s*"));
                s.setLearningGoals(editGoalsField.getText().trim());
                s.setSkillLevel(editSkillCombo.getValue());
                s.setPreferredLanguage(editLangCombo.getValue());
                try { s.setDailyGoalMinutes(Integer.parseInt(editGoalMinCombo.getValue())); }
                catch (Exception ex) { s.setDailyGoalMinutes(30); }

                return profileService.updateStudentProfile(p, s);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            ProfileService.Result result = task.getValue();
            if (result.success()) {
                pendingAvatarFile = null;
                cancelEdit();
                populateView();
                ToastNotification.show((javafx.stage.Stage) editPane.getScene().getWindow(), "Profile updated successfully!", ToastNotification.Type.SUCCESS);
            } else {
                showEditError(result.message());
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showEditError("Update failed. Please try again.");
        });

        new Thread(task).start();
    }

    // ======================== HELPERS ========================

    private String orDash(String val) { return val != null && !val.isBlank() ? val : "—"; }

    private void showEditError(String msg) {
        Platform.runLater(() -> { editError.setText(msg); editError.setVisible(true); editError.setManaged(true); });
    }
    private void hideEditError() {
        Platform.runLater(() -> { editError.setVisible(false); editError.setManaged(false); });
    }
    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            saveBtn.setDisable(loading);
            saveLoading.setVisible(loading); saveLoading.setManaged(loading);
            saveBtn.setText(loading ? "Saving..." : "💾  Save Changes");
        });
    }
}
