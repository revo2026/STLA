package com.stla.ui.student;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.IssuedCertificate;
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

public class CertificatesController {
    @javafx.fxml.FXML private VBox certsContainer;
    private final CourseService service = new CourseService();

    @javafx.fxml.FXML public void initialize() {
        certsContainer.getChildren().add(ComponentFactory.createLoadingState());
        Task<List<IssuedCertificate>> task = new Task<>() {
            @Override protected List<IssuedCertificate> call() {
                return service.getStudentCertificates(SessionManager.getInstance().getCurrentStudent().getId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> build(task.getValue())));
        new Thread(task).start();
    }

    private void build(List<IssuedCertificate> certs) {
        certsContainer.getChildren().clear();
        if (certs.isEmpty()) {
            certsContainer.getChildren().add(ComponentFactory.createEmptyState("🏆", "No certificates yet. Complete a course to earn one!"));
            return;
        }
        for (IssuedCertificate c : certs) {
            HBox card = new HBox(16);
            card.getStyleClass().add("card");
            card.setPadding(new Insets(20));
            card.setAlignment(Pos.CENTER_LEFT);

            VBox icon = new VBox();
            icon.setPrefSize(64, 64);
            icon.setStyle("-fx-background-color: linear-gradient(to bottom right, #F59E0B, #D97706); -fx-background-radius: 12;");
            icon.setAlignment(Pos.CENTER);
            icon.getChildren().add(new Label("🏆") {{ setStyle("-fx-font-size: 28px;"); }});

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);
            info.getChildren().addAll(
                new Label(c.getCourseName()) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 15px;"); }},
                new Label("Certificate #" + c.getCertificateNo()) {{ getStyleClass().add("text-secondary"); }},
                new Label("Issued: " + (c.getIssuedAt() != null ? c.getIssuedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "N/A")) {{ getStyleClass().add("text-secondary"); }}
            );

            Button dl = new Button("📥 Download");
            dl.getStyleClass().addAll("btn-outline", "btn-sm");
            card.getChildren().addAll(icon, info, dl);
            certsContainer.getChildren().add(card);
        }
    }
}
