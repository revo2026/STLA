package com.stla.ui.admin;

import com.stla.domain.models.WalletTransaction;
import com.stla.services.WalletService;
import com.stla.ui.components.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin view: instructor withdrawal history only (read-only, no approve/reject).
 */
public class WithdrawalsController {

    @javafx.fxml.FXML private VBox withdrawalsContainer;
    @javafx.fxml.FXML private TextField searchField;

    private final WalletService walletService = new WalletService();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private List<WalletTransaction> allHistory = List.of();

    @javafx.fxml.FXML
    public void initialize() {
        if (searchField != null) {
            searchField.textProperty().addListener((o, ov, nv) -> build(filtered()));
        }
        load();
    }

    @javafx.fxml.FXML
    private void handleRefresh() {
        load();
    }

    private void load() {
        withdrawalsContainer.getChildren().clear();
        withdrawalsContainer.getChildren().add(ComponentFactory.createLoadingState());
        Task<List<WalletTransaction>> task = new Task<>() {
            @Override protected List<WalletTransaction> call() {
                return walletService.getAllWithdrawalHistory();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            allHistory = task.getValue() != null ? task.getValue() : List.of();
            build(filtered());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            withdrawalsContainer.getChildren().clear();
            withdrawalsContainer.getChildren().add(ComponentFactory.createEmptyState("⚠️", "Failed to load withdrawal history"));
        }));
        new Thread(task).start();
    }

    private List<WalletTransaction> filtered() {
        String query = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase() : "";
        if (query.isEmpty()) return allHistory;
        return allHistory.stream().filter(t -> {
            String name = t.getInstructorName() != null ? t.getInstructorName().toLowerCase() : "";
            String email = t.getInstructorEmail() != null ? t.getInstructorEmail().toLowerCase() : "";
            return name.contains(query) || email.contains(query);
        }).collect(Collectors.toList());
    }

    private void build(List<WalletTransaction> history) {
        withdrawalsContainer.getChildren().clear();

        Label info = new Label("View-only: instructors withdraw instantly — no approval required.");
        info.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px; -fx-padding: 0 0 12 0;");
        withdrawalsContainer.getChildren().add(info);

        if (history.isEmpty()) {
            withdrawalsContainer.getChildren().add(ComponentFactory.createEmptyState("💸",
                    "No instructor withdrawals yet"));
            return;
        }

        HBox header = new HBox(12);
        header.setPadding(new Insets(8, 16, 8, 16));
        header.getChildren().addAll(
                hdr("Instructor", 150), hdr("Email", 170), hdr("Amount", 90),
                hdr("Status", 80), hdr("Date", 140), hdr("Details", 220));
        withdrawalsContainer.getChildren().add(header);

        for (WalletTransaction t : history) {
            HBox row = new HBox(12);
            row.getStyleClass().add("card");
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setAlignment(Pos.CENTER_LEFT);

            Label name = cell(t.getInstructorName() != null ? t.getInstructorName() : "—", 150, true);
            Label email = cell(t.getInstructorEmail() != null ? t.getInstructorEmail() : "—", 170, false);
            email.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
            Label amount = cell("-$" + t.getAmount(), 90, true);
            amount.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            Label status = cell(t.getStatus() != null ? t.getStatus() : "completed", 80, false);
            status.getStyleClass().add("wd-badge-completed");
            Label date = cell(t.getCreatedAt() != null ? t.getCreatedAt().format(dtf) : "—", 140, false);
            date.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
            Label desc = cell(t.getDescription() != null ? t.getDescription() : "—", 220, false);
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 11px;");

            row.getChildren().addAll(name, email, amount, status, date, desc);
            withdrawalsContainer.getChildren().add(row);
        }
        AnimationUtils.staggerFadeIn(withdrawalsContainer, 50);
    }

    private Label cell(String text, double width, boolean bold) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        if (bold) l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private Label hdr(String t, double w) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        l.setPrefWidth(w);
        return l;
    }
}
