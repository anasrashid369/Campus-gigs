package com.campusgigs.presentation.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;

/**
 * A screen with a title, a read-only table of rows and a row of buttons, used by the lists.
 * Subclasses load rows with {@link #setRows} and add their buttons with {@link #addAction};
 * buttons that need a selected row are disabled until one is chosen.
 *
 * @param <T> the DTO shown in each row
 */
public abstract class TablePanel<T> extends BasePanel {

    private final TableView<T> table = new TableView<>();
    private final Label heading = new Label();
    private final FlowPane buttons = new FlowPane(UiConstants.SPACING_MEDIUM, UiConstants.SPACING_MEDIUM);
    private final List<Button> selectionButtons = new ArrayList<>();

    /**
     * @param title   the heading above the table
     * @param columns the column headings
     * @param toCells turns one row into its cell values, in column order
     */
    protected TablePanel(String title, List<String> columns, Function<T, Object[]> toCells) {
        for (int i = 0; i < columns.size(); i++) {
            int index = i;
            TableColumn<T, Object> column = new TableColumn<>(columns.get(i));
            column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(toCells.apply(cell.getValue())[index]));
            column.setSortable(false);
            column.setReorderable(false);
            table.getColumns().add(column);
        }
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nothing to show yet"));
        table.getSelectionModel().selectedItemProperty().addListener((observable, before, now) -> updateButtons());

        heading.setText(title);
        heading.getStyleClass().add("screen-title");
        setMargin(heading, new Insets(0, 0, UiConstants.SPACING_MEDIUM + 4, 0));
        buttons.getStyleClass().add("action-bar");

        setTop(heading);
        setCenter(table);
        setBottom(buttons);
    }

    /**
     * Adds a button below the table.
     *
     * @param needsSelection when true the button is enabled only while a row is selected
     */
    protected Button addAction(String text, boolean needsSelection, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        buttons.getChildren().add(button);
        if (needsSelection) {
            selectionButtons.add(button);
            button.setDisable(true);
        }
        return button;
    }

    /** Changes the heading above the table, for example to add a total. */
    protected void setTitle(String title) {
        heading.setText(title);
    }

    /** Sets the preferred column widths in pixels, in column order; extra space is shared out. */
    protected void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumns().size(); i++) {
            table.getColumns().get(i).setPrefWidth(widths[i]);
        }
    }

    /** Replaces the rows; the selection is cleared. */
    protected void setRows(List<T> rows) {
        table.getSelectionModel().clearSelection();
        table.getItems().setAll(rows);
        updateButtons();
    }

    /** The rows currently shown. */
    protected List<T> rows() {
        return List.copyOf(table.getItems());
    }

    /** The selected row, if any. */
    protected Optional<T> selected() {
        return Optional.ofNullable(table.getSelectionModel().getSelectedItem());
    }

    /** Called when the selection changes; override to enable or relabel buttons for the selected row. */
    protected void onSelectionChanged(Optional<T> row) {
    }

    private void updateButtons() {
        Optional<T> row = selected();
        for (Button button : selectionButtons) {
            button.setDisable(row.isEmpty());
        }
        onSelectionChanged(row);
    }
}
