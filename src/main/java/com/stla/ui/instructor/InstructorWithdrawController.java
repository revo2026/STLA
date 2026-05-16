package com.stla.ui.instructor;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.InstructorWallet;
import com.stla.domain.models.WalletTransaction;
import com.stla.patterns.strategy.WithdrawStrategy;
import com.stla.patterns.strategy.WithdrawStrategyFactory;
import com.stla.services.WalletService;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class InstructorWithdrawController {

    @FXML private Label availableBalanceLabel;
    @FXML private Label balanceCardAmount;
    @FXML private Label totalWithdrawnLabel;
    @FXML private TextField amountField;
    @FXML private VBox visaMethodCard;
    @FXML private VBox walletMethodCard;
    @FXML private VBox paymentDetailsContainer;
    @FXML private VBox visaFormBox;
    @FXML private VBox walletFormBox;
    @FXML private TextField cardholderNameField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryDateField;
    @FXML private TextField cvvField;
    @FXML private ComboBox<String> walletProviderCombo;
    @FXML private TextField walletPhoneField;
    @FXML private TextField walletAccountNameField;
    @FXML private TextArea notesField;
    @FXML private Label errorLabel;
    @FXML private Button submitBtn;
    @FXML private Button cancelBtn;
    @FXML private Button refreshHistoryBtn;
    @FXML private ProgressIndicator submitLoading;
    @FXML private VBox historyContainer;

    private static final Pattern CARD_DIGITS = Pattern.compile("^[0-9]{13,19}$");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])/([0-9]{2})$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3,4}$");

    private final WalletService walletService = new WalletService();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private InstructorWallet wallet;
    private String selectedMethod = "visa";

    @FXML
    public void initialize() {
        setupWalletProviders();
        setupCardAndExpiryFields();
        handleSelectVisa();
        loadWallet();
    }

    private void setupWalletProviders() {
        walletProviderCombo.getItems().setAll(
                "Vodafone Cash",
                "Orange Cash",
                "Etisalat Cash",
                "WE Pay",
                "InstaPay");
        walletProviderCombo.getSelectionModel().selectFirst();
    }

    private void setupCardAndExpiryFields() {
        if (expiryDateField != null) {
            expiryDateField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    return;
                }
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
                    expiryDateField.setText(formatted);
                    expiryDateField.end();
                }
            });
        }
        if (cvvField != null) {
            Pattern cvvPattern = Pattern.compile("\\d{0,4}");
            cvvField.setTextFormatter(new TextFormatter<>(change -> {
                String proposed = change.getControlNewText();
                return cvvPattern.matcher(proposed).matches() ? change : null;
            }));
        }
        if (cardNumberField != null) {
            cardNumberField.textProperty().addListener((o, ov, nv) -> {
                if (nv == null) {
                    return;
                }
                String digits = nv.replaceAll("\\D", "");
                if (digits.length() > 19) {
                    digits = digits.substring(0, 19);
                }
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(' ');
                    }
                    formatted.append(digits.charAt(i));
                }
                String f = formatted.toString();
                if (!f.equals(nv)) {
                    cardNumberField.setText(f);
                    cardNumberField.end();
                }
            });
        }
    }

    @FXML
    private void handleSelectVisa() {
        selectedMethod = "visa";
        togglePaymentForms(true);
        walletMethodCard.getStyleClass().remove("method-card-active");
        visaMethodCard.getStyleClass().remove("method-card-active");
        visaMethodCard.getStyleClass().add("method-card-active");
    }

    @FXML
    private void handleSelectWallet() {
        selectedMethod = "digital_wallet";
        togglePaymentForms(false);
        visaMethodCard.getStyleClass().remove("method-card-active");
        walletMethodCard.getStyleClass().remove("method-card-active");
        walletMethodCard.getStyleClass().add("method-card-active");
    }

    private void togglePaymentForms(boolean visaSelected) {
        visaFormBox.setVisible(visaSelected);
        visaFormBox.setManaged(visaSelected);
        walletFormBox.setVisible(!visaSelected);
        walletFormBox.setManaged(!visaSelected);
    }

    @FXML
    private void handleClear() {
        amountField.clear();
        cardholderNameField.clear();
        cardNumberField.clear();
        expiryDateField.clear();
        cvvField.clear();
        walletPhoneField.clear();
        walletAccountNameField.clear();
        walletProviderCombo.getSelectionModel().selectFirst();
        notesField.clear();
        hideError();
    }

    @FXML
    private void handleRefresh() {
        loadWallet();
    }

    @FXML
    private void handleSubmit() {
        if (wallet == null) {
            showError("Wallet not initialized.");
            return;
        }
        hideError();
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            String details;
            if ("visa".equals(selectedMethod)) {
                String fieldErr = validateVisaFields();
                if (fieldErr != null) {
                    showError(fieldErr);
                    return;
                }
                details = buildVisaDisplayDetails();
            } else {
                String fieldErr = validateWalletFields();
                if (fieldErr != null) {
                    showError(fieldErr);
                    return;
                }
                details = buildWalletDisplayDetails();
            }

            WithdrawStrategy strategy = WithdrawStrategyFactory.getStrategy(selectedMethod);
            String valErr = strategy.validate(details);
            if (valErr != null) {
                showError(valErr);
                return;
            }

            submitLoading.setVisible(true);
            submitLoading.setManaged(true);
            submitBtn.setDisable(true);

            new Thread(() -> {
                try {
                    walletService.withdrawNow(
                            SessionManager.getInstance().getCurrentInstructor().getId(),
                            amount, selectedMethod, details);
                    Platform.runLater(() -> {
                        submitLoading.setVisible(false);
                        submitLoading.setManaged(false);
                        submitBtn.setDisable(false);
                        handleClear();
                        toast("Withdrawal completed successfully!", ToastNotification.Type.SUCCESS);
                        loadWallet();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        submitLoading.setVisible(false);
                        submitLoading.setManaged(false);
                        submitBtn.setDisable(false);
                        showError(ex.getMessage() != null ? ex.getMessage() : "Withdrawal failed");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            showError("Enter a valid amount.");
        }
    }

    private String validateVisaFields() {
        String name = cardholderNameField.getText() != null ? cardholderNameField.getText().trim() : "";
        String pan = cardNumberField.getText() != null ? cardNumberField.getText().replaceAll("\\D", "") : "";
        String exp = expiryDateField.getText() != null ? expiryDateField.getText().trim() : "";
        String cvv = cvvField.getText() != null ? cvvField.getText().trim() : "";

        if (name.isBlank()) {
            return "Cardholder name is required.";
        }
        if (!CARD_DIGITS.matcher(pan).matches()) {
            return "Enter a valid card number (13–19 digits).";
        }
        if (!EXPIRY_PATTERN.matcher(exp).matches()) {
            return "Enter a valid expiry date (MM/YY).";
        }
        if (!expiryYearMonthNotInPast(exp)) {
            return "Card has expired.";
        }
        if (!CVV_PATTERN.matcher(cvv).matches()) {
            return "Enter a valid CVV (3 or 4 digits).";
        }
        return null;
    }

    private boolean expiryYearMonthNotInPast(String expiryDate) {
        try {
            String[] parts = expiryDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = 2000 + Integer.parseInt(parts[1]);
            YearMonth exp = YearMonth.of(year, month);
            return !exp.isBefore(YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }

    private String buildVisaDisplayDetails() {
        String name = cardholderNameField.getText().trim();
        String pan = cardNumberField.getText().replaceAll("\\D", "");
        String last4 = pan.length() >= 4 ? pan.substring(pan.length() - 4) : pan;
        String exp = expiryDateField.getText().trim();
        return name + " • ****" + last4 + " • exp " + exp;
    }

    private String validateWalletFields() {
        String provider = walletProviderCombo.getValue();
        if (provider == null || provider.isBlank()) {
            return "Select a wallet provider.";
        }
        String phone = walletPhoneField.getText() != null
                ? walletPhoneField.getText().trim().replaceAll("[^0-9+]", "")
                : "";
        if (phone.length() < 6) {
            return "Enter a valid mobile number.";
        }
        return null;
    }

    private String buildWalletDisplayDetails() {
        String provider = walletProviderCombo.getValue() != null ? walletProviderCombo.getValue().trim() : "";
        String phone = walletPhoneField.getText() != null ? walletPhoneField.getText().trim() : "";
        String account = walletAccountNameField.getText() != null ? walletAccountNameField.getText().trim() : "";
        StringBuilder sb = new StringBuilder(provider).append(" • ").append(phone);
        if (!account.isEmpty()) {
            sb.append(" • ").append(account);
        }
        return sb.toString();
    }

    private void loadWallet() {
        historyContainer.getChildren().clear();
        historyContainer.getChildren().add(ComponentFactory.createLoadingState());

        String instructorId = SessionManager.getInstance().getCurrentInstructor().getId();
        Task<WalletData> task = new Task<>() {
            InstructorWallet w;
            List<WalletTransaction> withdrawals;

            @Override protected WalletData call() {
                w = walletService.getWallet(instructorId).orElse(null);
                withdrawals = walletService.getWithdrawalHistory(instructorId);
                return new WalletData(w, withdrawals);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            WalletData data = task.getValue();
            wallet = data.wallet();
            updateBalanceLabels();
            buildHistory(data.withdrawals());
            submitBtn.setDisable(wallet == null);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            historyContainer.getChildren().clear();
            historyContainer.getChildren().add(ComponentFactory.createEmptyState("⚠️", "Failed to load wallet"));
        }));
        new Thread(task).start();
    }

    private void updateBalanceLabels() {
        if (wallet == null) {
            availableBalanceLabel.setText("Available: $0.00");
            balanceCardAmount.setText("$0.00");
            totalWithdrawnLabel.setText("$0.00");
            return;
        }
        String avail = "$" + wallet.getAvailableBalance();
        String withdrawn = "$" + wallet.getTotalWithdrawn();
        availableBalanceLabel.setText("Available: " + avail);
        balanceCardAmount.setText(avail);
        totalWithdrawnLabel.setText(withdrawn);
    }

    private void buildHistory(List<WalletTransaction> withdrawals) {
        historyContainer.getChildren().clear();
        if (withdrawals == null || withdrawals.isEmpty()) {
            historyContainer.getChildren().add(ComponentFactory.createEmptyState("💸", "No withdrawals yet"));
            return;
        }
        for (WalletTransaction t : withdrawals) {
            HBox card = new HBox(16);
            card.getStyleClass().add("withdrawal-card");
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 18, 14, 18));

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label amt = new Label("-$" + t.getAmount());
            amt.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
            Label desc = new Label(t.getDescription() != null ? t.getDescription() : "Withdrawal");
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
            desc.setWrapText(true);
            Label dt = new Label(t.getCreatedAt() != null ? t.getCreatedAt().format(dtf) : "");
            dt.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
            info.getChildren().addAll(amt, desc, dt);

            Label badge = new Label("Completed");
            badge.getStyleClass().add("wd-badge-completed");
            card.getChildren().addAll(info, badge);
            historyContainer.getChildren().add(card);
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void toast(String msg, ToastNotification.Type type) {
        try {
            if (historyContainer.getScene() != null) {
                ToastNotification.show((Stage) historyContainer.getScene().getWindow(), msg, type);
            }
        } catch (Exception ignored) {}
    }

    private record WalletData(InstructorWallet wallet, List<WalletTransaction> withdrawals) {}
}
