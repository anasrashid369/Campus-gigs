package com.campusgigs.presentation.auth;

import com.campusgigs.dto.RegisterRequest;
import com.campusgigs.model.Role;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.AuthService;
import com.campusgigs.util.Validators;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.Arrays;

/**
 * Registration screen for clients and freelancers. Gives quick feedback next to each
 * field; the full rules (password strength, duplicate email) are checked by {@link AuthService}.
 */
public class RegisterPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final transient AuthService authService;
    private final MainFrame frame;

    private final JTextField nameField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS);
    private final JTextField emailField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS);
    private final JPasswordField passwordField = new JPasswordField(UiConstants.TEXT_FIELD_COLUMNS);
    private final JPasswordField confirmField = new JPasswordField(UiConstants.TEXT_FIELD_COLUMNS);
    private final JRadioButton clientOption = new JRadioButton("Client (I want to hire)");
    private final JRadioButton freelancerOption = new JRadioButton("Freelancer (I want to work)");
    private final ButtonGroup roleGroup = new ButtonGroup();

    private final JLabel nameMessage = FormLayout.messageLabel();
    private final JLabel emailMessage = FormLayout.messageLabel();
    private final JLabel passwordMessage = FormLayout.messageLabel();
    private final JLabel confirmMessage = FormLayout.messageLabel();
    private final JLabel formMessage = FormLayout.messageLabel();
    private final JButton registerButton = new JButton("Create account");

    public RegisterPanel(AuthService authService, MainFrame frame) {
        this.authService = authService;
        this.frame = frame;
        setLayout(new GridBagLayout());

        JPanel form = FormLayout.createForm("Create your account");
        FormLayout.addField(form, "Full name", nameField, nameMessage);
        FormLayout.addField(form, "Email", emailField, emailMessage);
        FormLayout.addField(form, "Password (8+ characters, a letter and a digit)", passwordField, passwordMessage);
        FormLayout.addField(form, "Confirm password", confirmField, confirmMessage);
        FormLayout.addField(form, "I am a", createRoleChoice(), null);
        FormLayout.addRow(form, formMessage, UiConstants.SPACING_MEDIUM);
        FormLayout.addRow(form, registerButton, UiConstants.SPACING_MEDIUM);
        JButton loginLink = new JButton("Already have an account? Log in");
        loginLink.setBorderPainted(false);
        loginLink.setContentAreaFilled(false);
        loginLink.setForeground(UiConstants.PRIMARY);
        FormLayout.addRow(form, loginLink, 0);
        add(form);

        registerButton.addActionListener(e -> register());
        loginLink.addActionListener(e -> frame.showCard(MainFrame.LOGIN_CARD));
    }

    private JPanel createRoleChoice() {
        JPanel choice = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        choice.setOpaque(false);
        for (JRadioButton option : new JRadioButton[] {clientOption, freelancerOption}) {
            option.setOpaque(false);
            roleGroup.add(option);
            choice.add(option);
        }
        clientOption.setSelected(true);
        return choice;
    }

    private void register() {
        String password = readAndClear(passwordField.getPassword());
        String confirm = readAndClear(confirmField.getPassword());
        if (!inputLooksComplete(password, confirm)) {
            return;
        }
        RegisterRequest request = new RegisterRequest(nameField.getText(), emailField.getText(), password,
                freelancerOption.isSelected() ? Role.FREELANCER : Role.CLIENT);
        registerButton.setEnabled(false);
        runInBackground(() -> authService.register(request), user -> {
            registerButton.setEnabled(true);
            clearForm();
            frame.startSession(user);
        }, error -> {
            registerButton.setEnabled(true);
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
        formMessage.setText(FormLayout.NO_MESSAGE);
        return ok;
    }

    /** Sets the message (or clears it when null) and returns true when there was none. */
    private static boolean setMessage(JLabel label, String message) {
        label.setText(message == null ? FormLayout.NO_MESSAGE : message);
        return message == null;
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        clientOption.setSelected(true);
    }

    private static String readAndClear(char[] typed) {
        String value = new String(typed);
        Arrays.fill(typed, '\0');
        return value;
    }
}
