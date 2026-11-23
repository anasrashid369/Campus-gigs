package com.campusgigs.presentation.common;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.BoxLayout;
import java.awt.Component;
import java.util.Optional;

/**
 * A small dialog that asks for a longer text, for example a proposal or the reason for a dispute.
 * The length is checked on screen for convenience; the services check it again.
 */
public final class TextPrompt {

    private static final int ROWS = 7;
    private static final int COLUMNS = 40;

    private TextPrompt() {
    }

    /**
     * Asks for a text until the length is acceptable or the user cancels.
     *
     * @return the trimmed text, or empty if the user cancelled
     */
    public static Optional<String> ask(Component parent, String title, String label, int minLength, int maxLength) {
        String problem = null;
        String previous = "";
        while (true) {
            JTextArea area = new JTextArea(previous, ROWS, COLUMNS);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.add(new JLabel(label));
            content.add(new JScrollPane(area));
            if (problem != null) {
                JLabel error = new JLabel(problem);
                error.setForeground(UiConstants.ERROR);
                content.add(error);
            }
            int answer = JOptionPane.showConfirmDialog(parent, content, title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            previous = area.getText();
            problem = problem(previous, minLength, maxLength);
            if (problem == null) {
                return Optional.of(previous.trim());
            }
        }
    }

    /**
     * Checks a typed text.
     *
     * @return a message for the user, or null if the text is acceptable
     */
    static String problem(String text, int minLength, int maxLength) {
        int length = text == null ? 0 : text.trim().length();
        if (length < minLength || length > maxLength) {
            return minLength <= 1
                    ? "Please write between 1 and " + maxLength + " characters."
                    : "Please write between " + minLength + " and " + maxLength + " characters.";
        }
        return null;
    }
}
