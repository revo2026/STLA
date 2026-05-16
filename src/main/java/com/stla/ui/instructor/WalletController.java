package com.stla.ui.instructor;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.*;
import com.stla.services.WalletService;
import com.stla.ui.components.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class WalletController {
    @javafx.fxml.FXML private VBox walletContainer;
    @javafx.fxml.FXML private Button btnWithdraw;
    private final WalletService walletService = new WalletService();
    private InstructorWallet wallet;
    private Runnable onOpenWithdraw;

    public void setOnOpenWithdraw(Runnable onOpenWithdraw) {
        this.onOpenWithdraw = onOpenWithdraw;
    }
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @javafx.fxml.FXML public void initialize() { loadWallet(); }

    private void loadWallet() {
        walletContainer.getChildren().clear();
        walletContainer.getChildren().add(ComponentFactory.createLoadingState());
        String iid = SessionManager.getInstance().getCurrentInstructor().getId();
        Task<Void> task = new Task<>() {
            InstructorWallet w;
            List<WalletTransaction> txns;
            List<WalletTransaction> withdrawals;
            Map<String, BigDecimal> monthly, courseRev;
            @Override protected Void call() {
                w = walletService.getWallet(iid).orElse(null);
                txns = walletService.getTransactions(iid);
                withdrawals = walletService.getWithdrawalHistory(iid);
                monthly = walletService.getMonthlyEarnings(iid);
                courseRev = walletService.getCourseRevenue(iid);
                return null;
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> { wallet = w; buildUI(w, txns, withdrawals, monthly, courseRev); });
            }
        };
        new Thread(task).start();
    }

    private void buildUI(InstructorWallet w, List<WalletTransaction> txns, List<WalletTransaction> withdrawals,
                         Map<String, BigDecimal> monthly, Map<String, BigDecimal> courseRev) {
        walletContainer.getChildren().clear();
        if (w == null) { walletContainer.getChildren().add(ComponentFactory.createEmptyState("💰", "Wallet not initialized yet")); btnWithdraw.setDisable(true); return; }
        HBox cards = new HBox(16);
        cards.setAlignment(Pos.CENTER_LEFT);
        cards.getChildren().addAll(
            summaryCard("💵", "Available Balance", "$" + w.getAvailableBalance(), "wallet-card-available"),
            summaryCard("📊", "Total Earned", "$" + w.getTotalEarned(), "wallet-card-earned"),
            summaryCard("💳", "Total Withdrawn", "$" + w.getTotalWithdrawn(), "wallet-card-withdrawn")
        );
        walletContainer.getChildren().add(cards);
        AnimationUtils.staggerFadeIn(cards, 80);

        // Charts
        HBox chartsRow = new HBox(20);
        if (!monthly.isEmpty()) {
            String[] labels = monthly.keySet().toArray(new String[0]);
            double[] vals = monthly.values().stream().mapToDouble(BigDecimal::doubleValue).toArray();
            VBox mc = ChartFactory.createAreaChart("Monthly Earnings", vals, labels);
            HBox.setHgrow(mc, Priority.ALWAYS); chartsRow.getChildren().add(mc);
        }
        if (!courseRev.isEmpty()) {
            String[] labels = courseRev.keySet().stream().map(t -> t.length() > 14 ? t.substring(0,14) + "…" : t).toArray(String[]::new);
            double[] vals = courseRev.values().stream().mapToDouble(BigDecimal::doubleValue).toArray();
            VBox bc = ChartFactory.createBarChart("Course Revenue", labels, vals);
            HBox.setHgrow(bc, Priority.ALWAYS); chartsRow.getChildren().add(bc);
        }
        if (!chartsRow.getChildren().isEmpty()) walletContainer.getChildren().add(chartsRow);

        walletContainer.getChildren().add(buildTransactionsSection(txns));
        walletContainer.getChildren().add(buildWithdrawalsSection(withdrawals));
    }

    // ==================== SUMMARY CARD ====================
    private VBox summaryCard(String icon, String label, String amount, String styleClass) {
        VBox card = new VBox(6); card.getStyleClass().addAll("wallet-summary-card", styleClass);
        card.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(card, Priority.ALWAYS);
        Label ic = new Label(icon); ic.getStyleClass().add("wallet-card-icon");
        Label lb = new Label(label); lb.getStyleClass().add("wallet-card-label");
        Label am = new Label(amount); am.getStyleClass().add("wallet-card-amount");
        card.getChildren().addAll(ic, lb, am);
        return card;
    }

    // ==================== TRANSACTIONS ====================
    private VBox buildTransactionsSection(List<WalletTransaction> txns) {
        VBox section = new VBox(0); section.getStyleClass().add("card"); section.setPadding(new Insets(0));
        Label hdr = new Label("📋 Transaction History"); hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#1F2937;-fx-padding:16 16 8 16;");
        section.getChildren().add(hdr);
        if (txns == null || txns.isEmpty()) {
            Label empty = new Label("No transactions yet"); empty.setStyle("-fx-text-fill:#6B7280;-fx-padding:20;");
            section.getChildren().add(empty); return section;
        }
        // Header
        HBox th = new HBox(); th.getStyleClass().add("txn-header");
        th.getChildren().addAll(thLbl("Date", 140), thLbl("Type", 100), thLbl("Amount", 100), thLbl("Status", 80), thLbl("Description", 200), thLbl("Balance", 120));
        section.getChildren().add(th);
        for (WalletTransaction t : txns) {
            HBox row = new HBox(); row.getStyleClass().add("txn-row"); row.setAlignment(Pos.CENTER_LEFT);
            Label date = new Label(t.getCreatedAt() != null ? t.getCreatedAt().format(dtf) : "—"); date.setPrefWidth(140); date.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");
            Label type = txnBadge(t.getType()); type.setPrefWidth(100);
            String sign = "earning".equals(t.getType()) ? "+" : "-";
            Label amt = new Label(sign + "$" + t.getAmount()); amt.setPrefWidth(100);
            amt.setStyle("earning".equals(t.getType()) ? "-fx-text-fill:#059669;-fx-font-weight:bold;" : "-fx-text-fill:#DC2626;-fx-font-weight:bold;");
            Label sts = new Label(t.getStatus() != null ? t.getStatus() : "—"); sts.setPrefWidth(80); sts.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;");
            Label desc = new Label(t.getDescription() != null ? t.getDescription() : "—"); desc.setPrefWidth(200); desc.setStyle("-fx-font-size:12px;-fx-text-fill:#374151;"); desc.setWrapText(true);
            String bal = "";
            if (t.getBalanceBefore() != null && t.getBalanceAfter() != null) bal = "$" + t.getBalanceBefore() + " → $" + t.getBalanceAfter();
            Label balL = new Label(bal); balL.setPrefWidth(120); balL.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
            row.getChildren().addAll(date, type, amt, sts, desc, balL);
            section.getChildren().add(row);
        }
        return section;
    }

    private Label thLbl(String t, double w) { Label l = new Label(t); l.getStyleClass().add("txn-header-label"); l.setPrefWidth(w); return l; }
    private Label txnBadge(String type) {
        Label l = new Label(type != null ? type.substring(0,1).toUpperCase() + type.substring(1) : "—");
        String sc = switch (type != null ? type : "") { case "earning" -> "txn-badge-earning"; case "withdrawal" -> "txn-badge-withdrawal"; default -> "txn-badge-adjustment"; };
        l.getStyleClass().add(sc); return l;
    }

    private VBox buildWithdrawalsSection(List<WalletTransaction> withdrawals) {
        VBox section = new VBox(12);
        Label hdr = new Label("💸 Withdrawal History");
        hdr.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#1F2937;");
        section.getChildren().add(hdr);
        if (withdrawals == null || withdrawals.isEmpty()) {
            section.getChildren().add(ComponentFactory.createEmptyState("💸", "No withdrawals yet"));
            return section;
        }
        for (WalletTransaction t : withdrawals) {
            HBox card = new HBox(16);
            card.getStyleClass().add("withdrawal-card");
            card.setAlignment(Pos.CENTER_LEFT);
            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label amt = new Label("-$" + t.getAmount());
            amt.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#DC2626;");
            Label desc = new Label(t.getDescription() != null ? t.getDescription() : "Withdrawal");
            desc.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;");
            desc.setWrapText(true);
            Label dt = new Label(t.getCreatedAt() != null ? t.getCreatedAt().format(dtf) : "");
            dt.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;");
            info.getChildren().addAll(amt, desc, dt);
            Label badge = new Label("Completed");
            badge.getStyleClass().add("wd-badge-completed");
            card.getChildren().addAll(info, badge);
            section.getChildren().add(card);
        }
        return section;
    }

    @javafx.fxml.FXML
    private void handleWithdraw() {
        if (onOpenWithdraw != null) {
            onOpenWithdraw.run();
        }
    }
}
