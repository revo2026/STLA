package com.stla.ui.components;

import com.stla.core.session.SessionManager;
import com.stla.domain.models.Notification;
import com.stla.services.NotificationService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

/**
 * Top-bar notification bell with unread badge and dropdown preview.
 */
public final class NotificationBellHelper {

    private static final int POLL_SECONDS = 30;
    private static final int DROPDOWN_LIMIT = 5;

    private NotificationBellHelper() {}

    public static void install(StackPane host, Runnable onViewAll) {
        if (host == null) return;

        NotificationService service = new NotificationService();
        Label badge = new Label();
        badge.getStyleClass().add("notification-badge-count");
        badge.setVisible(false);
        badge.setMouseTransparent(true);

        Button bell = new Button("🔔");
        bell.getStyleClass().add("notification-bell");
        bell.setMaxSize(40, 40);

        host.getChildren().setAll(bell, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(2, 2, 0, 0));

        Runnable refreshBadge = () -> {
            String profileId = currentProfileId();
            if (profileId == null) return;
            new Thread(() -> {
                int count = service.countUnread(profileId);
                Platform.runLater(() -> {
                    if (count > 0) {
                        badge.setText(count > 99 ? "99+" : String.valueOf(count));
                        badge.setVisible(true);
                    } else {
                        badge.setVisible(false);
                    }
                });
            }).start();
        };

        bell.setOnAction(e -> showDropdown(bell, service, refreshBadge, onViewAll));

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(POLL_SECONDS), ev -> refreshBadge.run()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        refreshBadge.run();
    }

    private static void showDropdown(Button anchor, NotificationService service,
                                     Runnable refreshBadge, Runnable onViewAll) {
        String profileId = currentProfileId();
        if (profileId == null) return;

        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox panel = new VBox(0);
        panel.getStyleClass().add("notification-dropdown-panel");
        panel.setPrefWidth(340);
        panel.setMaxHeight(420);

        HBox header = new HBox(12);
        header.setPadding(new Insets(12, 14, 8, 14));
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Notifications");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button markAll = new Button("Mark all read");
        markAll.getStyleClass().addAll("btn-outline", "btn-sm");
        header.getChildren().addAll(title, spacer, markAll);

        VBox listBox = new VBox(0);
        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(260);
        scroll.setStyle("-fx-background-color: transparent;");

        Button viewAll = new Button("View all notifications");
        viewAll.getStyleClass().add("btn-primary");
        viewAll.setMaxWidth(Double.MAX_VALUE);
        viewAll.setOnAction(ev -> {
            popup.hide();
            if (onViewAll != null) onViewAll.run();
        });

        VBox footer = new VBox(8);
        footer.setPadding(new Insets(8, 14, 12, 14));
        footer.getChildren().add(viewAll);

        panel.getChildren().addAll(header, new Separator(), scroll, footer);
        popup.getContent().add(panel);

        Consumer<Notification> onMarkRead = n -> {
            service.markAsRead(n.getId());
            n.setRead(true);
            refreshBadge.run();
            popup.hide();
            showDropdown(anchor, service, refreshBadge, onViewAll);
        };

        markAll.setOnAction(ev -> {
            service.markAllAsRead(profileId);
            refreshBadge.run();
            popup.hide();
            showDropdown(anchor, service, refreshBadge, onViewAll);
        });

        listBox.getChildren().add(ComponentFactory.createLoadingState());
        popup.show(anchor, anchor.localToScreen(anchor.getBoundsInLocal()).getMaxX() - 340,
                anchor.localToScreen(anchor.getBoundsInLocal()).getMaxY() + 6);

        new Thread(() -> {
            List<Notification> items = service.getRecentNotifications(profileId, DROPDOWN_LIMIT);
            Platform.runLater(() -> {
                listBox.getChildren().clear();
                if (items.isEmpty()) {
                    listBox.getChildren().add(new Label("No notifications yet") {{
                        setPadding(new Insets(16));
                        getStyleClass().add("text-secondary");
                    }});
                } else {
                    for (Notification n : items) {
                        listBox.getChildren().add(NotificationUiHelper.buildCompactRow(n, onMarkRead));
                    }
                }
            });
        }).start();
    }

    private static String currentProfileId() {
        SessionManager session = SessionManager.getInstance();
        return session.getCurrentProfile() != null ? session.getCurrentProfile().getId() : null;
    }
}
