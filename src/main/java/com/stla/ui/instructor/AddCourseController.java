package com.stla.ui.instructor;

import com.stla.core.session.SessionManager;
import com.stla.data.repositories.CategoryRepositoryImpl;
import com.stla.data.repositories.CourseRepositoryImpl;
import com.stla.domain.enums.CourseLevel;
import com.stla.domain.enums.CourseStatus;
import com.stla.domain.models.Category;
import com.stla.domain.models.Course;
import com.stla.patterns.facade.CoursePublishFacade;
import com.stla.services.SupabaseStorageAdapter;
import com.stla.ui.components.AnimationUtils;
import com.stla.ui.components.ComponentFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

/**
 * Multi-step course creation wizard (4 steps).
 * Step 1: Basic Info, Step 2: Description, Step 3: Add-ons, Step 4: Review.
 */
public class AddCourseController {

    @javafx.fxml.FXML private HBox stepIndicator;
    @javafx.fxml.FXML private StackPane stepContainer;
    @javafx.fxml.FXML private HBox navButtons;
    @javafx.fxml.FXML private Button btnBack, btnNext, btnSaveDraft, btnSubmit;

    private final CourseRepositoryImpl courseRepo = new CourseRepositoryImpl();
    private final CategoryRepositoryImpl categoryRepo = new CategoryRepositoryImpl();
    private final CoursePublishFacade facade = new CoursePublishFacade();
    private final SupabaseStorageAdapter storageAdapter = new SupabaseStorageAdapter();

    private int currentStep = 0;
    private final String[] stepTitles = {"Basic Info", "Description", "Add-ons", "Review"};
    private final VBox[] stepPanes = new VBox[4];

    // Step 1 fields
    private TextField titleField, subtitleField, priceField, hoursField, videoUrlField;
    private ComboBox<String> categoryCombo, languageCombo;
    private ToggleGroup levelGroup;
    private Label thumbnailLabel;
    private File thumbnailFile;
    private List<Category> categories;

    // Step 2 fields
    private TextArea descriptionArea, learnArea, requirementsArea, audienceArea;

    // Step 3 toggles
    private CheckBox certToggle, quizToggle, mentorToggle, resourcesToggle;

    // Step 4 review labels
    private VBox reviewBox;

    private VBox successPane;

    private Runnable onGoToMyCourses;

    public void setOnGoToMyCourses(Runnable handler) {
        this.onGoToMyCourses = handler;
    }

    @javafx.fxml.FXML
    public void initialize() {
        loadCategories();
        buildStepIndicator();
        buildStep1();
        buildStep2();
        buildStep3();
        buildStep4();
        showStep(0);
    }

    private void loadCategories() {
        try {
            categories = categoryRepo.findAll();
        } catch (Exception e) {
            categories = List.of();
        }
    }

    // ==================== STEP INDICATOR ====================

