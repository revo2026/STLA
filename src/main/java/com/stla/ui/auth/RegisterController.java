package com.stla.ui.auth;

import com.stla.core.navigation.NavigationManager;
import com.stla.domain.enums.AppRole;
import com.stla.domain.models.CountryPhone;
import com.stla.services.AuthService;
import com.stla.services.CountryPhoneLoader;
import com.stla.services.SupabaseStorageService;
import com.stla.ui.components.AnimationUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.ZoneId;
import java.util.*;

/**
 * Premium 6-step Registration controller.
 * Features: chip selectors, avatar Supabase upload, country phone, auto timezone,
 *           skill level, preferred language, daily goal minutes.
 */
public class RegisterController {

    // Containers
    @FXML private VBox registerCard;
    @FXML private Region brandPanel;
    @FXML private HBox authSplit;
    @FXML private VBox step1Pane, step2Pane, step3Pane, step4StudentPane, step4InstructorPane, step4bDocPane, step5Pane, step6Pane;
    @FXML private Label stepDescription;
    @FXML private HBox stepperBar, loginLink;
    @FXML private Label stepLbl1, stepLbl2, stepLbl3, stepLbl4, stepLbl5, stepLbl6;

    // Role
    @FXML private Button roleStudentBtn, roleInstructorBtn;

    // Step 2
    @FXML private TextField fullNameField, emailField, phoneField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private ComboBox<CountryPhone> countryCombo;
    @FXML private Label dialCodeLabel, phoneLbl;
    @FXML private Label fullNameErr, emailErr, passwordErr, confirmErr, countryErr, phoneErr;

    // Step 3
    @FXML private StackPane avatarCard;
    @FXML private ImageView avatarPreview;
    @FXML private Label avatarPlaceholder, tzLabel;
    @FXML private TextArea bioField;

    // Step 4A: Student chips
    @FXML private FlowPane headlineChips, interestChips, goalChips;
    @FXML private FlowPane skillLevelChips, languageChips, dailyGoalChips;
    @FXML private Label interestsErr, goalsErr;

    // Step 4B: Instructor
    @FXML private TextField titleField, expertiseField, yearsExpField;
    @FXML private Label titleErr, expertiseErr, yearsErr;

    // Step 4B-2: Instructor Documents
    @FXML private TextArea instructorBioField;
    @FXML private Label idFrontLabel, idBackLabel, certLabel, cvLabel;
    @FXML private VBox idFrontCard, idBackCard, certCard, cvCard;
    @FXML private ImageView idFrontPreview, idBackPreview, certPreview;
    @FXML private Label docsErr;

    // Step 5
    @FXML private Label rvRole, rvName, rvEmail, rvPhone, rvCountry, rvBio;
    @FXML private Label rvExtra1, rvExtra2, rvExtra3, rvExtra4, rvExtra5, rvExtra6;
    @FXML private Button registerButton;
    @FXML private ProgressIndicator regLoading;

    // Step 6
    @FXML private Label successMsg;
    @FXML private Label globalError;

    private final AuthService authService = new AuthService();
    private AppRole selectedRole = AppRole.STUDENT;
    private File avatarFile = null;
    private File idFrontFile = null, idBackFile = null, certFile = null, cvFile = null;
    private String detectedTimezone;

    // Chip selections
    private final Set<String> selectedHeadlines = new LinkedHashSet<>();
    private final Set<String> selectedInterests = new LinkedHashSet<>();
    private final Set<String> selectedGoals = new LinkedHashSet<>();
    private String selectedSkillLevel = "Beginner";
    private String selectedLanguage = "English";
    private String selectedDailyGoal = "30";

