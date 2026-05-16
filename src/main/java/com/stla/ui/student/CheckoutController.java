package com.stla.ui.student;

import com.stla.core.navigation.StudentNavigationContext;
import com.stla.core.session.SessionManager;
import com.stla.domain.models.Course;
import com.stla.patterns.facade.EnrollmentFacade;
import com.stla.patterns.strategy.PaymentStrategy;
import com.stla.patterns.strategy.PaymentStrategyFactory;
import com.stla.services.CourseDetailsService;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class CheckoutController {

    @FXML private VBox rootBox;
    @FXML private Label courseTitleLabel;
    @FXML private Label instructorLabel;
    @FXML private Label originalPriceLabel;
    @FXML private Label finalPriceLabel;
    @FXML private VBox visaForm;
    @FXML private VBox walletForm;
    @FXML private VBox visaMethodCard;
    @FXML private VBox walletMethodCard;
    @FXML private TextField cardholderField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private Label cardPreviewNumber;
    @FXML private Label cardPreviewName;
    @FXML private ComboBox<String> walletProviderCombo;
    @FXML private TextField walletIdentifierField;
    @FXML private Label errorLabel;
    @FXML private VBox loadingOverlay;
    @FXML private Button payButton;

    private final CourseDetailsService detailsService = new CourseDetailsService();
    private final EnrollmentFacade enrollmentFacade = new EnrollmentFacade();
    private String courseId;
    private Course course;
    private String selectedMethod = "visa";
    private Consumer<EnrollmentFacade.PurchaseResult> onSuccess;

    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setOnSuccess(Consumer<EnrollmentFacade.PurchaseResult> onSuccess) { this.onSuccess = onSuccess; }

    @FXML
    public void initialize() {
        if (courseId == null) courseId = StudentNavigationContext.getSelectedCourseId();
        walletProviderCombo.getItems().addAll("Vodafone Cash", "Orange Cash", "Etisalat Cash", "PayPal", "Fawry", "InstaPay");
        walletProviderCombo.setValue("Vodafone Cash");
        selectVisa();
        setupCardPreview();
        setupExpiryAndCvvFields();
        loadCourseSummary();
    }

    /**
     * Expiry: digits only, max 4 → display as MM/YY with "/" inserted after the month (2 digits).
     * CVV: digits only, max 3 characters.
     */
    private void setupExpiryAndCvvFields() {
        if (expiryField != null) {
            expiryField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) return;
                String digits = newVal.replaceAll("\\D", "");
                if (digits.length() > 4) {
                    digits = digits.substring(0, 4);
                }
                String formatted;
                if (digits.length() >= 3) {
                    formatted = digits.substring(0, 2) + "/" + digits.substring(2);
                } else if (digits.length() == 2) {
                    formatted = digits + "/";
                } else {
                    formatted = digits;
                }
                if (!formatted.equals(newVal)) {
                    expiryField.setText(formatted);
                    expiryField.end();
                }
            });
        }
        if (cvvField != null) {
            Pattern cvvPattern = Pattern.compile("\\d{0,3}");
            cvvField.setTextFormatter(new TextFormatter<>(change -> {
                String proposed = change.getControlNewText();
                return cvvPattern.matcher(proposed).matches() ? change : null;
            }));
        }
    }

    private void setupCardPreview() {
        cardNumberField.textProperty().addListener((o, ov, nv) -> {
            String digits = nv.replaceAll("\\D", "");
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length() && i < 16; i++) {
                if (i > 0 && i % 4 == 0) formatted.append(' ');
                formatted.append(digits.charAt(i));
            }
            if (!formatted.toString().equals(nv)) cardNumberField.setText(formatted.toString());
            cardPreviewNumber.setText(formatted.length() >= 4 ? "**** " + formatted.substring(Math.max(0, formatted.length() - 4)) : "**** **** **** ****");
        });
        cardholderField.textProperty().addListener((o, ov, nv) -> cardPreviewName.setText(nv.isBlank() ? "CARDHOLDER NAME" : nv.toUpperCase()));
    }

    private void loadCourseSummary() {
        String studentId = SessionManager.getInstance().getCurrentStudent().getId();
        Task<com.stla.domain.models.CourseDetailsView> task = new Task<>() {
            @Override protected com.stla.domain.models.CourseDetailsView call() {
                return detailsService.getCourseDetails(courseId, studentId).orElse(null);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            var view = task.getValue();
            if (view == null) return;
            course = view.getCourse();
            courseTitleLabel.setText(course.getTitle());
            instructorLabel.setText(view.getInstructorName() != null ? view.getInstructorName() : "Instructor");
            BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
            originalPriceLabel.setText("$" + price);
            finalPriceLabel.setText("$" + price);
            payButton.setText("Pay $" + price + " Securely");
        }));
        new Thread(task).start();
    }

    @FXML private void selectVisa() {
        selectedMethod = "visa";
        visaForm.setVisible(true);
        visaForm.setManaged(true);
        walletForm.setVisible(false);
        walletForm.setManaged(false);
        visaMethodCard.getStyleClass().remove("payment-method-selected");
        walletMethodCard.getStyleClass().remove("payment-method-selected");
        visaMethodCard.getStyleClass().add("payment-method-selected");
    }

    @FXML private void selectWallet() {
        selectedMethod = "wallet";
        visaForm.setVisible(false);
        visaForm.setManaged(false);
        walletForm.setVisible(true);
        walletForm.setManaged(true);
        visaMethodCard.getStyleClass().remove("payment-method-selected");
        walletMethodCard.getStyleClass().remove("payment-method-selected");
        walletMethodCard.getStyleClass().add("payment-method-selected");
    }

    @FXML
    private void handlePay() {
        errorLabel.setText("");
        if (course == null || courseId == null) {
            errorLabel.setText("Course not loaded.");
            return;
        }

        PaymentStrategy strategy;
        String paymentMethod;
        String walletProvider = null;

        if ("visa".equals(selectedMethod)) {
            strategy = PaymentStrategyFactory.visa(
                    cardholderField.getText(), cardNumberField.getText(), expiryField.getText(), cvvField.getText());
            paymentMethod = "Visa";
            if (!strategy.validatePaymentDetails()) {
                errorLabel.setText("Please check your card details.");
                return;
            }
        } else {
            walletProvider = walletProviderCombo.getValue();
            strategy = PaymentStrategyFactory.digitalWallet(walletProvider, walletIdentifierField.getText());
            paymentMethod = "Digital Wallet";
            if (!strategy.validatePaymentDetails()) {
                errorLabel.setText("Please enter a valid wallet provider and number/email.");
                return;
            }
        }

        String studentId = SessionManager.getInstance().getCurrentStudent().getId();
        BigDecimal amount = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        String finalWalletProvider = walletProvider;

        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
        payButton.setDisable(true);

        Task<EnrollmentFacade.PurchaseResult> task = new Task<>() {
            @Override protected EnrollmentFacade.PurchaseResult call() {
                return enrollmentFacade.purchaseCourse(studentId, courseId, amount, strategy, paymentMethod, finalWalletProvider);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> finishPay(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            loadingOverlay.setVisible(false);
            loadingOverlay.setManaged(false);
            payButton.setDisable(false);
            errorLabel.setText("Payment failed. Please try again.");
        }));
        new Thread(task).start();
    }

    private void finishPay(EnrollmentFacade.PurchaseResult result) {
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
        payButton.setDisable(false);
        if (result.success()) {
            StudentNavigationContext.setLastPurchaseResult(result);
            if (onSuccess != null) onSuccess.accept(result);
        } else {
            errorLabel.setText(result.message());
            ToastNotification.show((Stage) rootBox.getScene().getWindow(), result.message(), ToastNotification.Type.ERROR);
        }
    }

    @FXML private void handleCancel() {
        if (rootBox.getScene() != null) {
            // Parent handles back navigation
        }
    }
}
