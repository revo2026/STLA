package com.stla.ui.admin;

import com.stla.core.navigation.NavigationManager;
import com.stla.core.session.SessionManager;
import com.stla.domain.models.Instructor;
import com.stla.domain.models.Profile;
import com.stla.services.AdminService;
import com.stla.ui.components.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;

public class InstructorVerificationController {

    @javafx.fxml.FXML private VBox requestsContainer;
    @javafx.fxml.FXML private Label countLabel;

    private final AdminService adminService = new AdminService();

    @javafx.fxml.FXML
    public void initialize() {
        loadRequests();
    }

    private void loadRequests() {
        requestsContainer.getChildren().clear();
        requestsContainer.getChildren().add(ComponentFactory.createLoadingState());

        Task<List<Instructor>> task = new Task<>() {
            @Override protected List<Instructor> call() {
                return adminService.getPendingInstructorVerifications();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> buildList(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            requestsContainer.getChildren().clear();
            requestsContainer.getChildren().add(ComponentFactory.createEmptyState("❌", "Error loading verifications"));
        }));
        new Thread(task).start();
    }

    private void buildList(List<Instructor> instructors) {
        requestsContainer.getChildren().clear();
        countLabel.setText(instructors.size() + " pending verification request(s)");

        if (instructors.isEmpty()) {
            requestsContainer.getChildren().add(ComponentFactory.createEmptyState("✅", "No pending verification requests"));
            return;
        }

        for (Instructor inst : instructors) {
            requestsContainer.getChildren().add(buildRequestCard(inst));
        }
        AnimationUtils.staggerFadeIn(requestsContainer, 80);
    }

