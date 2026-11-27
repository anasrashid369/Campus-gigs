package com.campusgigs.presentation.common;

import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * A small dialog that asks for a longer text, for example a proposal or the reason for a dispute.
 * The length is checked on screen for convenience; the services check it again.
 */
public final class TextPrompt {

    private static final int ROWS = 7;
    private static final int COLUMNS = 44;

    private TextPrompt() {
    }

    /**
     * Asks for a text; the dialog stays open until the length is acceptable or the user cancels.
     *
     * @return the trimmed text, or empty if the user cancelled
     */
    public static Optional<String> ask(Node owner, String title, String label, int minLength, int maxLength) {
        TextArea area = new TextArea();
        area.setWrapText(true);
        area.setPrefRowCount(ROWS);
        area.setPrefColumnCount(COLUMNS);
        Label caption = new Label(label);
        caption.setWrapText(true);
        Label problem = FormLayout.messageLabel();

        Dialog<String> dialog = new Dialog<>();
        dialog.getDialogPane().setContent(new VBox(UiConstants.SPACING_MEDIUM, caption, area, problem));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event -> {
            String message = problem(area.getText(), minLength, maxLength);
            if (message != null) {
                FormLayout.showError(problem, message);
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK ? area.getText().trim() : null);
        Dialogs.prepare(dialog, owner, title);
        return dialog.showAndWait();
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
