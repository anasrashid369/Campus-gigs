package com.campusgigs.presentation.common;

import com.campusgigs.dto.UserDto;
import com.campusgigs.service.UserService;
import com.campusgigs.util.SessionManager;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Basic profile of the logged-in user (F3): shows email and role, edits the full name. */
public class ProfilePanel extends BasePanel {

    private final UserService userService;

    private final Label emailValue = new Label();
    private final Label roleValue = new Label();
    private final TextField nameField = new TextField();
    private final Label messageLabel = FormLayout.messageLabel();
    private final Button saveButton = new Button("Save");

    public ProfilePanel(UserService userService) {
        this.userService = userService;

        VBox form = FormLayout.createForm("My profile");
        FormLayout.addField(form, "Email", emailValue, null);
        FormLayout.addField(form, "Role", roleValue, null);
        FormLayout.addField(form, "Full name", nameField, null);
        FormLayout.addRow(form, messageLabel);
        saveButton.getStyleClass().add("primary");
        FormLayout.addRow(form, saveButton);
        setCenteredContent(form);

        saveButton.setOnAction(event -> save());
        load();
    }

    private void load() {
        long userId = SessionManager.getInstance().getCurrentUser().userId();
        runInBackground(() -> userService.getUser(userId), this::display);
    }

    private void save() {
        long userId = SessionManager.getInstance().getCurrentUser().userId();
        String name = nameField.getText();
        saveButton.setDisable(true);
        runInBackground(() -> userService.updateProfile(userId, name), updated -> {
            saveButton.setDisable(false);
            SessionManager.getInstance().setCurrentUser(updated);
            display(updated);
            FormLayout.showSuccess(messageLabel, "Profile saved");
            DashboardPanel dashboard = dashboard();
            if (dashboard != null) {
                dashboard.refreshHeader();
            }
        }, error -> {
            saveButton.setDisable(false);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }

    private DashboardPanel dashboard() {
        for (Parent parent = getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof DashboardPanel dashboard) {
                return dashboard;
            }
        }
        return null;
    }

    private void display(UserDto user) {
        emailValue.setText(user.email());
        roleValue.setText(user.role().name());
        nameField.setText(user.fullName());
    }
}
