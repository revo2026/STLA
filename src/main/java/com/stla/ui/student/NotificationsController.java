package com.stla.ui.student;

import com.stla.core.session.SessionManager;
import com.stla.domain.enums.NotificationType;
import com.stla.domain.models.Notification;
import com.stla.services.NotificationService;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationsController {
    @FXML private VBox notifsContainer;
    @FXML private Button markAllBtn;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label unreadCountLabel;

    private final NotificationService notificationService = new NotificationService();
    private List<Notification> allNotifications = List.of();

    @FXML
    public void initialize() {
        filterCombo.setItems(FXCollections.observableArrayList(
                "All", "Payment", "Lesson", "Quiz", "Course", "Certificate"));
        filterCombo.getSelectionModel().selectFirst();
        filterCombo.setOnAction(e -> build(filtered()));
        loadNotifications();
    }

    private void loadNotifications() {
        notifsContainer.getChildren().clear();
        notifsContainer.getChildren().add(ComponentFactory.createLoadingState());
        String profileId = SessionManager.getInstance().getCurrentProfile().getId();
        Task<List<Notification>> task = new Task<>() {
            @Override protected List<Notification> call() {
                return notificationService.getStudentNotifications(profileId);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            allNotifications = task.getValue() != null ? task.getValue() : List.of();
            int unread = notificationService.countUnread(profileId);
            unreadCountLabel.setText(unread > 0 ? unread + " unread" : "All caught up");
            build(filtered());
        }));
        new Thread(task).start();
    }

    private List<Notification> filtered() {
        String filter = filterCombo.getSelectionModel().getSelectedItem();
        if (filter == null || "All".equals(filter)) return allNotifications;
        return allNotifications.stream().filter(n -> matchesFilter(n, filter)).collect(Collectors.toList());
    }

    private boolean matchesFilter(Notification n, String filter) {
        NotificationType t = n.getType();
        return switch (filter) {
            case "Payment" -> t == NotificationType.PAYMENT;
            case "Lesson" -> t == NotificationType.NEW_LESSON
                    || (n.getTitle() != null && n.getTitle().toLowerCase().contains("lesson"));
            case "Quiz" -> n.getTitle() != null && n.getTitle().toLowerCase().contains("quiz");
            case "Course" -> t == NotificationType.COURSE_UPDATE || t == NotificationType.ENROLLMENT;
            case "Certificate" -> n.getTitle() != null && n.getTitle().toLowerCase().contains("certificate");
            default -> true;
        };
    }

    private void build(List<Notification> notifs) {
        notifsContainer.getChildren().clear();
        if (notifs.isEmpty()) {
            notifsContainer.getChildren().add(ComponentFactory.createEmptyState("🔔", "No notifications"));
            return;
        }
        for (Notification n : notifs) {
            notifsContainer.getChildren().add(buildCard(n));
        }
    }

    private HBox buildCard(Notification n) {
        HBox card = new HBox(12);
        card.getStyleClass().add(n.isRead() ? "notification-card" : "notification-card-unread");
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(javafx.scene.Cursor.HAND);

        Label icon = new Label(iconFor(n));
        icon.getStyleClass().add("notification-icon");
        icon.getStyleClass().add(iconStyleClass(n));

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.getChildren().addAll(
                new Label(n.getTitle()) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); }},
                new Label(n.getBody()) {{ getStyleClass().add("text-secondary"); setWrapText(true); }},
                new Label(n.getCreatedAt() != null
                        ? n.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")) : "")
                        {{ setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;"); }}
        );

        if (!n.isRead()) {
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 10px;");
            card.getChildren().addAll(icon, dot, info);
        } else {
            card.getChildren().addAll(icon, info);
        }

        card.setOnMouseClicked(e -> {
            if (!n.isRead()) {
                notificationService.markAsRead(n.getId());
                n.setRead(true);
                showToast(n.getTitle());
                loadNotifications();
            }
        });
        return card;
    }

    private String iconFor(Notification n) {
        String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
        if (n.getType() == NotificationType.PAYMENT) {
            return title.contains("fail") ? "❌" : "💳";
        }
        if (title.contains("certificate")) return "🏆";
        if (title.contains("quiz")) {
            return title.contains("pass") || title.contains("passed") ? "✅" : "📝";
        }
        if (title.contains("lesson")) return "📘";
        if (n.getType() == NotificationType.ENROLLMENT) return "🎓";
        return "🔔";
    }

    private String iconStyleClass(Notification n) {
        String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
        if (n.getType() == NotificationType.PAYMENT) {
            return title.contains("fail") ? "notif-icon-red" : "notif-icon-green";
        }
        if (title.contains("certificate")) return "notif-icon-gold";
        if (title.contains("quiz") && (title.contains("pass") || title.contains("passed"))) return "notif-icon-green";
        if (title.contains("quiz") && title.contains("fail")) return "notif-icon-red";
        if (title.contains("lesson")) return "notif-icon-blue";
        return "notif-icon-purple";
    }

    private void showToast(String title) {
        try {
            if (notifsContainer.getScene() != null) {
                ToastNotification.show((Stage) notifsContainer.getScene().getWindow(),
                        title, ToastNotification.Type.INFO);
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleMarkAllRead() {
        String profileId = SessionManager.getInstance().getCurrentProfile().getId();
        new Thread(() -> {
            notificationService.markAllAsRead(profileId);
            Platform.runLater(this::loadNotifications);
        }).start();
    }
}
