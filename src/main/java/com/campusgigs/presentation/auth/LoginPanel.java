package com.campusgigs.presentation.auth;

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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Login screen. Checks only that both fields are filled; the real check is in {@link AuthService}. */
public class LoginPanel extends BasePanel {

    private final AuthService authService;
    private final MainFrame frame;

    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label messageLabel = FormLayout.messageLabel();
    private final Button loginButton = new Button("Log in");

    public LoginPanel(AuthService authService, MainFrame frame) {
        this.authService = authService;
        this.frame = frame;
        getStyleClass().add("auth-screen");

        VBox form = FormLayout.createForm("Log in to " + UiConstants.APP_TITLE);
        Label subtitle = new Label("The student marketplace for campus gigs");
        subtitle.getStyleClass().add("muted");
        FormLayout.addRow(form, subtitle);
        emailField.setPromptText("you@university.edu");
        FormLayout.addField(form, "Email", emailField, null);
        FormLayout.addField(form, "Password", passwordField, null);
        FormLayout.addRow(form, messageLabel);
        loginButton.getStyleClass().add("primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        FormLayout.addRow(form, loginButton);
        Hyperlink registerLink = new Hyperlink("No account yet? Register");
        FormLayout.addRow(form, registerLink);
        setCenteredContent(form);

        loginButton.setOnAction(event -> logIn());
        emailField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> logIn());
        registerLink.setOnAction(event -> frame.showCard(MainFrame.REGISTER_CARD));
    }

    private void logIn() {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (Validators.isBlank(email) || password.isEmpty()) {
            FormLayout.showError(messageLabel, "Please enter your email and password");
            return;
        }
        FormLayout.clear(messageLabel);
        loginButton.setDisable(true);
        runInBackground(() -> authService.login(email, password), user -> {
            loginButton.setDisable(false);
            passwordField.clear();
            frame.startSession(user);
        }, error -> {
            loginButton.setDisable(false);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }
}
