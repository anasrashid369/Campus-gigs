package com.campusgigs.presentation.auth;

import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.AuthService;
import com.campusgigs.util.Validators;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.util.Arrays;

/** Login screen. Checks only that both fields are filled; the real check is in {@link AuthService}. */
public class LoginPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final transient AuthService authService;
    private final MainFrame frame;

    private final JTextField emailField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS);
    private final JPasswordField passwordField = new JPasswordField(UiConstants.TEXT_FIELD_COLUMNS);
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final JButton loginButton = new JButton("Log in");

    public LoginPanel(AuthService authService, MainFrame frame) {
        this.authService = authService;
        this.frame = frame;
        setLayout(new GridBagLayout());

        JPanel form = FormLayout.createForm("Log in to " + UiConstants.APP_TITLE);
        FormLayout.addField(form, "Email", emailField, null);
        FormLayout.addField(form, "Password", passwordField, null);
        FormLayout.addRow(form, messageLabel, UiConstants.SPACING_MEDIUM);
        FormLayout.addRow(form, loginButton, UiConstants.SPACING_MEDIUM);
        JButton registerLink = new JButton("No account yet? Register");
        registerLink.setBorderPainted(false);
        registerLink.setContentAreaFilled(false);
        registerLink.setForeground(UiConstants.PRIMARY);
        FormLayout.addRow(form, registerLink, 0);
        add(form);

        loginButton.addActionListener(e -> logIn());
        passwordField.addActionListener(e -> logIn());
        registerLink.addActionListener(e -> frame.showCard(MainFrame.REGISTER_CARD));
    }

    private void logIn() {
        String email = emailField.getText();
        char[] typed = passwordField.getPassword();
        String password = new String(typed);
        Arrays.fill(typed, '\0');
        if (Validators.isBlank(email) || password.isEmpty()) {
            messageLabel.setText("Please enter your email and password");
            return;
        }
        messageLabel.setText(FormLayout.NO_MESSAGE);
        loginButton.setEnabled(false);
        runInBackground(() -> authService.login(email, password), user -> {
            loginButton.setEnabled(true);
            passwordField.setText("");
            frame.startSession(user);
        }, error -> {
            loginButton.setEnabled(true);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }
}
