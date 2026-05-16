package com.stla.ui.admin;

import com.stla.domain.models.AdminWallet;
import com.stla.domain.models.AdminWalletTransaction;
import com.stla.domain.models.WalletTransaction;
import com.stla.services.WalletService;
import com.stla.ui.components.ComponentFactory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminWalletController {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @FXML private FlowPane statsRow;
    @FXML private TableView<AdminWalletTransaction> transactionsTable;
    @FXML private TableView<WalletTransaction> withdrawalsTable;
    @FXML private VBox rootBox;

    private final WalletService walletService = new WalletService();

    @FXML
    public void initialize() {
        setupPaymentTable();
        setupWithdrawalsTable();
        loadData();
    }

    private void setupPaymentTable() {
        transactionsTable.getColumns().clear();

        TableColumn<AdminWalletTransaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setPrefWidth(160);
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(
                formatTxnType(cd.getValue() != null ? cd.getValue().getTransactionType() : null)));

        TableColumn<AdminWalletTransaction, String> amountCol = new TableColumn<>("Amount");
        amountCol.setPrefWidth(100);
        amountCol.setCellValueFactory(cd -> new SimpleStringProperty(
                money(cd.getValue() != null ? cd.getValue().getAmount() : null)));
        amountCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<AdminWalletTransaction, String> noteCol = new TableColumn<>("Note");
        noteCol.setPrefWidth(280);
        noteCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue() != null && cd.getValue().getNote() != null ? cd.getValue().getNote() : "—"));

        TableColumn<AdminWalletTransaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setPrefWidth(160);
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue() != null && cd.getValue().getCreatedAt() != null
                        ? cd.getValue().getCreatedAt().format(DTF) : "—"));

        transactionsTable.getColumns().addAll(typeCol, amountCol, noteCol, dateCol);
        transactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void setupWithdrawalsTable() {
        withdrawalsTable.getColumns().clear();

        TableColumn<WalletTransaction, String> nameCol = new TableColumn<>("Instructor");
        nameCol.setPrefWidth(140);
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue() != null && cd.getValue().getInstructorName() != null
                        ? cd.getValue().getInstructorName() : "—"));

        TableColumn<WalletTransaction, String> emailCol = new TableColumn<>("Email");
        emailCol.setPrefWidth(160);
        emailCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue() != null && cd.getValue().getInstructorEmail() != null
                        ? cd.getValue().getInstructorEmail() : "—"));

        TableColumn<WalletTransaction, String> amountCol = new TableColumn<>("Amount");
        amountCol.setPrefWidth(100);
        amountCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue() != null && cd.getValue().getAmount() != null
                        ? "-" + money(cd.getValue().getAmount()) : "—"));
        amountCol.setCellFactory(withdrawalAmountCellFactory());

        TableColumn<WalletTransaction, String> noteCol = new TableColumn<>("Details");
        noteCol.setPrefWidth(220);
        noteCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue() != null && cd.getValue().getDescription() != null
                        ? cd.getValue().getDescription() : "—"));

        TableColumn<WalletTransaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setPrefWidth(150);
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue() != null && cd.getValue().getCreatedAt() != null
                        ? cd.getValue().getCreatedAt().format(DTF) : "—"));

        withdrawalsTable.getColumns().addAll(nameCol, emailCol, amountCol, noteCol, dateCol);
        withdrawalsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private Callback<TableColumn<WalletTransaction, String>, TableCell<WalletTransaction, String>> withdrawalAmountCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                }
            }
        };
    }

    private void loadData() {
        statsRow.getChildren().clear();
        statsRow.getChildren().add(ComponentFactory.createLoadingState());

        Task<Void> task = new Task<>() {
            private AdminWallet wallet;
            private List<AdminWalletTransaction> paymentTxns;
            private List<WalletTransaction> withdrawalTxns;
            private BigDecimal instructorEarningsPaid;
            private BigDecimal instructorWithdrawn;
            private BigDecimal adminBalance;

            @Override protected Void call() {
                wallet = walletService.getAdminWallet().orElse(null);
                paymentTxns = walletService.getAdminTransactions();
                withdrawalTxns = walletService.getAllWithdrawalHistory();
                instructorEarningsPaid = walletService.getTotalInstructorEarningsPaid();
                instructorWithdrawn = walletService.getTotalInstructorWithdrawn();
                adminBalance = walletService.getAdminSpendableBalance(wallet);
                return null;
            }

            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    statsRow.getChildren().clear();
                    if (wallet != null) {
                        statsRow.getChildren().addAll(
                            ComponentFactory.createStatCard("💰", "Gross Sales", money(wallet.getTotalRevenue()), false),
                            ComponentFactory.createStatCard("📊", "Platform Commission", money(wallet.getTotalCommissions()), false),
                            ComponentFactory.createStatCard("🏦", "Admin Balance", money(adminBalance), false),
                            ComponentFactory.createStatCard("👨‍🏫", "Paid to Instructors", money(instructorEarningsPaid), false),
                            ComponentFactory.createStatCard("💸", "Instructor Withdrawals", money(instructorWithdrawn), false)
                        );
                    } else {
                        statsRow.getChildren().add(new Label(
                                "Admin wallet not configured. Run payment_enrollment_wallet_migration.sql"));
                    }
                    transactionsTable.getItems().setAll(paymentTxns != null ? paymentTxns : List.of());
                    withdrawalsTable.getItems().setAll(withdrawalTxns != null ? withdrawalTxns : List.of());
                    withdrawalsTable.setPlaceholder(new Label("No instructor withdrawals recorded yet."));
                });
            }

            @Override protected void failed() {
                Platform.runLater(() -> {
                    statsRow.getChildren().clear();
                    statsRow.getChildren().add(new Label("Failed to load admin wallet data."));
                });
            }
        };
        new Thread(task).start();
    }

    private static String money(BigDecimal v) {
        if (v == null) return "$0.00";
        return "$" + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatTxnType(String type) {
        if (type == null) return "—";
        return type.replace('_', ' ');
    }
}
