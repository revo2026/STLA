package com.stla.ui.components;

import com.stla.core.navigation.NavigationManager;
import com.stla.core.session.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/**
 * Reusable UI component builders for the STLA desktop application.
 */
public class ComponentFactory {

    // ==================== STAT CARD ====================
    public static VBox createStatCard(String icon, String label, String value, boolean primary) {
        VBox card = new VBox(6);
        card.getStyleClass().add(primary ? "stat-card-primary" : "stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size: 24px;");
        Label val = new Label(value);
        val.getStyleClass().add("stat-value");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");
        card.getChildren().addAll(ic, val, lbl);
        return card;
    }

    // ==================== COURSE CARD ====================
    public static VBox createCourseCard(String title, String level, String price, String rating, int students, Runnable onClick) {
        VBox card = new VBox(8);
        card.getStyleClass().add("course-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(260);
        card.setCursor(onClick != null ? Cursor.HAND : Cursor.DEFAULT);
        card.setOnMouseClicked(e -> {
            if (onClick != null && e.getClickCount() == 1) {
                onClick.run();
                e.consume();
            }
        });

        VBox thumb = new VBox();
        thumb.setPrefHeight(130);
        thumb.setStyle("-fx-background-color: linear-gradient(to bottom right, #0E4A54, #0F3D45); -fx-background-radius: 10;");
        thumb.setAlignment(Pos.CENTER);
        thumb.getChildren().add(new Label("📘") {{ setStyle("-fx-font-size: 32px;"); }});

        Label ttl = new Label(title);
        ttl.getStyleClass().add("course-title");
        ttl.setWrapText(true);
        ttl.setMaxWidth(240);

        Label lvl = new Label(level != null ? level.toUpperCase() : "");
        lvl.getStyleClass().addAll("badge", "badge-student");

        Label prc = new Label(price);
        prc.getStyleClass().add("course-price");

        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.getChildren().addAll(
            new Label("⭐ " + rating) {{ getStyleClass().add("course-rating"); }},
            new Label("👥 " + students) {{ setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;"); }}
        );

        card.getChildren().addAll(thumb, ttl, lvl, prc, bottom);
        return card;
    }

    // ==================== SIDEBAR ====================
    public static VBox createSidebar(String portalName, String[][] items, String activeItem, Consumer<String> onNav) {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");

        // Logo
        VBox header = new VBox(4);
        header.getStyleClass().add("sidebar-header");
        header.getChildren().addAll(
            new Label("📚 STLA") {{ getStyleClass().add("sidebar-logo"); }},
            new Label(portalName) {{ getStyleClass().add("sidebar-subtitle"); }}
        );
        sidebar.getChildren().add(header);

        Region sep = new Region();
        sep.getStyleClass().add("sidebar-separator");
        sidebar.getChildren().add(sep);

        VBox nav = new VBox(2);
        nav.setPadding(new Insets(8, 0, 8, 0));

        for (String[] item : items) {
            if (item.length == 1) {
                // Section title
                Label sectionTitle = new Label(item[0]);
                sectionTitle.getStyleClass().add("sidebar-section-title");
                nav.getChildren().add(sectionTitle);
            } else {
                Button btn = new Button(item[0]);
                btn.getStyleClass().add("sidebar-item");
                if (item[1].equals(activeItem)) {
                    btn.getStyleClass().add("sidebar-item-active");
                }
                String navKey = item[1];
                btn.setOnAction(e -> onNav.accept(navKey));
                nav.getChildren().add(btn);
            }
        }

        ScrollPane scroll = new ScrollPane(nav);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        sidebar.getChildren().add(scroll);

        // User info at bottom
        SessionManager session = SessionManager.getInstance();
        VBox userBox = new VBox(4);
        userBox.getStyleClass().add("sidebar-user");
        userBox.getChildren().addAll(
            new Label(session.getCurrentUserName()) {{ getStyleClass().add("sidebar-user-name"); }},
            new Label(session.getCurrentRole().getValue().toUpperCase()) {{ getStyleClass().add("sidebar-user-role"); }}
        );
        Region sp2 = new Region();
        sp2.setPrefHeight(6);
        Button logout = new Button("🚪  Sign Out");
        logout.getStyleClass().add("sidebar-item");
        logout.setStyle("-fx-text-fill: #EF6B6B;");
        logout.setOnAction(e -> NavigationManager.getInstance().navigateToLogin());
        userBox.getChildren().addAll(sp2, logout);
        sidebar.getChildren().add(userBox);

        return sidebar;
    }

    // ==================== TOP HEADER ====================
    public static HBox createTopHeader(String title, String subtitle) {
        HBox header = new HBox(16);
        header.getStyleClass().add("top-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(2);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.getChildren().addAll(
            new Label(title) {{ getStyleClass().add("page-title"); }},
            new Label(subtitle) {{ getStyleClass().add("page-subtitle"); }}
        );

        Button bell = new Button("🔔");
        bell.getStyleClass().add("notification-bell");

        header.getChildren().addAll(left, bell);
        return header;
    }

    // ==================== CONFIRM DIALOG ====================
    public static boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    // ==================== INFO DIALOG ====================
    public static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== ERROR DIALOG ====================
    public static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== EMPTY STATE ====================
    public static VBox createEmptyState(String icon, String message) {
        VBox box = new VBox(12);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(
            new Label(icon) {{ getStyleClass().add("empty-state-icon"); }},
            new Label(message) {{ getStyleClass().add("empty-state-message"); }}
        );
        return box;
    }

    // ==================== LOADING ====================
    public static VBox createLoadingState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(40, 40);
        box.getChildren().addAll(spinner, new Label("Loading...") {{ getStyleClass().add("text-secondary"); }});
        return box;
    }

    // ==================== PROGRESS BAR ====================
    public static HBox createProgressBar(double percent) {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);
        ProgressBar bar = new ProgressBar(percent / 100.0);
        bar.setPrefWidth(150);
        bar.setPrefHeight(8);
        bar.setStyle("-fx-accent: #10B981;");
        Label lbl = new Label(String.format("%.0f%%", percent));
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        container.getChildren().addAll(bar, lbl);
        return container;
    }

    // ==================== STATUS BADGE ====================
    public static Label createStatusBadge(String text, String type) {
        Label badge = new Label(text.toUpperCase());
        badge.getStyleClass().add("badge");
        badge.getStyleClass().add(switch (type) {
            case "success", "approved", "active", "paid", "completed" -> "badge-success";
            case "warning", "pending", "draft" -> "badge-warning";
            case "danger", "rejected", "failed", "cancelled" -> "badge-danger";
            case "student" -> "badge-student";
            case "instructor" -> "badge-instructor";
            case "admin" -> "badge-admin";
            default -> "badge-student";
        });
        return badge;
    }
}
