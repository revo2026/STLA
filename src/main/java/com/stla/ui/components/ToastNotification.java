package com.stla.ui.components;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Toast notification system — slide-in popup with auto-dismiss.
 */
public class ToastNotification {

    public enum Type { SUCCESS, ERROR, WARNING, INFO }

    public static void show(Stage stage, String message, Type type) {
        if (stage == null || stage.getScene() == null) return;

        String icon = switch (type) {
            case SUCCESS -> "✅";
            case ERROR -> "❌";
            case WARNING -> "⚠️";
            case INFO -> "ℹ️";
        };

        String styleClass = switch (type) {
            case SUCCESS -> "toast-success";
            case ERROR -> "toast-error";
            case WARNING -> "toast-warning";
            case INFO -> "toast-info";
        };

        Label label = new Label(icon + "  " + message);
        label.getStyleClass().addAll("toast", styleClass);
        label.setStyle(label.getStyle() + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        StackPane root = (StackPane) stage.getScene().getRoot().lookup(".toast-container");
        if (root == null) {
            // Fallback: use Popup
            Popup popup = new Popup();
            popup.setAutoHide(true);
            popup.getContent().add(label);
            popup.show(stage,
                stage.getX() + stage.getWidth() / 2 - 150,
                stage.getY() + stage.getHeight() - 80
            );

            // Fade in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), label);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            fadeIn.play();

            // Auto dismiss
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), label);
                fadeOut.setFromValue(1); fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> popup.hide());
                fadeOut.play();
            });
            pause.play();
        }
    }
}
