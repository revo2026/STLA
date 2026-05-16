package com.stla.ui.admin;

import com.stla.data.repositories.CategoryRepositoryImpl;
import com.stla.domain.models.Category;
import com.stla.ui.components.ComponentFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class CategoriesManagementController {
    @javafx.fxml.FXML private VBox catsContainer;
    @javafx.fxml.FXML private TextField nameField;
    @javafx.fxml.FXML private TextField slugField;
    @javafx.fxml.FXML private TextField descField;

    private final CategoryRepositoryImpl catRepo = new CategoryRepositoryImpl();

    @javafx.fxml.FXML public void initialize() { load(); }

    private void load() {
        catsContainer.getChildren().clear();
        catsContainer.getChildren().add(ComponentFactory.createLoadingState());
        Task<List<Category>> task = new Task<>() {
            @Override protected List<Category> call() { return catRepo.findAll(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> build(task.getValue())));
        new Thread(task).start();
    }

    private void build(List<Category> cats) {
        catsContainer.getChildren().clear();
        if (cats.isEmpty()) {
            catsContainer.getChildren().add(ComponentFactory.createEmptyState("📁", "No categories yet"));
            return;
        }
        for (Category c : cats) {
            HBox row = new HBox(16);
            row.getStyleClass().add("card");
            row.setPadding(new Insets(12, 16, 12, 16));
            row.setAlignment(Pos.CENTER_LEFT);

            Label icon = new Label(c.getIconName() != null ? c.getIconName() : "📁");
            icon.setStyle("-fx-font-size: 20px;");
            Label name = new Label(c.getName());
            name.setPrefWidth(200);
            name.setStyle("-fx-font-weight: bold;");
            Label slug = new Label(c.getSlug());
            slug.setPrefWidth(150);
            slug.getStyleClass().add("text-secondary");
            HBox statusBox = new HBox(ComponentFactory.createStatusBadge(c.isActive() ? "Active" : "Inactive", c.isActive() ? "active" : "danger"));
            statusBox.setPrefWidth(100);
            Button deact = new Button(c.isActive() ? "Deactivate" : "Activate");
            deact.getStyleClass().addAll(c.isActive() ? "btn-danger" : "btn-success", "btn-sm");
            deact.setOnAction(e -> { catRepo.deactivate(c.getId()); load(); });

            row.getChildren().addAll(icon, name, slug, statusBox, deact);
            catsContainer.getChildren().add(row);
        }
    }

    @javafx.fxml.FXML private void handleAddCategory() {
        String name = nameField.getText().trim();
        String slug = slugField.getText().trim();
        String desc = descField.getText().trim();
        if (name.isEmpty()) { ComponentFactory.showErrorDialog("Error", "Name is required"); return; }
        Category cat = new Category();
        cat.setName(name);
        cat.setSlug(slug.isEmpty() ? name.toLowerCase().replaceAll("\\s+", "-") : slug);
        cat.setDescription(desc);
        new Thread(() -> { catRepo.save(cat); Platform.runLater(() -> { nameField.clear(); slugField.clear(); descField.clear(); load(); }); }).start();
    }
}
