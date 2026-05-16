package com.stla.ui.components;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;

/**
 * Reusable circular avatar component.
 * Shows profile image clipped to circle, or initials fallback, or default icon.
 *
 * Usage: new CircularAvatar("https://...avatar.png", "Mohammed Ali", 40);
 */
public class CircularAvatar extends StackPane {

    private final double radius;
    private final ImageView imageView;
    private final Label initialsLabel;
    private final Circle borderCircle;
    private final Circle bgCircle;

    /**
     * @param avatarUrl URL of the profile image (nullable)
     * @param fullName  full name for initials fallback (nullable)
     * @param radius    radius in pixels (e.g. 20 = 40px diameter)
     */
    public CircularAvatar(String avatarUrl, String fullName, double radius) {
        this.radius = radius;
        setAlignment(Pos.CENTER);
        setPrefSize(radius * 2, radius * 2);
        setMinSize(radius * 2, radius * 2);
        setMaxSize(radius * 2, radius * 2);

        // Background circle with gradient
        bgCircle = new Circle(radius);
        bgCircle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0E4A54")),
            new Stop(1, Color.web("#1A6B5A"))
        ));

        // Border circle
        borderCircle = new Circle(radius + 2);
        borderCircle.setFill(Color.TRANSPARENT);
        borderCircle.setStroke(Color.web("#0E4A54", 0.3));
        borderCircle.setStrokeWidth(2);

        // Initials label
        initialsLabel = new Label();
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (radius * 0.7) + "px;");
        initialsLabel.setAlignment(Pos.CENTER);

        // Image view
        imageView = new ImageView();
        imageView.setFitWidth(radius * 2);
        imageView.setFitHeight(radius * 2);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        Circle clip = new Circle(radius);
        clip.setCenterX(radius);
        clip.setCenterY(radius);
        imageView.setClip(clip);

        // Drop shadow
        DropShadow shadow = new DropShadow();
        shadow.setRadius(6);
        shadow.setOffsetY(2);
        shadow.setColor(Color.web("#000000", 0.15));
        setEffect(shadow);

        // Hover effect
        setOnMouseEntered(e -> setScaleX(1.05));
        setOnMouseExited(e -> setScaleX(1.0));
        scaleYProperty().bind(scaleXProperty());

        getChildren().addAll(borderCircle, bgCircle, initialsLabel, imageView);

        // Load avatar or show fallback
        setAvatar(avatarUrl, fullName);
    }

    /**
     * Update the avatar display. Can be called after profile update.
     */
    public void setAvatar(String avatarUrl, String fullName) {
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            try {
                Image img = new Image(avatarUrl, radius * 2, radius * 2, false, true, true);
                img.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) showInitials(fullName);
                });
                img.progressProperty().addListener((obs, oldP, newP) -> {
                    if (newP.doubleValue() >= 1.0 && !img.isError()) {
                        imageView.setImage(img);
                        imageView.setVisible(true);
                        initialsLabel.setVisible(false);
                        bgCircle.setVisible(false);
                    }
                });
                // Check if already loaded (local file)
                if (img.getProgress() >= 1.0 && !img.isError()) {
                    imageView.setImage(img);
                    imageView.setVisible(true);
                    initialsLabel.setVisible(false);
                    bgCircle.setVisible(false);
                } else if (img.isError()) {
                    showInitials(fullName);
                }
            } catch (Exception e) {
                showInitials(fullName);
            }
        } else {
            showInitials(fullName);
        }
    }

    private void showInitials(String fullName) {
        imageView.setImage(null);
        imageView.setVisible(false);
        bgCircle.setVisible(true);
        initialsLabel.setVisible(true);
        initialsLabel.setText(extractInitials(fullName));
    }

    /**
     * Extract initials from full name. "Mohammed Ali" → "MA", "John" → "J"
     */
    private String extractInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "👤";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    /** Get the radius */
    public double getRadius() { return radius; }

    /** Navigate to profile when the avatar is clicked. */
    public void setOnProfileNavigate(Runnable action) {
        if (action == null) {
            setCursor(Cursor.DEFAULT);
            setOnMouseClicked(null);
            return;
        }
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> {
            if (e.getClickCount() >= 1) action.run();
        });
    }
}
