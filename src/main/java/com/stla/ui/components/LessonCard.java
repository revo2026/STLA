package com.stla.ui.components;

import com.stla.domain.models.CourseLesson;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Lesson card component for the course player sidebar.
 * Shows lesson icon, title, duration, badges, and context actions.
 */
public class LessonCard extends HBox {

    private final CourseLesson lesson;
    private boolean active = false;
    private Consumer<CourseLesson> onSelect, onEdit, onDelete, onDuplicate, onMoveUp, onMoveDown;

    public LessonCard(CourseLesson lesson) {
        this.lesson = lesson;
        buildUI();
    }

    private void buildUI() {
        getStyleClass().add("lesson-item");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(10, 12, 10, 24));

        boolean hasVideo = lesson.getVideoUrl() != null && !lesson.getVideoUrl().isBlank();
        Label icon = new Label(hasVideo ? "\uD83C\uDFAC" : "\uD83D\uDCC4");
        icon.getStyleClass().add("lesson-item-icon");

        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        Label title = new Label(lesson.getTitle());
        title.getStyleClass().add("lesson-item-title");
        title.setMaxWidth(180);

        HBox metaRow = new HBox(6);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label duration = new Label(lesson.getDurationFormatted());
        duration.getStyleClass().add("lesson-item-duration");
        metaRow.getChildren().add(duration);
        if (lesson.isPreview()) {
            Label p = new Label("PREVIEW");
            p.getStyleClass().add("lesson-item-preview-badge");
            metaRow.getChildren().add(p);
        }
        if (hasVideo) {
            Label v = new Label("VIDEO");
            v.getStyleClass().add("lesson-item-video-badge");
            metaRow.getChildren().add(v);
        }
        textBox.getChildren().addAll(title, metaRow);

        HBox actions = new HBox(2);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = ctxBtn("✏", "Edit", l -> { if (onEdit != null) onEdit.accept(l); });
        Button dupeBtn = ctxBtn("📋", "Duplicate", l -> { if (onDuplicate != null) onDuplicate.accept(l); });
        Button upBtn = ctxBtn("▲", "Move Up", l -> { if (onMoveUp != null) onMoveUp.accept(l); });
        Button downBtn = ctxBtn("▼", "Move Down", l -> { if (onMoveDown != null) onMoveDown.accept(l); });
        Button delBtn = ctxBtn("🗑", "Delete", l -> { if (onDelete != null) onDelete.accept(l); });
        delBtn.setStyle("-fx-text-fill: #EF4444;");
        actions.getChildren().addAll(editBtn, dupeBtn, upBtn, downBtn, delBtn);

        getChildren().addAll(icon, textBox, actions);
        setOnMouseClicked(e -> { if (onSelect != null) onSelect.accept(lesson); });
    }

    private Button ctxBtn(String text, String tip, Consumer<CourseLesson> action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("lesson-ctx-btn");
        btn.setTooltip(new Tooltip(tip));
        btn.setOnAction(e -> { action.accept(lesson); e.consume(); });
        return btn;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) { if (!getStyleClass().contains("lesson-item-active")) getStyleClass().add("lesson-item-active"); }
        else getStyleClass().remove("lesson-item-active");
    }

    public boolean isActive() { return active; }
    public CourseLesson getLesson() { return lesson; }
    public void setOnSelect(Consumer<CourseLesson> h) { this.onSelect = h; }
    public void setOnEdit(Consumer<CourseLesson> h) { this.onEdit = h; }
    public void setOnDelete(Consumer<CourseLesson> h) { this.onDelete = h; }
    public void setOnDuplicate(Consumer<CourseLesson> h) { this.onDuplicate = h; }
    public void setOnMoveUp(Consumer<CourseLesson> h) { this.onMoveUp = h; }
    public void setOnMoveDown(Consumer<CourseLesson> h) { this.onMoveDown = h; }
}
