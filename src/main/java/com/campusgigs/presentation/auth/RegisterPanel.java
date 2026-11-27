package com.campusgigs.presentation.auth;

import com.campusgigs.dto.RegisterRequest;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.AuthService;
import com.campusgigs.util.Validators;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Registration screen for clients and freelancers. Gives quick feedback next to each
 * field; the full rules (password strength, duplicate email) are checked by {@link AuthService}.
 */
public class RegisterPanel extends BasePanel {

    private final AuthService authService;
    private final MainFrame frame;

    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final RadioButton clientOption = new RadioButton("Client (I want to hire)");
    private final RadioButton freelancerOption = new RadioButton("Freelancer (I want to work)");
    private final Label nameMessage = FormLayout.messageLabel();
    private final Label emailMessage = FormLayout.messageLabel();
    private final Label passwordMessage = FormLayout.messageLabel();
    private final Label confirmMessage = FormLayout.messageLabel();
    private final Label formMessage = FormLayout.messageLabel();
    private final Button registerButton = new Button("Create account");

    public RegisterPanel(AuthService authService, MainFrame frame) {
        this.authService = authService;
        this.frame = frame;
        getStyleClass().add("auth-screen");

        VBox form = FormLayout.createForm("Create your account");
        FormLayout.addField(form, "Full name", nameField, nameMessage);
        FormLayout.addField(form, "Email", emailField, emailMessage);
        FormLayout.addField(form, "Password (8+ characters, a letter and a digit)", passwordField, passwordMessage);
        FormLayout.addField(form, "Confirm password", confirmField, confirmMessage);
        FormLayout.addField(form, "I am a", createRoleChoice(), null);
        FormLayout.addRow(form, formMessage);
        registerButton.getStyleClass().add("primary");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        FormLayout.addRow(form, registerButton);
        Hyperlink loginLink = new Hyperlink("Already have an account? Log in");
        FormLayout.addRow(form, loginLink);
        setCenteredContent(form);

        registerButton.setOnAction(event -> register());
        confirmField.setOnAction(event -> register());
        loginLink.setOnAction(event -> frame.showCard(MainFrame.LOGIN_CARD));
    }

    private HBox createRoleChoice() {
        ToggleGroup roleGroup = new ToggleGroup();
        clientOption.setToggleGroup(roleGroup);
        freelancerOption.setToggleGroup(roleGroup);
        clientOption.setSelected(true);
        return new HBox(UiConstants.SPACING_LARGE, clientOption, freelancerOption);
    }

    private void register() {
        String password = passwordField.getText();
        String confirm = confirmField.getText();
        if (!inputLooksComplete(password, confirm)) {
            return;
        }
        RegisterRequest request = new RegisterRequest(nameField.getText(), emailField.getText(), password,
                freelancerOption.isSelected() ? Role.FREELANCER : Role.CLIENT);
        registerButton.setDisable(true);
        runInBackground(() -> authService.register(request), user -> {
            registerButton.setDisable(false);
            clearForm();
            frame.startSession(user);
        }, error -> {
            registerButton.setDisable(false);
            FormLayout.showFailure(this, formMessage, error);
        });
    }

    /** Quick checks shown next to each field; returns false when something is missing. */
    private boolean inputLooksComplete(String password, String confirm) {
        boolean ok = true;
        ok &= setMessage(nameMessage, Validators.isBlank(nameField.getText()) ? "Please enter your name" : null);
        ok &= setMessage(emailMessage, Validators.isValidEmail(emailField.getText().trim())
                ? null : "Please enter a valid email address");
        ok &= setMessage(passwordMessage, Validators.hasMinLength(password, AuthService.MIN_PASSWORD_LENGTH)
                ? null : "At least " + AuthService.MIN_PASSWORD_LENGTH + " characters");
        ok &= setMessage(confirmMessage, password.equals(confirm) ? null : "Passwords do not match");
        FormLayout.clear(formMessage);
        return ok;
    }

    /** Sets the message (or clears it when null) and returns true when there was none. */
    private static boolean setMessage(Label label, String message) {
        if (message == null) {
            FormLayout.clear(label);
        } else {
            FormLayout.showError(label, message);
        }
        return message == null;
    }

    private void clearForm() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmField.clear();
        clientOption.setSelected(true);
    }
}
