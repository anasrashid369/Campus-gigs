package com.campusgigs.presentation.common;

import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Builds the label-above-field forms used by the login, register, profile and gig screens,
 * so they all look the same, and shows their messages.
 */
public final class FormLayout {

    /** Text of an empty message label; keeps its height so the form does not jump. */
    public static final String NO_MESSAGE = " ";

    private static final String[] MESSAGE_KINDS = {"message-error", "message-success", "message-muted"};

    private FormLayout() {
    }

    /** A white card with a heading; add rows with the other methods. */
    public static VBox createForm(String heading) {
        VBox form = new VBox();
        form.getStyleClass().add("form-card");
        form.setPrefWidth(UiConstants.FORM_WIDTH);
        Label title = new Label(heading);
        title.getStyleClass().add("form-title");
        form.getChildren().add(title);
        return form;
    }

    /** Adds a field with its label above it and, if given, its message label below it. */
    public static void addField(VBox form, String label, Node field, Label message) {
        Label caption = new Label(label);
        caption.getStyleClass().add("field-label");
        caption.setWrapText(true);
        caption.setLabelFor(field);
        VBox group = new VBox(UiConstants.SPACING_SMALL + 2, caption, field);
        if (message != null) {
            group.getChildren().add(message);
        }
        form.getChildren().add(group);
    }

    /** Adds any node as a full-width row. */
    public static void addRow(VBox form, Node node) {
        form.getChildren().add(node);
    }

    /** A small label for validation messages. */
    public static Label messageLabel() {
        Label label = new Label(NO_MESSAGE);
        label.getStyleClass().addAll("message", "message-error");
        label.setWrapText(true);
        return label;
    }

    /** Shows a problem in red. */
    public static void showError(Label label, String text) {
        show(label, text, "message-error");
    }

    /** Shows a confirmation in green. */
    public static void showSuccess(Label label, String text) {
        show(label, text, "message-success");
    }

    /** Shows neutral information in grey. */
    public static void showInfo(Label label, String text) {
        show(label, text, "message-muted");
    }

    /** Empties a message label. */
    public static void clear(Label label) {
        label.setText(NO_MESSAGE);
    }

    /**
     * Shows a failure: messages written for the user go into {@code label};
     * anything else opens {@link ErrorDialog} with a generic message.
     */
    public static void showFailure(Node owner, Label label, Exception error) {
        if (error instanceof ValidationException || error instanceof ResourceNotFoundException) {
            showError(label, error.getMessage());
        } else {
            clear(label);
            ErrorDialog.show(owner, error);
        }
    }

    /** Makes a combo box show each item with the given text. */
    public static <T> void showItemsAs(ComboBox<T> box, Function<T, String> text) {
        box.setCellFactory(list -> new NamedCell<>(text));
        box.setButtonCell(new NamedCell<>(text));
    }

    /**
     * A date picker whose text is written as yyyy-MM-dd, so the typed text can be read with
     * {@link LocalDate#parse}; the calendar button fills it in.
     */
    public static DatePicker isoDatePicker() {
        DatePicker picker = new DatePicker();
        picker.setPromptText("yyyy-MM-dd");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.toString();
            }

            @Override
            public LocalDate fromString(String text) {
                try {
                    return text == null || text.isBlank() ? null : LocalDate.parse(text.trim());
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
        return picker;
    }

    private static void show(Label label, String text, String kind) {
        label.getStyleClass().removeAll(MESSAGE_KINDS);
        label.getStyleClass().add(kind);
        label.setText(text);
    }

    private static final class NamedCell<T> extends ListCell<T> {

        private final Function<T, String> text;

        NamedCell(Function<T, String> text) {
            this.text = text;
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : text.apply(item));
        }
    }
}
