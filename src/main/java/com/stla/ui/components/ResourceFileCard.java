package com.stla.ui.components;

import com.stla.domain.models.LessonResource;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Resource file card component showing file type icon, title, size, and remove button.
 */
public class ResourceFileCard extends HBox {

    private final LessonResource resource;
    private Consumer<LessonResource> onRemove;

    public ResourceFileCard(LessonResource resource) {
        this.resource = resource;
        buildUI();
    }

    private void buildUI() {
        getStyleClass().add("resource-file-card");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(14, 18, 14, 18));

        String icon = switch (resource.getResourceType() != null ? resource.getResourceType().toLowerCase() : "") {
            case "pdf" -> "📄";
            case "doc", "docx" -> "📝";
            case "zip" -> "📦";
            case "png", "jpg", "jpeg", "webp" -> "🖼";
            default -> "📎";
        };

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("resource-file-icon");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(resource.getTitle() != null ? resource.getTitle() : "Untitled");
        title.getStyleClass().add("resource-file-title");
        Label meta = new Label(resource.getResourceType().toUpperCase() + " • " + resource.getFileSizeFormatted());
        meta.getStyleClass().add("resource-file-meta");
        info.getChildren().addAll(title, meta);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("resource-file-remove-btn");
        removeBtn.setOnAction(e -> { if (onRemove != null) onRemove.accept(resource); });

        getChildren().addAll(iconLbl, info, removeBtn);
    }

    public void setOnRemove(Consumer<LessonResource> handler) { this.onRemove = handler; }
    public LessonResource getResource() { return resource; }
}
