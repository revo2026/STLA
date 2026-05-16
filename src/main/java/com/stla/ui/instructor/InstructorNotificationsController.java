package com.stla.ui.instructor;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.Notification;
import com.stla.services.NotificationService;
import com.stla.ui.components.ComponentFactory;
import com.stla.ui.components.NotificationUiHelper;
import com.stla.ui.components.ToastNotification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class InstructorNotificationsController {

    @FXML private VBox notifsContainer;
    @FXML private Button markAllBtn;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label unreadCountLabel;

    private final NotificationService notificationService = new NotificationService();
    private List<Notification> allNotifications = List.of();

    @FXML
    public void initialize() {
        filterCombo.setItems(FXCollections.observableArrayList(
                "All", "Revenue", "Courses", "Students", "Quizzes", "Wallet", "System"));
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
                return notificationService.getNotifications(profileId);
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
        return allNotifications.stream()
                .filter(n -> NotificationUiHelper.matchesInstructorFilter(n, filter))
                .collect(Collectors.toList());
    }

    private void build(List<Notification> notifs) {
        notifsContainer.getChildren().clear();
        if (notifs.isEmpty()) {
            notifsContainer.getChildren().add(ComponentFactory.createEmptyState("🔔", "No notifications"));
            return;
        }
        for (Notification n : notifs) {
            notifsContainer.getChildren().add(NotificationUiHelper.buildCard(n, this::onMarkRead));
        }
    }

    private void onMarkRead(Notification n) {
        notificationService.markAsRead(n.getId());
        n.setRead(true);
        showToast(n.getTitle());
        loadNotifications();
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
