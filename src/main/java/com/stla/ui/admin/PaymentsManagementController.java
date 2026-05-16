package com.stla.ui.admin;

import com.stla.domain.models.Payment;
import com.stla.services.CourseService;
import com.stla.ui.components.ComponentFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PaymentsManagementController {
    @javafx.fxml.FXML private VBox paymentsContainer;
    private final CourseService service = new CourseService();

    @javafx.fxml.FXML public void initialize() {
        paymentsContainer.getChildren().add(ComponentFactory.createLoadingState());
        Task<List<Payment>> task = new Task<>() {
            @Override protected List<Payment> call() { return service.getAllPayments(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> build(task.getValue())));
        new Thread(task).start();
    }

    private void build(List<Payment> payments) {
        paymentsContainer.getChildren().clear();
        if (payments.isEmpty()) {
            paymentsContainer.getChildren().add(ComponentFactory.createEmptyState("💳", "No payments recorded"));
            return;
        }
        HBox header = new HBox(16);
        header.setPadding(new Insets(8, 16, 8, 16));
        header.getChildren().addAll(hdr("Student", 180), hdr("Course", 220), hdr("Amount", 90), hdr("Status", 90), hdr("Provider", 100), hdr("Date", 120));
        paymentsContainer.getChildren().add(header);

        for (Payment p : payments) {
            HBox row = new HBox(16);
            row.getStyleClass().add("card");
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(
                cell(p.getStudentName(), 180, true),
                cell(p.getCourseName(), 220, false),
                cell("$" + p.getAmount(), 90, true),
                wrap(ComponentFactory.createStatusBadge(p.getStatus().getValue(), p.getStatus().getValue()), 90),
                cell(p.getGatewayProvider() != null ? p.getGatewayProvider() : "—", 100, false),
                cell(p.getPaidAt() != null ? p.getPaidAt().format(DateTimeFormatter.ofPattern("MMM d, yy")) : "—", 120, false)
            );
            paymentsContainer.getChildren().add(row);
        }
    }

    private Label hdr(String t, double w) { Label l = new Label(t); l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B7280;"); l.setPrefWidth(w); return l; }
    private Label cell(String t, double w, boolean bold) { Label l = new Label(t != null ? t : "—"); l.setPrefWidth(w); if (bold) l.setStyle("-fx-font-weight: bold;"); return l; }
    private HBox wrap(javafx.scene.Node n, double w) { HBox b = new HBox(n); b.setPrefWidth(w); b.setAlignment(Pos.CENTER_LEFT); return b; }
}