    // Chip data
    private static final String[] HEADLINES = {
        "💻 Future Software Engineer", "🎨 Aspiring UI/UX Designer", "🤖 AI Enthusiast",
        "📊 Data Analyst Learner", "🔒 Cybersecurity Beginner", "📱 Mobile App Developer",
        "🌐 Web Developer", "☁ Cloud Computing Student"
    };
    private static final String[] INTERESTS = {
        "💻 Programming", "🎨 Design", "📈 Marketing", "📊 Data Science",
        "🤖 AI", "📱 Mobile Dev", "🌐 Web Dev", "🔒 Cybersecurity",
        "💼 Business", "☁ Cloud Computing"
    };
    private static final String[] GOALS = {
        "💼 Get a Job", "📈 Improve Skills", "🎓 Prepare for University",
        "🛠 Build Real Projects", "🧑‍💻 Become Freelancer", "🔄 Career Switch",
        "🎮 Learn for Fun", "📜 Earn Certificates"
    };
    private static final String[] SKILL_LEVELS = { "🌱 Beginner", "📚 Intermediate", "🚀 Advanced" };
    private static final String[] LANGUAGES = { "🇺🇸 English", "🇪🇬 Arabic", "🇫🇷 French", "🇩🇪 German", "🇪🇸 Spanish", "🇯🇵 Japanese", "🇨🇳 Chinese" };
    private static final String[] DAILY_GOALS = { "⏱ 15 min", "⏱ 30 min", "⏱ 45 min", "⏱ 60 min", "⏱ 90 min", "⏱ 120 min" };

    @FXML
    public void initialize() {
        detectedTimezone = ZoneId.systemDefault().getId();
        Platform.runLater(() -> {
            tzLabel.setText("🕐 Timezone: " + detectedTimezone + " (auto-detected)");
            AnimationUtils.fadeInUp(registerCard, 400);
            AuthLayoutHelper.bindResponsive(registerCard, brandPanel, registerCard);
            bindChipWrapLengths();
            setupCountryCombo();
            buildChips(headlineChips, HEADLINES, selectedHeadlines);
            buildChips(interestChips, INTERESTS, selectedInterests);
            buildChips(goalChips, GOALS, selectedGoals);
            buildSingleSelectChips(skillLevelChips, SKILL_LEVELS, "Beginner", v -> selectedSkillLevel = v);
            buildSingleSelectChips(languageChips, LANGUAGES, "English", v -> selectedLanguage = v);
            buildSingleSelectChips(dailyGoalChips, DAILY_GOALS, "30 min", v -> selectedDailyGoal = v.replaceAll("[^0-9]", ""));
            setupAvatarClip();
        });
    }

    private void bindChipWrapLengths() {
        Runnable update = () -> {
            double w = registerCard.getWidth() > 0 ? registerCard.getWidth() - 40 : 560;
            double wrap = Math.max(260, w);
            if (headlineChips != null) headlineChips.setPrefWrapLength(wrap);
            if (interestChips != null) interestChips.setPrefWrapLength(wrap);
            if (goalChips != null) goalChips.setPrefWrapLength(wrap);
            if (skillLevelChips != null) skillLevelChips.setPrefWrapLength(wrap);
            if (languageChips != null) languageChips.setPrefWrapLength(wrap);
            if (dailyGoalChips != null) dailyGoalChips.setPrefWrapLength(wrap);
        };
        registerCard.widthProperty().addListener((o, ov, nv) -> update.run());
        Platform.runLater(update);
    }

    // ======================== COUNTRY PHONE ========================

