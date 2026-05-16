package com.stla.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Drag-and-drop file upload zone with visual feedback.
 * Supports configurable file types and max size.
 * Falls back to FileChooser on click.
 */
public class UploadDropZone extends VBox {

    private Consumer<File> onFileSelected;
    private Set<String> allowedExtensions;
    private long maxFileSize;
    private String title;
    private String subtitle;

    private Label iconLabel;
    private Label titleLabel;
    private Label subtitleLabel;
    private Label browseLabel;
    private Label errorLabel;

    /**
     * @param title       Main text (e.g., "Drop your video here")
     * @param subtitle    Secondary text (e.g., "MP4, MOV, MKV, WEBM — Max 500MB")
     * @param extensions  Allowed file extensions including dot (e.g., ".mp4")
     * @param maxSize     Maximum file size in bytes
     * @param onFile      Callback when valid file is selected
     */
    public UploadDropZone(String title, String subtitle, Set<String> extensions, long maxSize, Consumer<File> onFile) {
        this.title = title;
        this.subtitle = subtitle;
        this.allowedExtensions = extensions;
        this.maxFileSize = maxSize;
        this.onFileSelected = onFile;
        buildUI();
        setupDragDrop();
    }

    private void buildUI() {
        getStyleClass().add("upload-dropzone");
        setAlignment(Pos.CENTER);
        setSpacing(8);
        setPadding(new Insets(32));

        iconLabel = new Label("📁");
        iconLabel.getStyleClass().add("upload-dropzone-icon");

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("upload-dropzone-title");

        subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("upload-dropzone-subtitle");
        subtitleLabel.setWrapText(true);

        browseLabel = new Label("Click to browse files");
        browseLabel.getStyleClass().add("upload-dropzone-browse");
        browseLabel.setStyle(browseLabel.getStyle() + "; -fx-cursor: hand;");

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        getChildren().addAll(iconLabel, titleLabel, subtitleLabel, browseLabel, errorLabel);

        // Click to browse
        setOnMouseClicked(e -> openFileChooser());
    }

    private void setupDragDrop() {
        setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                getStyleClass().add("upload-dropzone-active");
                iconLabel.setText("📥");
                titleLabel.setText("Drop here!");
            }
        });

        setOnDragExited(event -> {
            getStyleClass().remove("upload-dropzone-active");
            iconLabel.setText("📁");
            titleLabel.setText(title);
        });

        setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                success = validateAndAccept(file);
            }
            event.setDropCompleted(success);
            event.consume();
            getStyleClass().remove("upload-dropzone-active");
            iconLabel.setText("📁");
            titleLabel.setText(title);
        });
    }

    private void openFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select File");

        // Build extension filter
        String[] exts = allowedExtensions.stream()
            .map(ext -> "*" + ext)
            .toArray(String[]::new);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Allowed Files", exts));

        File file = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            validateAndAccept(file);
        }
    }

    private boolean validateAndAccept(File file) {
        clearError();

        // Check extension
        String name = file.getName().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        if (!allowedExtensions.contains(ext)) {
            showError("Invalid file type: " + ext.toUpperCase() + ". Allowed: " +
                String.join(", ", allowedExtensions));
            AnimationUtils.shake(this);
            return false;
        }

        // Check size
        if (file.length() > maxFileSize) {
            String maxStr = maxFileSize > (1024 * 1024) ?
                String.format("%.0f MB", maxFileSize / (1024.0 * 1024)) :
                String.format("%.0f KB", maxFileSize / 1024.0);
            showError("File too large. Maximum: " + maxStr);
            AnimationUtils.shake(this);
            return false;
        }

        // Accept
        if (onFileSelected != null) {
            onFileSelected.accept(file);
        }
        return true;
    }

    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /** Update the callback */
    public void setOnFileSelected(Consumer<File> callback) {
        this.onFileSelected = callback;
    }
}