    private VBox buildRequestCard(Instructor inst) {
        Profile p = inst.getProfile();
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));

        // Header row
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        CircularAvatar avatar = new CircularAvatar(p != null ? p.getAvatarUrl() : null, p != null ? p.getFullName() : "?", 24);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel = new Label(p != null ? p.getFullName() : "Unknown");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label emailLabel = new Label(p != null ? p.getEmail() : "");
        emailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B7280;");
        info.getChildren().addAll(nameLabel, emailLabel);

        Label badge = ComponentFactory.createStatusBadge("PENDING", "pending");
        header.getChildren().addAll(avatar, info, badge);

        // Details grid
        HBox details = new HBox(24);
        details.setStyle("-fx-padding: 8 0 0 0;");
        details.getChildren().addAll(
            createDetailItem("💼 Title", inst.getTitle()),
            createDetailItem("🎯 Expertise", inst.getExpertiseTags() != null ? String.join(", ", inst.getExpertiseTags()) : "—"),
            createDetailItem("📅 Experience", inst.getYearsExperience() != null ? inst.getYearsExperience() + " years" : "—"),
            createDetailItem("🌍 Country", p != null && p.getCountry() != null ? p.getCountry() : "—")
        );

        // Documents section
        Label docsHeader = new Label("📄 Verification Documents");
        docsHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 0 4 0;");

        HBox docsRow = new HBox(16);
        docsRow.setStyle("-fx-padding: 4 0 8 0;");
        docsRow.setAlignment(Pos.CENTER_LEFT);
        docsRow.getChildren().addAll(
            createDocThumb("🪪 ID Front", inst.getIdFrontUrl()),
            createDocThumb("🪪 ID Back", inst.getIdBackUrl()),
            createDocThumb("📜 Certificate", inst.getExperienceCertificateUrl()),
            createDocStatus("📄 CV", inst.getCvUrl())
        );

        // Bio
        VBox bioBox = new VBox(2);
        if (inst.getInstructorBio() != null && !inst.getInstructorBio().isBlank()) {
            Label bioLabel = new Label("Bio: " + inst.getInstructorBio());
            bioLabel.setWrapText(true);
            bioLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4B5563;");
            bioBox.getChildren().add(bioLabel);
        }

        // Action buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 8 0 0 0;");

        TextArea rejectionField = new TextArea();
        rejectionField.setPromptText("Rejection reason (required for reject)...");
        rejectionField.setPrefHeight(50);
        rejectionField.setWrapText(true);
        rejectionField.setVisible(false);
        rejectionField.setManaged(false);

        Button approveBtn = new Button("✅ Approve");
        approveBtn.getStyleClass().addAll("btn-gradient");
        approveBtn.setStyle("-fx-background-color: linear-gradient(to right, #10B981, #059669); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 24;");
        approveBtn.setOnAction(e -> handleApprove(inst, card));

        Button rejectBtn = new Button("❌ Reject");
        rejectBtn.setStyle("-fx-background-color: linear-gradient(to right, #EF4444, #DC2626); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 24;");
        rejectBtn.setOnAction(e -> {
            if (!rejectionField.isVisible()) {
                rejectionField.setVisible(true);
                rejectionField.setManaged(true);
                AnimationUtils.fadeIn(rejectionField, 200);
            } else {
                String reason = rejectionField.getText().trim();
                if (reason.isEmpty()) {
                    rejectionField.setStyle("-fx-border-color: #EF4444;");
                    return;
                }
                handleReject(inst, reason, card);
            }
        });

        actions.getChildren().addAll(approveBtn, rejectBtn);

        card.getChildren().addAll(header, details, docsHeader, docsRow, bioBox, rejectionField, actions);
        return card;
    }

    private VBox createDetailItem(String label, String value) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        val.setWrapText(true);
        val.setMaxWidth(180);
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private VBox createDocThumb(String label, String url) {
        String docName = label.length() > 2 ? label.substring(2).trim() : label;
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(200);
        box.setPrefHeight(180);
        box.setStyle("-fx-padding: 10; -fx-background-radius: 10;");

        if (url != null && !url.isBlank()) {
            // Green border for uploaded docs
            box.setStyle("-fx-padding: 10; -fx-background-color: #F0FDF4; -fx-background-radius: 10; -fx-border-color: #10B981; -fx-border-width: 2; -fx-border-radius: 10;");

            ImageView iv = new ImageView();
            iv.setFitWidth(180);
            iv.setFitHeight(120);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setStyle("-fx-cursor: hand;");

            // Load image async
            try {
                Image img = new Image(url, 360, 240, true, true, true);
                iv.setImage(img);
            } catch (Exception e) {
                System.err.println("[Verification] Failed to load image: " + url + " — " + e.getMessage());
            }

            // Click to view full-size in a popup dialog
            iv.setOnMouseClicked(e -> showFullImageDialog(docName, url));

            Label docLabel = new Label("✅ " + docName);
            docLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669; -fx-font-weight: bold;");

            Label clickHint = new Label("🔍 Click to enlarge");
            clickHint.setStyle("-fx-font-size: 9px; -fx-text-fill: #9CA3AF; -fx-cursor: hand;");
            clickHint.setOnMouseClicked(e -> showFullImageDialog(docName, url));

            box.getChildren().addAll(iv, docLabel, clickHint);
        } else {
            // Red border for missing docs
            box.setStyle("-fx-padding: 10; -fx-background-color: #FEF2F2; -fx-background-radius: 10; -fx-border-color: #EF4444; -fx-border-width: 2; -fx-border-radius: 10; -fx-border-style: segments(6, 4);");
            box.getChildren().addAll(
                new Label("❌") {{ setStyle("-fx-font-size: 28px;"); }},
                new Label(docName) {{ setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;"); }},
                new Label("Not uploaded") {{ setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444;"); }}
            );
        }
        return box;
    }

    private VBox createDocStatus(String label, String url) {
        String docName = label.length() > 2 ? label.substring(2).trim() : label;
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(200);
        box.setPrefHeight(180);

        if (url != null && !url.isBlank()) {
            box.setStyle("-fx-padding: 10; -fx-background-color: #F0FDF4; -fx-background-radius: 10; -fx-border-color: #10B981; -fx-border-width: 2; -fx-border-radius: 10;");

            // CV is usually a PDF — show icon + open in browser
            box.getChildren().addAll(
                new Label("📄") {{ setStyle("-fx-font-size: 36px;"); }},
                new Label("✅ " + docName) {{ setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #059669;"); }},
                new Label("Uploaded") {{ setStyle("-fx-font-size: 10px; -fx-text-fill: #10B981;"); }}
            );

            Button openBtn = new Button("🔗 Open File");
            openBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 10;");
            openBtn.setOnAction(e -> {
                try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(url)); } catch (Exception ex) { System.err.println("Failed to open URL: " + ex.getMessage()); }
            });
            box.getChildren().add(openBtn);
        } else {
            box.setStyle("-fx-padding: 10; -fx-background-color: #FEF2F2; -fx-background-radius: 10; -fx-border-color: #EF4444; -fx-border-width: 2; -fx-border-radius: 10; -fx-border-style: segments(6, 4);");
            box.getChildren().addAll(
                new Label("❌") {{ setStyle("-fx-font-size: 28px;"); }},
                new Label(docName) {{ setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;"); }},
                new Label("Not uploaded") {{ setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444;"); }}
            );
        }
        return box;
    }

    /** Show full-size image in a popup dialog */
    private void showFullImageDialog(String title, String imageUrl) {
        try {
            javafx.stage.Stage popup = new javafx.stage.Stage();
            popup.setTitle("📄 " + title);
            popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            ImageView fullImage = new ImageView(new Image(imageUrl, 800, 600, true, true, true));
            fullImage.setPreserveRatio(true);
            fullImage.setSmooth(true);

            ScrollPane scroll = new ScrollPane(fullImage);
            scroll.setFitToWidth(true);
            scroll.setPannable(true);
            scroll.setStyle("-fx-background-color: #1F2937;");

            Label titleLabel = new Label("📄 " + title);
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 12 16;");

            Button closeBtn = new Button("✕ Close");
            closeBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 16;");
            closeBtn.setOnAction(e -> popup.close());

            HBox topBar = new HBox(12);
            topBar.setAlignment(Pos.CENTER_LEFT);
            topBar.setStyle("-fx-background-color: #111827; -fx-padding: 8 16;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            topBar.getChildren().addAll(titleLabel, spacer, closeBtn);

            VBox root = new VBox(0);
            root.getChildren().addAll(topBar, scroll);
            VBox.setVgrow(scroll, Priority.ALWAYS);

            javafx.scene.Scene scene = new javafx.scene.Scene(root, 850, 650);
            popup.setScene(scene);
            popup.show();
        } catch (Exception e) {
            System.err.println("[Verification] Failed to show image: " + e.getMessage());
            ComponentFactory.showErrorDialog("Error", "Could not load image: " + e.getMessage());
        }
    }

    private void handleApprove(Instructor inst, VBox card) {
        if (!ComponentFactory.showConfirmDialog("Approve Instructor", "Approve " + (inst.getProfile() != null ? inst.getProfile().getFullName() : "this instructor") + "?")) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                adminService.approveInstructorVerification(inst.getId(), SessionManager.getInstance().getCurrentUserId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            AnimationUtils.fadeIn(card, 300);
            requestsContainer.getChildren().remove(card);
            ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(), "Instructor approved!", ToastNotification.Type.SUCCESS);
            loadRequests();
        }));
        new Thread(task).start();
    }

    private void handleReject(Instructor inst, String reason, VBox card) {
        if (!ComponentFactory.showConfirmDialog("Reject Instructor", "Reject with reason: " + reason + "?")) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                adminService.rejectInstructorVerification(inst.getId(), reason, SessionManager.getInstance().getCurrentUserId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            requestsContainer.getChildren().remove(card);
            ToastNotification.show(NavigationManager.getInstance().getPrimaryStage(), "Instructor rejected.", ToastNotification.Type.WARNING);
            loadRequests();
        }));
        new Thread(task).start();
    }
}