    private void setupCountryCombo() {
        List<CountryPhone> countries = CountryPhoneLoader.loadCountries();
        countryCombo.setItems(FXCollections.observableArrayList(countries));

        javafx.util.Callback<ListView<CountryPhone>, ListCell<CountryPhone>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(CountryPhone item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(item.getFlagEmoji() + "  " + item.getName() + "  (" + item.getDialCode() + ")"); setStyle("-fx-font-size: 13px;"); }
            }
        };
        countryCombo.setCellFactory(cellFactory);
        countryCombo.setButtonCell(cellFactory.call(null));

        CountryPhone detected = CountryPhoneLoader.detectDefaultCountry();
        if (detected != null) {
            countryCombo.getSelectionModel().select(detected);
            updateDialCodeLabel(detected);
            phoneField.setPromptText(detected.getPhoneFormat());
        }

        countryCombo.setOnAction(e -> {
            CountryPhone sel = countryCombo.getSelectionModel().getSelectedItem();
            if (sel != null) { updateDialCodeLabel(sel); phoneField.setPromptText(sel.getPhoneFormat()); clearFieldError(phoneField, phoneErr); }
        });

        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            CountryPhone sel = countryCombo.getSelectionModel().getSelectedItem();
            if (sel != null && !newVal.isBlank()) {
                if (CountryPhoneLoader.validatePhone(sel, newVal)) { clearFieldError(phoneField, phoneErr); }
                else { phoneErr.setText("Invalid format for " + sel.getName() + " (" + sel.getPhoneFormat() + ")"); phoneErr.setVisible(true); phoneErr.setManaged(true); }
            } else { clearFieldError(phoneField, phoneErr); }
        });
    }

    private void updateDialCodeLabel(CountryPhone cp) {
        dialCodeLabel.setText(cp.getFlagEmoji() + " " + cp.getDialCode());
    }

    // ======================== AVATAR UPLOAD ========================

    private void setupAvatarClip() {
        Circle clip = new Circle(40, 40, 40);
        avatarPreview.setClip(clip);
        avatarPreview.setImage(null);
    }

    @FXML
    private void handleAvatarUpload() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile Photo");
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = fc.showOpenDialog(registerCard.getScene().getWindow());
        if (file != null) {
            avatarFile = file;
            Image img = new Image(file.toURI().toString(), 80, 80, true, true);
            avatarPreview.setImage(img);
            avatarPlaceholder.setVisible(false);
            AnimationUtils.scaleBounce(avatarCard);
        }
    }

    // ======================== CHIP BUILDERS ========================

    private void buildChips(FlowPane container, String[] items, Set<String> selection) {
        container.getChildren().clear();
        for (String item : items) {
            Button chip = new Button(item);
            chip.getStyleClass().add("chip");
            chip.setOnAction(e -> {
                if (selection.contains(item)) { selection.remove(item); chip.getStyleClass().remove("chip-selected"); }
                else { selection.add(item); chip.getStyleClass().add("chip-selected"); }
                AnimationUtils.scaleBounce(chip);
            });
            container.getChildren().add(chip);
        }
    }

    /** Single-select chips — only one active at a time */
    private void buildSingleSelectChips(FlowPane container, String[] items, String defaultLabel, java.util.function.Consumer<String> onSelect) {
        container.getChildren().clear();
        for (String item : items) {
            Button chip = new Button(item);
            chip.getStyleClass().add("chip");
            // Select default
            if (item.contains(defaultLabel)) chip.getStyleClass().add("chip-selected");
            chip.setOnAction(e -> {
                container.getChildren().forEach(n -> n.getStyleClass().remove("chip-selected"));
                chip.getStyleClass().add("chip-selected");
                onSelect.accept(item);
                AnimationUtils.scaleBounce(chip);
            });
            container.getChildren().add(chip);
        }
    }

    // ======================== ROLE ========================

    @FXML private void selectStudent() {
        selectedRole = AppRole.STUDENT;
        roleStudentBtn.getStyleClass().remove("role-btn-active"); roleInstructorBtn.getStyleClass().remove("role-btn-active");
        roleStudentBtn.getStyleClass().add("role-btn-active"); AnimationUtils.scaleBounce(roleStudentBtn);
    }

    @FXML private void selectInstructor() {
        selectedRole = AppRole.INSTRUCTOR;
        roleStudentBtn.getStyleClass().remove("role-btn-active"); roleInstructorBtn.getStyleClass().remove("role-btn-active");
        roleInstructorBtn.getStyleClass().add("role-btn-active"); AnimationUtils.scaleBounce(roleInstructorBtn);
    }

    // ======================== STEPS ========================

    @FXML private void goStep1() { showStep(1); }
    @FXML private void goStep2() { showStep(2); }

    @FXML private void goStep3() {
        boolean ok = true;
        ok &= validateRequired(fullNameField, fullNameErr, "Full name is required");
        ok &= validateEmail(emailField, emailErr);
        ok &= validatePassword(passwordField, passwordErr);
        ok &= validateConfirm();
        if (countryCombo.getSelectionModel().isEmpty()) { countryErr.setText("Please select a country"); countryErr.setVisible(true); countryErr.setManaged(true); ok = false; }
        else { countryErr.setVisible(false); countryErr.setManaged(false); }
        ok &= validatePhone();
        if (!ok) { AnimationUtils.shake(step2Pane); return; }
        showStep(3);
    }

    @FXML private void goStep4() { showStep(4); }

    @FXML private void goStep4b() {
        // Validate instructor details before moving to docs step
        boolean ok = true;
        ok &= validateRequired(titleField, titleErr, "Title is required");
        ok &= validateRequired(expertiseField, expertiseErr, "Expertise is required");
        ok &= validateYears();
        if (!ok) { AnimationUtils.shake(step4InstructorPane); return; }
        showStep(42); // 42 = special step for docs
    }

    @FXML private void goStep5() {
        if (selectedRole == AppRole.STUDENT) {
            boolean ok = true;
            if (selectedInterests.isEmpty()) { interestsErr.setText("Select at least one interest"); interestsErr.setVisible(true); interestsErr.setManaged(true); ok = false; }
            else { interestsErr.setVisible(false); interestsErr.setManaged(false); }
            if (selectedGoals.isEmpty()) { goalsErr.setText("Select at least one goal"); goalsErr.setVisible(true); goalsErr.setManaged(true); ok = false; }
            else { goalsErr.setVisible(false); goalsErr.setManaged(false); }
            if (!ok) { AnimationUtils.shake(step4StudentPane); return; }
        } else {
            // Validate all 4 documents selected
            boolean ok = true;
            if (idFrontFile == null || idBackFile == null || certFile == null || cvFile == null) {
                if (docsErr != null) { docsErr.setText("All 4 verification documents are required"); docsErr.setVisible(true); docsErr.setManaged(true); }
                ok = false;
            } else {
                if (docsErr != null) { docsErr.setVisible(false); docsErr.setManaged(false); }
            }
            if (!ok) { AnimationUtils.shake(step4bDocPane); return; }
        }
        populateReview();
        showStep(5);
    }

    private void showStep(int step) {
        step1Pane.setVisible(step == 1); step1Pane.setManaged(step == 1);
        step2Pane.setVisible(step == 2); step2Pane.setManaged(step == 2);
        step3Pane.setVisible(step == 3); step3Pane.setManaged(step == 3);
        boolean s4s = step == 4 && selectedRole == AppRole.STUDENT;
        boolean s4i = step == 4 && selectedRole == AppRole.INSTRUCTOR;
        step4StudentPane.setVisible(s4s); step4StudentPane.setManaged(s4s);
        step4InstructorPane.setVisible(s4i); step4InstructorPane.setManaged(s4i);
        boolean docStep = step == 42;
        if (step4bDocPane != null) { step4bDocPane.setVisible(docStep); step4bDocPane.setManaged(docStep); }
        step5Pane.setVisible(step == 5); step5Pane.setManaged(step == 5);
        step6Pane.setVisible(step == 6); step6Pane.setManaged(step == 6);

        // Stepper: for instructor flow, step 42 maps to stepper position 5 (docs), step 5 maps to 6 (review)
        boolean isInstructor = selectedRole == AppRole.INSTRUCTOR;
        Label[] labels;
        int mapped;
        if (isInstructor && stepLbl6 != null) {
            labels = new Label[]{ stepLbl1, stepLbl2, stepLbl3, stepLbl4, stepLbl5, stepLbl6 };
            mapped = switch (step) {
                case 1 -> 1; case 2 -> 2; case 3 -> 3; case 4 -> 4; case 42 -> 5; case 5 -> 6; case 6 -> 7;
                default -> 1;
            };
            stepLbl5.setText("⑤ Docs");
            stepLbl6.setText("⑥ Review");
            stepLbl6.setVisible(true); stepLbl6.setManaged(true);
        } else {
            labels = new Label[]{ stepLbl1, stepLbl2, stepLbl3, stepLbl4, stepLbl5 };
            mapped = Math.min(step, 5);
            stepLbl5.setText("⑤ Review");
            if (stepLbl6 != null) { stepLbl6.setVisible(false); stepLbl6.setManaged(false); }
        }
        for (int i = 0; i < labels.length; i++) {
            labels[i].getStyleClass().removeAll("stepper-active", "stepper-inactive", "stepper-done");
            if (i + 1 < mapped) labels[i].getStyleClass().add("stepper-done");
            else if (i + 1 == mapped) labels[i].getStyleClass().add("stepper-active");
            else labels[i].getStyleClass().add("stepper-inactive");
        }

        stepDescription.setText(switch (step) {
            case 1 -> "Choose your role";
            case 2 -> "Enter your account information";
            case 3 -> "Set up your profile";
            case 4 -> selectedRole == AppRole.STUDENT ? "Tell us about your learning" : "Tell us about your expertise";
            case 42 -> "Upload your verification documents";
            case 5 -> "Review and confirm";
            case 6 -> "You're all set!";
            default -> "";
        });

        stepperBar.setVisible(step != 6); stepperBar.setManaged(step != 6);
        loginLink.setVisible(step != 6); loginLink.setManaged(step != 6);

        VBox vis = switch (step) {
            case 1 -> step1Pane; case 2 -> step2Pane; case 3 -> step3Pane;
            case 4 -> selectedRole == AppRole.STUDENT ? step4StudentPane : step4InstructorPane;
            case 42 -> step4bDocPane;
            case 5 -> step5Pane; case 6 -> step6Pane; default -> step1Pane;
        };
        if (vis != null) AnimationUtils.fadeIn(vis, 250);
        hideGlobalError();
    }

    // ======================== REVIEW ========================

    private void populateReview() {
        CountryPhone cp = countryCombo.getSelectionModel().getSelectedItem();
        String roleName = selectedRole.getValue().substring(0, 1).toUpperCase() + selectedRole.getValue().substring(1);
        rvRole.setText("🏷  Role:  " + roleName);
        rvName.setText("👤  Name:  " + fullNameField.getText().trim());
        rvEmail.setText("✉  Email:  " + emailField.getText().trim());
        String fullPhone = (cp != null ? cp.getDialCode() : "") + phoneField.getText().trim();
        rvPhone.setText("📞  Phone:  " + fullPhone);
        rvCountry.setText("🌍  Country:  " + (cp != null ? cp.getFlagEmoji() + " " + cp.getName() : ""));
        rvBio.setText("📝  Bio:  " + (bioField.getText().isBlank() ? "—" : bioField.getText().trim()));

        if (selectedRole == AppRole.STUDENT) {
            rvExtra1.setText("📰  Headline:  " + String.join(", ", selectedHeadlines));
            rvExtra2.setText("💡  Interests:  " + String.join(", ", selectedInterests));
            rvExtra3.setText("🎯  Goals:  " + String.join(", ", selectedGoals));
            rvExtra4.setText("📊  Skill Level:  " + selectedSkillLevel);
            rvExtra5.setText("🌐  Language:  " + selectedLanguage);
            rvExtra6.setText("⏱  Daily Goal:  " + selectedDailyGoal + " min");
        } else {
            rvExtra1.setText("💼  Title:  " + titleField.getText().trim());
            rvExtra2.setText("🎯  Expertise:  " + expertiseField.getText().trim());
            rvExtra3.setText("📅  Experience:  " + (yearsExpField.getText().isBlank() ? "—" : yearsExpField.getText().trim() + " years"));
            rvExtra4.setText("📄  ID Front:  " + (idFrontFile != null ? "✅ " + idFrontFile.getName() : "❌ Not uploaded"));
            rvExtra5.setText("📄  ID Back:  " + (idBackFile != null ? "✅ " + idBackFile.getName() : "❌ Not uploaded"));
            String certName = certFile != null ? "✅ " + certFile.getName() : "❌ Not uploaded";
            String cvName = cvFile != null ? "✅ " + cvFile.getName() : "❌ Not uploaded";
            rvExtra6.setText("📄  Certificate: " + certName + "  |  CV: " + cvName);
        }
    }

    // ======================== REGISTER ========================

    @FXML
    private void handleRegister() {
        setLoading(true);
        hideGlobalError();

        CountryPhone cp = countryCombo.getSelectionModel().getSelectedItem();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String phone = (cp != null ? cp.getDialCode() : "") + phoneField.getText().trim();
        String country = cp != null ? cp.getName() : "";
        String bio = bioField.getText().trim();
        String timezone = detectedTimezone;

        // Chip values → strings for DB
        String headline = String.join(", ", selectedHeadlines);
        String interests = String.join(", ", selectedInterests);
        String learningGoals = String.join(", ", selectedGoals);
        // Extract clean values from chip labels
        String skillLevel = selectedSkillLevel.replaceAll("^[^\\s]+\\s+", "").toLowerCase(); // "🌱 Beginner" → "beginner"
        String preferredLang = selectedLanguage.replaceAll("^[^\\s]+\\s+", "").toLowerCase(); // "🇺🇸 English" → "english"
        String dailyGoal = selectedDailyGoal.replaceAll("[^0-9]", ""); // "30"

        String title = titleField.getText().trim();
        String expertise = expertiseField.getText().trim();
        String yearsExp = yearsExpField.getText().trim();

        String instrBio = instructorBioField != null ? instructorBioField.getText().trim() : "";

        Task<AuthService.AuthResult> task = new Task<>() {
            @Override
            protected AuthService.AuthResult call() {
                // Upload avatar to Supabase Storage if file selected
                String avatarUrl = "";
                if (avatarFile != null) {
                    String role = selectedRole.getValue();
                    String tempId = java.util.UUID.randomUUID().toString();
                    String uploaded = SupabaseStorageService.getInstance().uploadAvatar(avatarFile, role, tempId);
                    if (uploaded != null) avatarUrl = uploaded;
                    System.out.println("[Register] Avatar URL: " + avatarUrl);
                }

                // Upload instructor verification documents
                String idFrontUrl = null, idBackUrl = null, certUrl = null, cvUrl = null;
                if (selectedRole == AppRole.INSTRUCTOR) {
                    String tempId = java.util.UUID.randomUUID().toString();
                    System.out.println("[Register] Uploading instructor docs with tempId: " + tempId);

                    com.stla.services.InstructorService instrService = new com.stla.services.InstructorService();
                    String[] urls = instrService.uploadVerificationDocuments(idFrontFile, idBackFile, certFile, cvFile, tempId);
                    idFrontUrl = urls[0];
                    idBackUrl = urls[1];
                    certUrl = urls[2];
                    cvUrl = urls[3];

                    System.out.println("[Register] ===== DOCUMENT URLS =====");
                    System.out.println("[Register] ID Front URL:  " + idFrontUrl);
                    System.out.println("[Register] ID Back URL:   " + idBackUrl);
                    System.out.println("[Register] Cert URL:      " + certUrl);
                    System.out.println("[Register] CV URL:        " + cvUrl);
                    System.out.println("[Register] ========================");

                    // Fail if any document upload failed
                    if (idFrontUrl == null || idBackUrl == null || certUrl == null || cvUrl == null) {
                        String missing = "";
                        if (idFrontUrl == null) missing += "ID Front, ";
                        if (idBackUrl == null) missing += "ID Back, ";
                        if (certUrl == null) missing += "Certificate, ";
                        if (cvUrl == null) missing += "CV, ";
                        return AuthService.AuthResult.failure("Document upload failed for: " + missing.replaceAll(", $", "") + ". Please check your internet connection and try again.");
                    }
                }

                return authService.register(
                    fullName, email, password, selectedRole,
                    phone, country, bio, avatarUrl, timezone,
                    headline, interests, learningGoals,
                    skillLevel, preferredLang, dailyGoal,
                    title, expertise, yearsExp,
                    instrBio, idFrontUrl, idBackUrl, certUrl, cvUrl
                );
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            AuthService.AuthResult result = task.getValue();
            if (result.isSuccess()) {
                String msg = selectedRole == AppRole.INSTRUCTOR
                    ? "Welcome, " + fullName + "! Your instructor account is pending verification. You'll be notified once approved."
                    : "Welcome, " + fullName + "! Your " + selectedRole.getValue() + " account is ready.";
                successMsg.setText(msg);
                showStep(6);
                AnimationUtils.successBounce(step6Pane);
            } else {
                showGlobalError(result.getMessage());
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showGlobalError("Registration failed. Please try again.");
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    @FXML private void goToDashboard() { NavigationManager.getInstance().navigateToDashboard(); }
    @FXML private void goToLogin() { NavigationManager.getInstance().navigateTo("/com/stla/views/auth/login.fxml"); }

    // ======================== DOCUMENT UPLOADS ========================

    @FXML private void handleIdFrontUpload() {
        File f = chooseImageFile("Select ID Front Photo");
        if (f != null) { idFrontFile = f; updateDocCard(idFrontCard, idFrontLabel, idFrontPreview, f, true); }
    }
    @FXML private void handleIdBackUpload() {
        File f = chooseImageFile("Select ID Back Photo");
        if (f != null) { idBackFile = f; updateDocCard(idBackCard, idBackLabel, idBackPreview, f, true); }
    }
    @FXML private void handleCertUpload() {
        File f = chooseImageFile("Select Experience Certificate");
        if (f != null) { certFile = f; updateDocCard(certCard, certLabel, certPreview, f, true); }
    }
    @FXML private void handleCvUpload() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select CV / Resume");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        File f = fc.showOpenDialog(registerCard.getScene().getWindow());
        if (f != null) { cvFile = f; updateDocCard(cvCard, cvLabel, null, f, false); }
    }

    private File chooseImageFile(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        return fc.showOpenDialog(registerCard.getScene().getWindow());
    }

    private void updateDocCard(VBox card, Label label, ImageView preview, File file, boolean showPreview) {
        if (card != null) { card.getStyleClass().remove("upload-card-success"); card.getStyleClass().add("upload-card-success"); }
        String sizeKb = String.format("%.1f KB", file.length() / 1024.0);
        if (label != null) label.setText("✅ " + file.getName() + " (" + sizeKb + ")");
        if (showPreview && preview != null) {
            preview.setImage(new Image(file.toURI().toString(), 60, 60, true, true));
        }
        if (docsErr != null) { docsErr.setVisible(false); docsErr.setManaged(false); }
        if (card != null) AnimationUtils.scaleBounce(card);
    }

    // ======================== VALIDATION ========================

    private boolean validateRequired(TextField f, Label err, String msg) {
        if (f.getText().trim().isEmpty()) { showFieldError(f, err, msg); return false; }
        clearFieldError(f, err); return true;
    }
    private boolean validateEmail(TextField f, Label err) {
        String v = f.getText().trim();
        if (v.isEmpty()) { showFieldError(f, err, "Email is required"); return false; }
        if (!v.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) { showFieldError(f, err, "Invalid email format"); return false; }
        clearFieldError(f, err); return true;
    }
    private boolean validatePassword(PasswordField f, Label err) {
        if (f.getText().length() < 8) { showFieldError(f, err, "Min 8 characters"); return false; }
        clearFieldError(f, err); return true;
    }
    private boolean validateConfirm() {
        if (!passwordField.getText().equals(confirmPasswordField.getText())) { showFieldError(confirmPasswordField, confirmErr, "Passwords don't match"); return false; }
        clearFieldError(confirmPasswordField, confirmErr); return true;
    }
    private boolean validatePhone() {
        CountryPhone cp = countryCombo.getSelectionModel().getSelectedItem();
        String val = phoneField.getText().trim();
        if (val.isEmpty()) { showFieldError(phoneField, phoneErr, "Phone number is required"); return false; }
        if (cp != null && !CountryPhoneLoader.validatePhone(cp, val)) { showFieldError(phoneField, phoneErr, "Invalid format for " + cp.getName() + " (" + cp.getPhoneFormat() + ")"); return false; }
        clearFieldError(phoneField, phoneErr); return true;
    }
    private boolean validateYears() {
        String v = yearsExpField.getText().trim();
        if (!v.isEmpty()) {
            try { if (Integer.parseInt(v) < 0) { showFieldError(yearsExpField, yearsErr, "Must be >= 0"); return false; } }
            catch (NumberFormatException ex) { showFieldError(yearsExpField, yearsErr, "Must be a number"); return false; }
        }
        clearFieldError(yearsExpField, yearsErr); return true;
    }

    private void showFieldError(Control f, Label err, String msg) {
        f.getStyleClass().remove("error"); f.getStyleClass().add("error");
        err.setText(msg); err.setVisible(true); err.setManaged(true);
    }
    private void clearFieldError(Control f, Label err) {
        f.getStyleClass().remove("error"); err.setVisible(false); err.setManaged(false);
    }
    private void showGlobalError(String msg) {
        Platform.runLater(() -> { globalError.setText(msg); globalError.setVisible(true); globalError.setManaged(true); AnimationUtils.shake(registerCard); });
    }
    private void hideGlobalError() { globalError.setVisible(false); globalError.setManaged(false); }
    private void setLoading(boolean b) {
        Platform.runLater(() -> { registerButton.setDisable(b); regLoading.setVisible(b); regLoading.setManaged(b); registerButton.setText(b ? "Creating account..." : "Confirm Registration"); });
    }
}