    private void buildStepIndicator() {
        stepIndicator.getChildren().clear();
        for (int i = 0; i < stepTitles.length; i++) {
            int idx = i;
            VBox step = new VBox(4);
            step.setAlignment(Pos.CENTER);

            Circle circle = new Circle(16);
            circle.setFill(i == 0 ? Color.web("#7C3AED") : Color.web("#E5E7EB"));
            circle.setStroke(Color.web("#7C3AED"));
            circle.setStrokeWidth(2);
            Label num = new Label(String.valueOf(i + 1));
            num.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (i == 0 ? "white" : "#7C3AED") + "; -fx-font-size: 13px;");

            StackPane circlePane = new StackPane(circle, num);

            Label title = new Label(stepTitles[i]);
            title.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

            step.getChildren().addAll(circlePane, title);
            stepIndicator.getChildren().add(step);

            if (i < stepTitles.length - 1) {
                Region line = new Region();
                line.setPrefHeight(2);
                line.setPrefWidth(60);
                line.setStyle("-fx-background-color: #E5E7EB;");
                HBox.setMargin(line, new Insets(12, 0, 0, 0));
                stepIndicator.getChildren().add(line);
            }
        }
    }

    private void updateStepIndicator() {
        int childIdx = 0;
        for (int i = 0; i < stepTitles.length; i++) {
            VBox step = (VBox) stepIndicator.getChildren().get(childIdx);
            StackPane circlePane = (StackPane) step.getChildren().get(0);
            Circle circle = (Circle) circlePane.getChildren().get(0);
            Label num = (Label) circlePane.getChildren().get(1);

            if (i <= currentStep) {
                circle.setFill(Color.web("#7C3AED"));
                num.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 13px;");
            } else {
                circle.setFill(Color.web("#E5E7EB"));
                num.setStyle("-fx-font-weight: bold; -fx-text-fill: #7C3AED; -fx-font-size: 13px;");
            }

            childIdx++;
            if (i < stepTitles.length - 1) {
                Region line = (Region) stepIndicator.getChildren().get(childIdx);
                line.setStyle("-fx-background-color: " + (i < currentStep ? "#7C3AED" : "#E5E7EB") + ";");
                childIdx++;
            }
        }
    }

    // ==================== STEP 1: BASIC INFO ====================

    private void buildStep1() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("card");

        Label header = new Label("📚 Basic Course Information");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        titleField = createStyledField("Course Title *", "e.g. Master Java Programming");
        subtitleField = createStyledField("Course Subtitle", "A short tagline for your course");

        categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("Select Category *");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.getStyleClass().add("combo-box");
        if (categories != null) {
            for (Category c : categories) categoryCombo.getItems().add(c.getName());
        }

        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "Arabic", "French", "Spanish", "German");
        languageCombo.setValue("English");
        languageCombo.setMaxWidth(Double.MAX_VALUE);

        // Level toggle
        HBox levelBox = new HBox(12);
        levelBox.setAlignment(Pos.CENTER_LEFT);
        levelGroup = new ToggleGroup();
        for (String lvl : new String[]{"Beginner", "Intermediate", "Advanced"}) {
            RadioButton rb = new RadioButton(lvl);
            rb.setToggleGroup(levelGroup);
            rb.setStyle("-fx-font-size: 13px;");
            if (lvl.equals("Beginner")) rb.setSelected(true);
            levelBox.getChildren().add(rb);
        }

        HBox priceHoursRow = new HBox(16);
        priceField = createStyledField("Price ($) *", "29.99");
        hoursField = createStyledField("Estimated Hours", "10");
        priceField.setPrefWidth(200);
        hoursField.setPrefWidth(200);
        priceHoursRow.getChildren().addAll(
            createFieldGroup("Price ($) *", priceField),
            createFieldGroup("Estimated Hours", hoursField)
        );

        videoUrlField = createStyledField("Intro Video URL", "https://youtube.com/...");

        // Thumbnail upload
        HBox thumbRow = new HBox(12);
        thumbRow.setAlignment(Pos.CENTER_LEFT);
        thumbnailLabel = new Label("No file selected");
        thumbnailLabel.setStyle("-fx-text-fill: #6B7280;");
        Button thumbBtn = new Button("📁 Upload Thumbnail");
        thumbBtn.getStyleClass().addAll("btn-outline", "btn-sm");
        thumbBtn.setOnAction(e -> chooseThumbnail());
        thumbRow.getChildren().addAll(thumbBtn, thumbnailLabel);

        pane.getChildren().addAll(
            header,
            createFieldGroup("Course Title *", titleField),
            createFieldGroup("Course Subtitle", subtitleField),
            createFieldGroup("Category *", categoryCombo),
            createFieldGroup("Language", languageCombo),
            createFieldGroup("Level", levelBox),
            priceHoursRow,
            createFieldGroup("Intro Video URL", videoUrlField),
            createFieldGroup("Course Thumbnail", thumbRow)
        );
        stepPanes[0] = pane;
    }

    // ==================== STEP 2: DESCRIPTION ====================

    private void buildStep2() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("card");

        Label header = new Label("📝 Course Description");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        descriptionArea = createStyledTextArea("Full course description...", 6);
        learnArea = createStyledTextArea("• Students will learn...\n• Master...\n• Build...", 4);
        requirementsArea = createStyledTextArea("• Basic knowledge of...\n• A computer with...", 3);
        audienceArea = createStyledTextArea("• Beginners who want to...\n• Professionals looking for...", 3);

        pane.getChildren().addAll(
            header,
            createFieldGroup("Full Description *", descriptionArea),
            createFieldGroup("What Students Will Learn *", learnArea),
            createFieldGroup("Requirements / Prerequisites", requirementsArea),
            createFieldGroup("Target Audience", audienceArea)
        );
        stepPanes[1] = pane;
    }

    // ==================== STEP 3: ADD-ONS ====================

    private void buildStep3() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("card");

        Label header = new Label("🎯 Course Add-ons");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        Label sub = new Label("Enable additional features for your course (Decorator Pattern)");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B7280;");

        certToggle = new CheckBox("🏆 Certificate of Completion");
        quizToggle = new CheckBox("📝 Quizzes & Assessments");
        mentorToggle = new CheckBox("👨‍🏫 Mentor Support");
        resourcesToggle = new CheckBox("📦 Downloadable Resources");

        VBox toggleCards = new VBox(12);
        for (CheckBox cb : new CheckBox[]{certToggle, quizToggle, mentorToggle, resourcesToggle}) {
            HBox card = new HBox(16);
            card.getStyleClass().add("card");
            card.setPadding(new Insets(16));
            card.setAlignment(Pos.CENTER_LEFT);
            cb.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
            card.getChildren().add(cb);
            card.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 16; -fx-cursor: hand;");
            card.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));
            toggleCards.getChildren().add(card);
        }

        pane.getChildren().addAll(header, sub, toggleCards);
        stepPanes[2] = pane;
    }

    // ==================== STEP 4: REVIEW ====================

    private void buildStep4() {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("card");

        Label header = new Label("✅ Review & Submit");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        reviewBox = new VBox(8);
        pane.getChildren().addAll(header, reviewBox);
        stepPanes[3] = pane;
    }

    private void populateReview() {
        reviewBox.getChildren().clear();
        reviewBox.getChildren().addAll(
            reviewRow("📚 Title", titleField.getText()),
            reviewRow("📰 Subtitle", subtitleField.getText()),
            reviewRow("📂 Category", categoryCombo.getValue()),
            reviewRow("🌐 Language", languageCombo.getValue()),
            reviewRow("📊 Level", getSelectedLevel()),
            reviewRow("💰 Price", "$" + priceField.getText()),
            reviewRow("⏱ Hours", hoursField.getText()),
            reviewRow("🎬 Video URL", videoUrlField.getText()),
            reviewRow("🖼 Thumbnail", thumbnailFile != null ? thumbnailFile.getName() : "None"),
            new Separator(),
            reviewRow("📝 Description", truncate(descriptionArea.getText(), 200)),
            reviewRow("🎯 What You'll Learn", truncate(learnArea.getText(), 200)),
            new Separator(),
            reviewRow("🏆 Certificate", certToggle.isSelected() ? "✅ Yes" : "❌ No"),
            reviewRow("📝 Quiz", quizToggle.isSelected() ? "✅ Yes" : "❌ No"),
            reviewRow("👨‍🏫 Mentor", mentorToggle.isSelected() ? "✅ Yes" : "❌ No"),
            reviewRow("📦 Resources", resourcesToggle.isSelected() ? "✅ Yes" : "❌ No")
        );
    }

    private HBox reviewRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-min-width: 160; -fx-text-fill: #374151;");
        Label v = new Label(value != null && !value.isBlank() ? value : "—");
        v.setStyle("-fx-text-fill: #6B7280;");
        v.setWrapText(true);
        row.getChildren().addAll(l, v);
        return row;
    }

    // ==================== NAVIGATION ====================

    private void showStep(int step) {
        currentStep = step;
        stepContainer.getChildren().setAll(stepPanes[step]);
        AnimationUtils.fadeIn(stepPanes[step], 300);
        updateStepIndicator();

        btnBack.setVisible(step > 0);
        btnNext.setVisible(step < 3);
        btnSaveDraft.setVisible(step == 3);
        btnSubmit.setVisible(step == 3);

        if (step == 3) populateReview();
    }

    @javafx.fxml.FXML
    private void handleBack() { if (currentStep > 0) showStep(currentStep - 1); }

    @javafx.fxml.FXML
    private void handleNext() {
        if (currentStep == 0 && !validateStep1()) return;
        if (currentStep == 1 && !validateStep2()) return;
        if (currentStep < 3) showStep(currentStep + 1);
    }

    @javafx.fxml.FXML
    private void handleSaveDraft() {
        saveCourse(CourseStatus.DRAFT);
    }

    @javafx.fxml.FXML
    private void handleSubmit() {
        saveCourse(CourseStatus.PENDING);
    }

    // ==================== SAVE / SUBMIT ====================

    private void saveCourse(CourseStatus status) {
        btnSaveDraft.setDisable(true);
        btnSubmit.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // Upload thumbnail if selected
                String thumbUrl = null;
                if (thumbnailFile != null) {
                    String tempId = java.util.UUID.randomUUID().toString();
                    thumbUrl = storageAdapter.uploadCourseThumbnail(thumbnailFile, tempId);
                }

                Course course = new Course();
                course.setInstructorId(SessionManager.getInstance().getCurrentInstructor().getId());
                course.setTitle(titleField.getText().trim());
                course.setSlug(titleField.getText().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
                course.setSubtitle(subtitleField.getText().trim());
                course.setDescription(descriptionArea.getText().trim());
                course.setThumbnailUrl(thumbUrl);
                course.setIntroVideoUrl(videoUrlField.getText().trim());
                course.setLanguage(languageCombo.getValue());
                course.setLevel(CourseLevel.fromValue(getSelectedLevel().toLowerCase()));
                try { course.setPrice(new BigDecimal(priceField.getText().trim())); } catch (Exception e) { course.setPrice(BigDecimal.ZERO); }
                try { course.setEstimatedHours(Integer.parseInt(hoursField.getText().trim())); } catch (Exception e) { course.setEstimatedHours(null); }

                // Find category ID
                if (categoryCombo.getValue() != null && categories != null) {
                    categories.stream()
                        .filter(c -> c.getName().equals(categoryCombo.getValue()))
                        .findFirst()
                        .ifPresent(c -> course.setCategoryId(c.getId()));
                }

                course.setWhatYouWillLearn(learnArea.getText().trim());
                course.setRequirements(requirementsArea.getText().trim());
                course.setTargetAudience(audienceArea.getText().trim());
                course.setHasCertificate(certToggle.isSelected());
                course.setHasQuiz(quizToggle.isSelected());
                course.setHasMentorSupport(mentorToggle.isSelected());
                course.setHasResources(resourcesToggle.isSelected());
                course.setStatus(status);

                if (status == CourseStatus.PENDING) {
                    // Save then submit via facade
                    course.setStatus(CourseStatus.DRAFT);
                    courseRepo.save(course);
                    if (course.getId() != null) {
                        facade.submitForApproval(course.getId(), course.getInstructorId());
                    }
                } else {
                    facade.saveDraft(course);
                }

                return status == CourseStatus.PENDING ? "submitted" : "draft";
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    btnSaveDraft.setDisable(false);
                    btnSubmit.setDisable(false);
                    showSuccessPage(getValue(), titleField.getText().trim());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    btnSaveDraft.setDisable(false);
                    btnSubmit.setDisable(false);
                    String errMsg = getException() != null ? getException().getMessage() : "Unknown error";
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + errMsg, ButtonType.OK);
                    alert.setHeaderText(null);
                    alert.showAndWait();
                });
            }
        };
        new Thread(task).start();
    }

    // ==================== SUCCESS SCREEN ====================

    private void showSuccessPage(String result, String courseTitle) {
        boolean submitted = "submitted".equals(result);

        stepIndicator.setVisible(false);
        stepIndicator.setManaged(false);
        navButtons.setVisible(false);
        navButtons.setManaged(false);

        if (successPane == null) {
            successPane = new VBox(20);
            successPane.setAlignment(Pos.CENTER);
            successPane.setMaxWidth(560);
            successPane.getStyleClass().add("card");
            successPane.setPadding(new Insets(48, 40, 48, 40));
            successPane.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-color: #E5E7EB; -fx-border-radius: 16; -fx-border-width: 1;");
        }

        successPane.getChildren().clear();

        Label icon = new Label(submitted ? "🎉" : "💾");
        icon.setStyle("-fx-font-size: 56px;");

        Label headline = new Label(submitted ? "Course Submitted!" : "Draft Saved!");
        headline.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        String safeTitle = courseTitle != null && !courseTitle.isBlank() ? courseTitle : "Your course";
        Label courseName = new Label("“" + safeTitle + "”");
        courseName.setWrapText(true);
        courseName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7C3AED; -fx-text-alignment: center;");

        Label message = new Label(submitted
            ? "Your course has been sent to the admin team for review. You will be notified once it is approved."
            : "Your course draft is saved. You can continue editing it anytime from My Courses.");
        message.setWrapText(true);
        message.setMaxWidth(480);
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280; -fx-text-alignment: center;");

        VBox tips = new VBox(6);
        tips.setAlignment(Pos.CENTER_LEFT);
        tips.setPadding(new Insets(16));
        tips.setStyle("-fx-background-color: #F5F3FF; -fx-background-radius: 12;");
        Label tipsTitle = new Label(submitted ? "What happens next?" : "Next steps");
        tipsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #5B21B6;");
        Label tip1 = new Label(submitted
            ? "• Admin reviews your course details and intro video"
            : "• Add lessons and sections when you are ready");
        Label tip2 = new Label(submitted
            ? "• You can track status under My Courses"
            : "• Submit for review when the course is complete");
        tip1.setStyle("-fx-text-fill: #4B5563; -fx-font-size: 13px;");
        tip2.setStyle("-fx-text-fill: #4B5563; -fx-font-size: 13px;");
        tips.getChildren().addAll(tipsTitle, tip1, tip2);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        Button myCoursesBtn = new Button("📚 View My Courses");
        myCoursesBtn.getStyleClass().addAll("btn-primary");
        myCoursesBtn.setOnAction(e -> {
            if (onGoToMyCourses != null) {
                onGoToMyCourses.run();
            }
        });

        Button createAnotherBtn = new Button("➕ Create Another Course");
        createAnotherBtn.getStyleClass().addAll("btn-outline");
        createAnotherBtn.setOnAction(e -> resetWizard());

        actions.getChildren().addAll(myCoursesBtn, createAnotherBtn);

        VBox wrapper = new VBox(24, icon, headline, courseName, message, tips, actions);
        wrapper.setAlignment(Pos.CENTER);

        successPane.getChildren().add(wrapper);

        StackPane centered = new StackPane(successPane);
        StackPane.setAlignment(successPane, Pos.CENTER);
        stepContainer.getChildren().setAll(centered);
        AnimationUtils.fadeIn(successPane, 350);
    }

    private void resetWizard() {
        titleField.clear();
        subtitleField.clear();
        categoryCombo.setValue(null);
        languageCombo.setValue("English");
        levelGroup.selectToggle(levelGroup.getToggles().get(0));
        priceField.clear();
        hoursField.clear();
        videoUrlField.clear();
        descriptionArea.clear();
        learnArea.clear();
        requirementsArea.clear();
        audienceArea.clear();
        certToggle.setSelected(false);
        quizToggle.setSelected(false);
        mentorToggle.setSelected(false);
        resourcesToggle.setSelected(false);
        thumbnailFile = null;
        thumbnailLabel.setText("No file selected");

        stepIndicator.setVisible(true);
        stepIndicator.setManaged(true);
        navButtons.setVisible(true);
        navButtons.setManaged(true);
        showStep(0);
    }

    // ==================== VALIDATION ====================

    private boolean validateStep1() {
        if (titleField.getText().isBlank()) { showError("Course title is required."); return false; }
        if (categoryCombo.getValue() == null) { showError("Please select a category."); return false; }
        if (priceField.getText().isBlank()) { showError("Price is required."); return false; }
        try { new BigDecimal(priceField.getText().trim()); } catch (Exception e) { showError("Invalid price."); return false; }
        return true;
    }

    private boolean validateStep2() {
        if (descriptionArea.getText().isBlank()) { showError("Description is required."); return false; }
        if (learnArea.getText().isBlank()) { showError("'What students will learn' is required."); return false; }
        return true;
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText("Validation");
        alert.showAndWait();
    }

    // ==================== HELPERS ====================

    private void chooseThumbnail() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Course Thumbnail");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File f = fc.showOpenDialog(stepContainer.getScene().getWindow());
        if (f != null) {
            thumbnailFile = f;
            thumbnailLabel.setText("✅ " + f.getName());
        }
    }

    private String getSelectedLevel() {
        RadioButton selected = (RadioButton) levelGroup.getSelectedToggle();
        return selected != null ? selected.getText() : "Beginner";
    }

    private TextField createStyledField(String label, String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private TextArea createStyledTextArea(String prompt, int rows) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefRowCount(rows);
        ta.setWrapText(true);
        ta.getStyleClass().add("text-area");
        return ta;
    }

    private VBox createFieldGroup(String label, javafx.scene.Node field) {
        VBox group = new VBox(4);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        group.getChildren().addAll(l, field);
        return group;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
