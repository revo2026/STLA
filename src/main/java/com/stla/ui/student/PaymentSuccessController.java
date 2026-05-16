package com.stla.ui.student;

import com.stla.core.navigation.StudentNavigationContext;
import com.stla.core.session.SessionManager;
import com.stla.domain.models.Payment;
import com.stla.patterns.facade.EnrollmentFacade;
import com.stla.ui.components.AnimationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class PaymentSuccessController {

    @FXML private VBox successRoot;
    @FXML private Label courseLabel;
    @FXML private Label amountLabel;
    @FXML private Label methodLabel;
    @FXML private Label txnLabel;
    @FXML private Label dateLabel;
    @FXML private Label studentLabel;

    private Consumer<String> onContinueLearning;

    public void setOnContinueLearning(Consumer<String> handler) { this.onContinueLearning = handler; }

    @FXML
    public void initialize() {
        EnrollmentFacade.PurchaseResult result = StudentNavigationContext.getLastPurchaseResult();
        studentLabel.setText(SessionManager.getInstance().getCurrentUserName());
        if (result != null && result.payment() != null) {
            Payment p = result.payment();
            courseLabel.setText(p.getCourseName() != null ? p.getCourseName() : "Your course");
            amountLabel.setText("$" + p.getAmount());
            methodLabel.setText(p.getMethodType() != null ? p.getMethodType() : p.getGatewayProvider());
            txnLabel.setText(p.getGatewayTransactionId() != null ? p.getGatewayTransactionId() : p.getPaymentReference());
            if (p.getPaidAt() != null) {
                dateLabel.setText(p.getPaidAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            }
        }
        AnimationUtils.fadeIn(successRoot, 500);
    }

    @FXML
    private void handleContinue() {
        String courseId = StudentNavigationContext.getSelectedCourseId();
        if (onContinueLearning != null && courseId != null) {
            onContinueLearning.accept(courseId);
        }
    }
}
