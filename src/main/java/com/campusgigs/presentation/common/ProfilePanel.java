package com.campusgigs.presentation.common;

import com.campusgigs.dto.UserDto;
import com.campusgigs.service.UserService;
import com.campusgigs.util.SessionManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.GridBagLayout;

/** Basic profile of the logged-in user (F3): shows email and role, edits the full name. */
public class ProfilePanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final transient UserService userService;

    private final JLabel emailValue = new JLabel();
    private final JLabel roleValue = new JLabel();
    private final JTextField nameField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS);
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final JButton saveButton = new JButton("Save");

    public ProfilePanel(UserService userService) {
        this.userService = userService;
        setLayout(new GridBagLayout());

        JPanel form = FormLayout.createForm("My profile");
        FormLayout.addField(form, "Email", emailValue, null);
        FormLayout.addField(form, "Role", roleValue, null);
        FormLayout.addField(form, "Full name", nameField, null);
        FormLayout.addRow(form, messageLabel, UiConstants.SPACING_MEDIUM);
        FormLayout.addRow(form, saveButton, 0);
        add(form);

        saveButton.addActionListener(e -> save());
        load();
    }

    private void load() {
        long userId = SessionManager.getInstance().getCurrentUser().userId();
        runInBackground(() -> userService.getUser(userId), this::display);
    }

    private void save() {
        long userId = SessionManager.getInstance().getCurrentUser().userId();
        String name = nameField.getText();
        saveButton.setEnabled(false);
        runInBackground(() -> userService.updateProfile(userId, name), updated -> {
            saveButton.setEnabled(true);
            SessionManager.getInstance().setCurrentUser(updated);
            display(updated);
            messageLabel.setForeground(UiConstants.SUCCESS);
            messageLabel.setText("Profile saved");
            DashboardPanel dashboard = (DashboardPanel) SwingUtilities.getAncestorOfClass(DashboardPanel.class, this);
            if (dashboard != null) {
                dashboard.refreshHeader();
            }
        }, error -> {
            saveButton.setEnabled(true);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }

    private void display(UserDto user) {
        emailValue.setText(user.email());
        roleValue.setText(user.role().name());
        nameField.setText(user.fullName());
    }
}
