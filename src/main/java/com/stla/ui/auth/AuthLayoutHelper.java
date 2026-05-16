package com.stla.ui.auth;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

/**
 * Collapses the brand panel on narrow windows so auth forms stay usable.
 */
public final class AuthLayoutHelper {

    private static final double COMPACT_BREAKPOINT = 920;

    private AuthLayoutHelper() {}

    public static void bindResponsive(Node root, Region brandPanel, Region formCard) {
        if (root == null) return;

        ChangeListener<Scene> sceneListener = (obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.widthProperty().removeListener(widthListener(root, brandPanel, formCard));
            }
            if (newScene != null) {
                newScene.widthProperty().addListener(widthListener(root, brandPanel, formCard));
                apply(newScene.getWidth(), brandPanel, formCard);
            }
        };

        root.sceneProperty().addListener(sceneListener);
        if (root.getScene() != null) {
            apply(root.getScene().getWidth(), brandPanel, formCard);
        }
    }

    private static ChangeListener<Number> widthListener(Node root, Region brandPanel, Region formCard) {
        return (obs, oldW, newW) -> apply(newW.doubleValue(), brandPanel, formCard);
    }

    private static void apply(double width, Region brandPanel, Region formCard) {
        boolean compact = width > 0 && width < COMPACT_BREAKPOINT;
        if (brandPanel != null) {
            brandPanel.setVisible(!compact);
            brandPanel.setManaged(!compact);
        }
        if (formCard != null) {
            if (compact) {
                if (!formCard.getStyleClass().contains("auth-card-compact")) {
                    formCard.getStyleClass().add("auth-card-compact");
                }
            } else {
                formCard.getStyleClass().remove("auth-card-compact");
            }
        }
    }
}
