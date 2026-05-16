package com.stla.ui.components;

import com.stla.domain.models.LessonResource;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Read-only resource row for students with a download action.
 */
public class StudentResourceFileCard extends HBox {

    private final LessonResource resource;
    private final Button downloadBtn = new Button("⬇ Download");
    private final ProgressIndicator loading = new ProgressIndicator();

    public StudentResourceFileCard(LessonResource resource) {
        this.resource = resource;
        buildUI();
    }

    private void buildUI() {
        getStyleClass().add("resource-file-card");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(12, 16, 12, 16));
        setStyle("-fx-background-color: #334155; -fx-background-radius: 10;");

        String icon = switch (resource.getResourceType() != null ? resource.getResourceType().toLowerCase() : "") {
            case "pdf" -> "📄";
            case "doc", "docx" -> "📝";
            case "zip" -> "📦";
            case "png", "jpg", "jpeg", "webp" -> "🖼";
            default -> "📎";
        };

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(resource.getTitle() != null ? resource.getTitle() : "Untitled");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        String type = resource.getResourceType() != null ? resource.getResourceType().toUpperCase() : "FILE";
        Label meta = new Label(type + " • " + resource.getFileSizeFormatted());
        meta.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        info.getChildren().addAll(title, meta);

        downloadBtn.getStyleClass().addAll("btn-accent", "btn-sm");
        loading.setPrefSize(20, 20);
        loading.setVisible(false);
        loading.setManaged(false);

        HBox actions = new HBox(8, downloadBtn, loading);
        actions.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(iconLbl, info, actions);
    }

    public void setOnDownload(Consumer<LessonResource> handler) {
        downloadBtn.setOnAction(e -> {
            if (handler != null) handler.accept(resource);
        });
    }

    public void setDownloading(boolean busy) {
        downloadBtn.setDisable(busy);
        loading.setVisible(busy);
        loading.setManaged(busy);
        downloadBtn.setText(busy ? "Downloading…" : "⬇ Download");
    }

    public LessonResource getResource() {
        return resource;
    }
}
