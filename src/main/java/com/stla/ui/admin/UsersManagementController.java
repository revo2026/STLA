package com.stla.ui.admin;

import com.stla.domain.models.Profile;
import com.stla.data.repositories.ProfileRepositoryImpl;
import com.stla.ui.components.ComponentFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class UsersManagementController {
    @javafx.fxml.FXML private TextField searchField;
    @javafx.fxml.FXML private ComboBox<String> roleFilter;
    @javafx.fxml.FXML private VBox usersContainer;
    @javafx.fxml.FXML private Label countLabel;

    private final ProfileRepositoryImpl profileRepo = new ProfileRepositoryImpl();
    private List<Profile> allUsers;

    @javafx.fxml.FXML public void initialize() {
        roleFilter.getItems().addAll("All Roles", "student", "instructor", "admin");
        roleFilter.setValue("All Roles");
        searchField.textProperty().addListener((o, ov, nv) -> filter());
        roleFilter.setOnAction(e -> filter());
        load();
    }

    private void load() {
        usersContainer.getChildren().clear();
        usersContainer.getChildren().add(ComponentFactory.createLoadingState());
        Task<List<Profile>> task = new Task<>() {
            @Override protected List<Profile> call() { return profileRepo.findAll(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> { allUsers = task.getValue(); filter(); }));
        new Thread(task).start();
    }

    private void filter() {
        if (allUsers == null) return;
        String keyword = searchField.getText().toLowerCase().trim();
        String role = roleFilter.getValue();
        List<Profile> filtered = allUsers.stream().filter(p -> {
            boolean matchName = keyword.isEmpty() || p.getFullName().toLowerCase().contains(keyword) || p.getEmail().toLowerCase().contains(keyword);
            boolean matchRole = "All Roles".equals(role) || p.getRole().getValue().equals(role);
            return matchName && matchRole;
        }).toList();
        buildTable(filtered);
    }

    private void buildTable(List<Profile> users) {
        usersContainer.getChildren().clear();
        countLabel.setText(users.size() + " users");
        if (users.isEmpty()) {
            usersContainer.getChildren().add(ComponentFactory.createEmptyState("👥", "No users found"));
            return;
        }
        // Header
        HBox header = new HBox(16);
        header.setPadding(new Insets(8, 16, 8, 16));
        header.getChildren().addAll(hdr("Name", 200), hdr("Email", 260), hdr("Role", 100), hdr("Status", 90), hdr("Actions", 120));
        usersContainer.getChildren().add(header);

        for (Profile p : users) {
            HBox row = new HBox(16);
            row.getStyleClass().add("card");
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(p.getFullName());
            name.setPrefWidth(200);
            name.setStyle("-fx-font-weight: bold;");

            Label email = new Label(p.getEmail());
            email.setPrefWidth(260);
            email.getStyleClass().add("text-secondary");

            HBox roleBox = new HBox(ComponentFactory.createStatusBadge(p.getRole().getValue(), p.getRole().getValue()));
            roleBox.setPrefWidth(100);

            HBox statusBox = new HBox(ComponentFactory.createStatusBadge(p.isActive() ? "Active" : "Inactive", p.isActive() ? "active" : "danger"));
            statusBox.setPrefWidth(90);

            Button toggle = new Button(p.isActive() ? "Deactivate" : "Activate");
            toggle.getStyleClass().addAll(p.isActive() ? "btn-danger" : "btn-success", "btn-sm");
            toggle.setOnAction(e -> {
                if (p.isActive()) profileRepo.deactivate(p.getId());
                else profileRepo.activate(p.getId());
                load();
            });
            HBox actBox = new HBox(toggle);
            actBox.setPrefWidth(120);

            row.getChildren().addAll(name, email, roleBox, statusBox, actBox);
            usersContainer.getChildren().add(row);
        }
    }

    private Label hdr(String text, double w) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        l.setPrefWidth(w);
        return l;
    }
}
