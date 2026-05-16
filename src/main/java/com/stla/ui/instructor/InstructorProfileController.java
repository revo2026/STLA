package com.stla.ui.instructor;

import com.stla.core.navigation.NavigationManager;
import com.stla.core.session.SessionManager;
import com.stla.domain.models.Instructor;
import com.stla.domain.models.Profile;
import com.stla.services.InstructorService;
import com.stla.services.ProfileService;
import com.stla.services.SupabaseStorageService;
import com.stla.ui.components.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class InstructorProfileController {

    @FXML private VBox verificationCard;
    @FXML private Label verifyIcon, verifyTitle, verifyMessage, verifyBadge, rejectionLabel;
    @FXML private StackPane avatarContainer;
    @FXML private Label nameLabel, emailLabel, memberSinceLabel;
    @FXML private HBox statsRow;

    @FXML private TextField editNameField, editPhoneField, editCountryField;
    @FXML private TextArea editBioField;
    @FXML private TextField editTitleField, editYearsField, editExpertiseField;
    @FXML private TextArea editInstructorBioField;

    @FXML private Label docIdFrontLabel, docIdBackLabel, docCertLabel, docCvLabel;
    @FXML private Button resubmitBtn, saveBtn;
    @FXML private ProgressIndicator saveLoading;

    private final InstructorService instructorService = new InstructorService();
    private final ProfileService profileService = new ProfileService();
    private CircularAvatar avatar;

    @FXML
    public void initialize() {
        loadProfile();
    }

    private void loadProfile() {
        SessionManager session = SessionManager.getInstance();
        Profile profile = session.getCurrentProfile();
        Instructor instructor = session.getCurrentInstructor();

        if (profile == null || instructor == null) return;

        // Header
        nameLabel.setText(profile.getFullName());
        emailLabel.setText(profile.getEmail());
        memberSinceLabel.setText("Member since " + (profile.getCreatedAt() != null ? profile.getCreatedAt().toLocalDate().toString() : "N/A"));

        // Avatar
        avatar = new CircularAvatar(profile.getAvatarUrl(), profile.getFullName(), 36);
        avatarContainer.getChildren().setAll(avatar);

        // Stats
        statsRow.getChildren().clear();
        statsRow.getChildren().addAll(
            ComponentFactory.createStatCard("📚", "Courses", String.valueOf(instructor.getTotalCourses()), false),
            ComponentFactory.createStatCard("👥", "Students", String.valueOf(instructor.getTotalStudents()), false),
            ComponentFactory.createStatCard("⭐", "Rating", instructor.getRatingAvg() != null ? instructor.getRatingAvg().toString() : "0.0", false)
        );

        // Editable fields
        editNameField.setText(profile.getFullName());
        editPhoneField.setText(profile.getPhone() != null ? profile.getPhone() : "");
        editCountryField.setText(profile.getCountry() != null ? profile.getCountry() : "");
        editBioField.setText(profile.getBio() != null ? profile.getBio() : "");

        editTitleField.setText(instructor.getTitle() != null ? instructor.getTitle() : "");
        editYearsField.setText(instructor.getYearsExperience() != null ? String.valueOf(instructor.getYearsExperience()) : "");
        editExpertiseField.setText(instructor.getExpertiseTags() != null ? String.join(", ", instructor.getExpertiseTags()) : "");
        editInstructorBioField.setText(instructor.getInstructorBio() != null ? instructor.getInstructorBio() : "");

        // Verification status
        updateVerificationUI(instructor);

        // Document status
        docIdFrontLabel.setText(instructor.getIdFrontUrl() != null ? "✅ Uploaded" : "❌ Not uploaded");
        docIdBackLabel.setText(instructor.getIdBackUrl() != null ? "✅ Uploaded" : "❌ Not uploaded");
        docCertLabel.setText(instructor.getExperienceCertificateUrl() != null ? "✅ Uploaded" : "❌ Not uploaded");
        docCvLabel.setText(instructor.getCvUrl() != null ? "✅ Uploaded" : "❌ Not uploaded");

        AnimationUtils.fadeIn(verificationCard, 300);
    }

    private void updateVerificationUI(Instructor instructor) {
        String status = instructor.getVerificationStatus() != null ? instructor.getVerificationStatus() : "PENDING";
        switch (status.toUpperCase()) {
            case "VERIFIED" -> {
                verifyIcon.setText("✅");
                verifyTitle.setText("Account Verified");
                verifyMessage.setText("Your instructor account is verified. You can create and publish courses.");
                verifyBadge.setText("VERIFIED");
                verifyBadge.getStyleClass().removeAll("badge-warning", "badge-danger", "badge-success");
                verifyBadge.getStyleClass().add("badge-success");
                verificationCard.setStyle("-fx-padding: 16 20; -fx-border-color: #10B981; -fx-border-width: 0 0 0 4; -fx-border-radius: 8;");
                rejectionLabel.setVisible(false); rejectionLabel.setManaged(false);
                resubmitBtn.setVisible(false); resubmitBtn.setManaged(false);
            }
            case "REJECTED" -> {
                verifyIcon.setText("❌");
                verifyTitle.setText("Verification Rejected");
                verifyMessage.setText("Your verification was rejected. Please review the reason below and resubmit.");
                verifyBadge.setText("REJECTED");
                verifyBadge.getStyleClass().removeAll("badge-warning", "badge-danger", "badge-success");
                verifyBadge.getStyleClass().add("badge-danger");
                verificationCard.setStyle("-fx-padding: 16 20; -fx-border-color: #EF4444; -fx-border-width: 0 0 0 4; -fx-border-radius: 8;");
                if (instructor.getRejectionReason() != null && !instructor.getRejectionReason().isBlank()) {
                    rejectionLabel.setText("Reason: " + instructor.getRejectionReason());
                    rejectionLabel.setVisible(true); rejectionLabel.setManaged(true);
                }
                resubmitBtn.setVisible(true); resubmitBtn.setManaged(true);
            }
            default -> { // PENDING
                verifyIcon.setText("⏳");
                verifyTitle.setText("Verification Pending");
                verifyMessage.setText("Your verification documents are being reviewed. You'll be notified once approved.");
                verifyBadge.setText("PENDING");
                verifyBadge.getStyleClass().removeAll("badge-warning", "badge-danger", "badge-success");
                verifyBadge.getStyleClass().add("badge-warning");
                verificationCard.setStyle("-fx-padding: 16 20; -fx-border-color: #F59E0B; -fx-border-width: 0 0 0 4; -fx-border-radius: 8;");
                rejectionLabel.setVisible(false); rejectionLabel.setManaged(false);
                resubmitBtn.setVisible(false); resubmitBtn.setManaged(false);
            }
        }
    }

    @FXML
    private void handleAvatarChange() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile Photo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = fc.showOpenDialog(avatarContainer.getScene().getWindow());
        if (file == null) return;

        Task<String> task = new Task<>() {
            @Override protected String call() {
                return profileService.updateAvatar(file, com.stla.domain.enums.AppRole.INSTRUCTOR);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            String url = task.getValue();
            if (url != null) {
                avatar.setAvatar(url, nameLabel.getText());
                ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(), "Avatar updated!", ToastNotification.Type.SUCCESS);
            }
        }));
        new Thread(task).start();
    }

    @FXML
    private void handleSave() {
        saveBtn.setDisable(true);
        saveLoading.setVisible(true); saveLoading.setManaged(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                SessionManager session = SessionManager.getInstance();
                Profile p = session.getCurrentProfile();
                Instructor inst = session.getCurrentInstructor();

                p.setFullName(editNameField.getText().trim());
                p.setPhone(editPhoneField.getText().trim());
                p.setCountry(editCountryField.getText().trim());
                p.setBio(editBioField.getText().trim());

                inst.setTitle(editTitleField.getText().trim());
                String yearsStr = editYearsField.getText().trim();
                inst.setYearsExperience(yearsStr.isEmpty() ? null : Integer.parseInt(yearsStr));
                inst.setExpertiseTags(editExpertiseField.getText().trim().isEmpty() ? new String[0] : editExpertiseField.getText().trim().split(",\\s*"));
                inst.setInstructorBio(editInstructorBioField.getText().trim());

                profileService.updateInstructorProfile(p, inst);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveLoading.setVisible(false); saveLoading.setManaged(false);
            nameLabel.setText(editNameField.getText().trim());
            ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(), "Profile saved!", ToastNotification.Type.SUCCESS);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveLoading.setVisible(false); saveLoading.setManaged(false);
            ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(), "Save failed: " + task.getException().getMessage(), ToastNotification.Type.ERROR);
        }));
        new Thread(task).start();
    }

    @FXML
    private void handleCancel() {
        loadProfile(); // Reload from session
    }

    @FXML
    private void handleResubmitDocuments() {
        // Re-upload all 4 documents
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        fc.setTitle("Select ID Front");
        File idFront = fc.showOpenDialog(avatarContainer.getScene().getWindow());
        if (idFront == null) return;

        fc.setTitle("Select ID Back");
        File idBack = fc.showOpenDialog(avatarContainer.getScene().getWindow());
        if (idBack == null) return;

        fc.setTitle("Select Experience Certificate");
        File cert = fc.showOpenDialog(avatarContainer.getScene().getWindow());
        if (cert == null) return;

        FileChooser cvFc = new FileChooser();
        cvFc.setTitle("Select CV / Resume");
        cvFc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File cv = cvFc.showOpenDialog(avatarContainer.getScene().getWindow());
        if (cv == null) return;

        saveBtn.setDisable(true);
        saveLoading.setVisible(true); saveLoading.setManaged(true);

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                SessionManager session = SessionManager.getInstance();
                String profileId = session.getCurrentUserId();
                String instructorId = session.getCurrentInstructor().getId();
                String[] urls = instructorService.uploadVerificationDocuments(idFront, idBack, cert, cv, profileId);
                instructorService.resubmitVerificationDocuments(instructorId, urls[0], urls[1], urls[2], urls[3]);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveLoading.setVisible(false); saveLoading.setManaged(false);
            loadProfile();
            ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(), "Documents resubmitted for review!", ToastNotification.Type.SUCCESS);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveLoading.setVisible(false); saveLoading.setManaged(false);
            ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(), "Resubmission failed.", ToastNotification.Type.ERROR);
        }));
        new Thread(task).start();
    }
}
