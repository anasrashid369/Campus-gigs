package com.campusgigs.presentation.common;

import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * The small pop-up windows of the app (questions, messages, longer texts, modal screens),
 * styled like the rest of it. Call on the JavaFX Application Thread.
 */
public final class Dialogs {

    private static final int TEXT_ROWS = 8;
    private static final int TEXT_COLUMNS = 44;

    private Dialogs() {
    }

    /** Asks a yes/no question; true when the user chose Yes. */
    public static boolean confirm(Node owner, String title, String question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, question, ButtonType.YES, ButtonType.NO);
        prepare(alert, owner, title);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    /** Shows a short message with an OK button. */
    public static void info(Node owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        prepare(alert, owner, title);
        alert.showAndWait();
    }

    /** Shows an error message with an OK button. */
    public static void error(Node owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        prepare(alert, owner, "Error");
        alert.showAndWait();
    }

    /**
     * Shows a longer read-only text, for example a proposal or the reason for a dispute.
     *
     * @param lines short lines shown above the text, may be empty
     */
    public static void showText(Node owner, String title, String text, String... lines) {
        Dialog<Void> dialog = new Dialog<>();
        VBox content = new VBox(UiConstants.SPACING_MEDIUM);
        for (String line : lines) {
            Label label = new Label(line);
            label.setWrapText(true);
            content.getChildren().add(label);
        }
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(TEXT_ROWS);
        area.setPrefColumnCount(TEXT_COLUMNS);
        content.getChildren().add(area);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        prepare(dialog, owner, title);
        dialog.showAndWait();
    }

    /**
     * Asks for one line of text, for example a new name.
     *
     * @return the text as typed, or empty if the user cancelled
     */
    public static Optional<String> input(Node owner, String title, String label, String initial) {
        TextInputDialog dialog = new TextInputDialog(initial == null ? "" : initial);
        dialog.setContentText(label);
        prepare(dialog, owner, title);
        return dialog.showAndWait();
    }

    /**
     * A modal window showing a whole screen, for example a gig's applications. It is not shown
     * yet; call {@code showAndWait()} on it.
     */
    public static Stage modal(Node owner, String title, Parent content, double width, double height) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        Window window = windowOf(owner);
        if (window != null) {
            stage.initOwner(window);
        }
        Scene scene = new Scene(content, width, height);
        scene.getStylesheets().add(UiConstants.stylesheet());
        stage.setScene(scene);
        return stage;
    }

    static void prepare(Dialog<?> dialog, Node owner, String title) {
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.getDialogPane().getStylesheets().add(UiConstants.stylesheet());
        dialog.getDialogPane().setMinWidth(420);
        Window window = windowOf(owner);
        if (window != null) {
            dialog.initOwner(window);
        }
    }

    private static Window windowOf(Node owner) {
        return owner == null || owner.getScene() == null ? null : owner.getScene().getWindow();
    }
}
