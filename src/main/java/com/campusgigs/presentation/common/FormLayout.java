package com.campusgigs.presentation.common;

import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Builds the simple label-above-field forms used by the login, register and
 * profile screens, so they all look the same.
 */
public final class FormLayout {

    /** Text of an empty message label; keeps its height so the form does not jump. */
    public static final String NO_MESSAGE = " ";

    private FormLayout() {
    }

    /** A white card with a heading; add rows with the other methods. */
    public static JPanel createForm(String heading) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiConstants.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(UiConstants.SPACING_XLARGE, UiConstants.SPACING_XLARGE,
                UiConstants.SPACING_XLARGE, UiConstants.SPACING_XLARGE));
        JLabel title = new JLabel(heading);
        title.setFont(UiConstants.HEADING_FONT);
        title.setForeground(UiConstants.PRIMARY);
        addRow(form, title, UiConstants.SPACING_LARGE);
        return form;
    }

    /** Adds a field with its label above it and, if given, its message label below it. */
    public static void addField(JPanel form, String label, JComponent field, JLabel message) {
        JLabel caption = new JLabel(label);
        caption.setFont(UiConstants.BODY_FONT);
        caption.setLabelFor(field);
        addRow(form, caption, UiConstants.SPACING_SMALL);
        addRow(form, field, message == null ? UiConstants.SPACING_MEDIUM : 0);
        if (message != null) {
            addRow(form, message, UiConstants.SPACING_MEDIUM);
        }
    }

    /** Adds any component as a full-width row. */
    public static void addRow(JPanel form, Component component, int spaceBelow) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, spaceBelow, 0);
        form.add(component, c);
    }

    /** A small red label for validation messages. */
    public static JLabel messageLabel() {
        JLabel label = new JLabel(NO_MESSAGE);
        label.setFont(UiConstants.SMALL_FONT);
        label.setForeground(UiConstants.ERROR);
        return label;
    }

    /**
     * Shows a failure: messages written for the user go into {@code label};
     * anything else opens {@link ErrorDialog} with a generic message.
     */
    public static void showFailure(Component parent, JLabel label, Exception error) {
        if (error instanceof ValidationException || error instanceof ResourceNotFoundException) {
            label.setForeground(UiConstants.ERROR);
            label.setText(error.getMessage());
        } else {
            label.setText(NO_MESSAGE);
            ErrorDialog.show(parent, error);
        }
    }
}
