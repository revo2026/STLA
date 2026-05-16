package com.stla.ui.components;

import com.stla.domain.enums.NotificationType;
import com.stla.domain.models.Notification;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Shared notification card rendering for all roles. */
public final class NotificationUiHelper {

    private NotificationUiHelper() {}

    public static HBox buildCard(Notification n, Consumer<Notification> onMarkRead) {
        HBox card = new HBox(12);
        card.getStyleClass().add(n.isRead() ? "notification-card" : "notification-card-unread");
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(javafx.scene.Cursor.HAND);

        Label icon = new Label(iconFor(n));
        icon.getStyleClass().addAll("notification-icon", iconStyleClass(n));

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(n.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label badge = new Label(typeBadge(n.getType()));
        badge.getStyleClass().add("notification-type-badge");
        titleRow.getChildren().addAll(title, badge);

        info.getChildren().addAll(
                titleRow,
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
            if (!n.isRead() && onMarkRead != null) onMarkRead.accept(n);
        });
        return card;
    }

    public static HBox buildCompactRow(Notification n, Consumer<Notification> onMarkRead) {
        HBox row = new HBox(10);
        row.getStyleClass().add("notification-dropdown-item");
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(javafx.scene.Cursor.HAND);

        Label icon = new Label(iconFor(n));
        icon.setStyle("-fx-font-size: 16px;");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(n.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        title.setMaxWidth(260);
        title.setWrapText(true);
        Label body = new Label(truncate(n.getBody(), 60));
        body.getStyleClass().add("text-secondary");
        body.setStyle("-fx-font-size: 11px;");
        body.setWrapText(true);
        info.getChildren().addAll(title, body);

        if (!n.isRead()) {
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 8px;");
            row.getChildren().addAll(icon, info, dot);
        } else {
            row.getChildren().addAll(icon, info);
        }

        row.setOnMouseClicked(e -> {
            if (!n.isRead() && onMarkRead != null) onMarkRead.accept(n);
        });
        return row;
    }

    public static String iconFor(Notification n) {
        String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
        if (n.getType() == NotificationType.PAYMENT || title.contains("payment") || title.contains("commission")) {
            return title.contains("fail") ? "❌" : "💳";
        }
        if (title.contains("withdrawal")) return "💸";
        if (title.contains("revenue") || title.contains("earned")) return "💰";
        if (title.contains("certificate")) return "🏆";
        if (title.contains("quiz")) {
            return title.contains("pass") ? "✅" : "📝";
        }
        if (title.contains("lesson")) return "📘";
        if (title.contains("verif")) return "✅";
        if (title.contains("reject")) return "❌";
        if (n.getType() == NotificationType.ENROLLMENT) return "🎓";
        if (n.getType() == NotificationType.ADMIN_ALERT) return "⚠️";
        return "🔔";
    }

    public static String iconStyleClass(Notification n) {
        String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
        if (n.getType() == NotificationType.PAYMENT || title.contains("payment")) {
            return title.contains("fail") ? "notif-icon-red" : "notif-icon-green";
        }
        if (title.contains("withdrawal") || title.contains("reject")) return "notif-icon-red";
        if (title.contains("revenue") || title.contains("commission")) return "notif-icon-green";
        if (title.contains("certificate")) return "notif-icon-gold";
        if (title.contains("quiz") && title.contains("pass")) return "notif-icon-green";
        if (title.contains("quiz") && title.contains("fail")) return "notif-icon-red";
        if (title.contains("lesson")) return "notif-icon-blue";
        return "notif-icon-purple";
    }

    public static String typeBadge(NotificationType type) {
        return switch (type) {
            case PAYMENT -> "Payment";
            case ENROLLMENT -> "Enrollment";
            case COURSE_UPDATE -> "Course";
            case NEW_LESSON -> "Lesson";
            case WITHDRAWAL -> "Wallet";
            case ADMIN_ALERT -> "Alert";
            default -> "System";
        };
    }

    public static boolean matchesAdminFilter(Notification n, String filter) {
        if (filter == null || "All".equals(filter)) return true;
        String title = safeLower(n.getTitle());
        String body = safeLower(n.getBody());
        NotificationType t = n.getType();
        return switch (filter) {
            case "Payments" -> t == NotificationType.PAYMENT || title.contains("payment")
                    || title.contains("commission") || title.contains("withdrawal");
            case "Courses" -> t == NotificationType.COURSE_UPDATE || title.contains("course");
            case "Instructors" -> title.contains("instructor") || title.contains("verif")
                    || t == NotificationType.ADMIN_ALERT;
            case "Students" -> t == NotificationType.ENROLLMENT || title.contains("student")
                    || title.contains("enrolled");
            case "System" -> t == NotificationType.GENERAL || title.contains("registered");
            default -> true;
        };
    }

    public static boolean matchesInstructorFilter(Notification n, String filter) {
        if (filter == null || "All".equals(filter)) return true;
        String title = safeLower(n.getTitle());
        NotificationType t = n.getType();
        return switch (filter) {
            case "Revenue" -> title.contains("revenue") || title.contains("earned") || title.contains("purchase");
            case "Courses" -> t == NotificationType.COURSE_UPDATE || title.contains("course");
            case "Students" -> t == NotificationType.ENROLLMENT || title.contains("student")
                    || title.contains("lesson") || title.contains("completed");
            case "Quizzes" -> title.contains("quiz");
            case "Wallet" -> t == NotificationType.WITHDRAWAL || title.contains("withdrawal");
            case "System" -> title.contains("verif") || t == NotificationType.GENERAL || t == NotificationType.ADMIN_ALERT;
            default -> true;
        };
    }

    private static String safeLower(String s) {
        return s != null ? s.toLowerCase() : "";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
