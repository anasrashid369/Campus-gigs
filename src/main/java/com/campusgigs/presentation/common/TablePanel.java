package com.campusgigs.presentation.common;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A screen with a title, a read-only table of rows and a row of buttons, used by the
 * admin lists. Subclasses load rows with {@link #setRows} and add their buttons with
 * {@link #addAction}; buttons that need a selected row are disabled until one is chosen.
 *
 * @param <T> the DTO shown in each row
 */
public abstract class TablePanel<T> extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final RowsModel model;
    private final JTable table;
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, UiConstants.SPACING_MEDIUM, 0));
    private final List<JButton> selectionButtons = new ArrayList<>();

    /**
     * @param title   the heading above the table
     * @param columns the column headings
     * @param toCells turns one row into its cell values, in column order
     */
    protected TablePanel(String title, List<String> columns, Function<T, Object[]> toCells) {
        setLayout(new BorderLayout(0, UiConstants.SPACING_MEDIUM));
        model = new RowsModel(List.copyOf(columns), toCells);
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(table.getRowHeight() + UiConstants.SPACING_SMALL);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());

        JLabel heading = new JLabel(title);
        heading.setFont(UiConstants.HEADING_FONT);
        heading.setForeground(UiConstants.PRIMARY);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, UiConstants.SPACING_SMALL, 0));
        buttons.setOpaque(false);

        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Adds a button below the table.
     *
     * @param needsSelection when true the button is enabled only while a row is selected
     */
    protected JButton addAction(String text, boolean needsSelection, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        buttons.add(button);
        if (needsSelection) {
            selectionButtons.add(button);
            button.setEnabled(false);
        }
        return button;
    }

    /** Sets the preferred column widths in pixels, in column order; extra space is shared out. */
    protected void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Replaces the rows; the selection is cleared. */
    protected void setRows(List<T> rows) {
        model.setRows(rows);
        updateButtons();
    }

    /** The selected row, if any. */
    protected Optional<T> selected() {
        int index = table.getSelectedRow();
        return index < 0 ? Optional.empty() : Optional.of(model.rows.get(index));
    }

    /** Called when the selection changes; override to relabel buttons for the selected row. */
    protected void onSelectionChanged(Optional<T> row) {
    }

    private void updateButtons() {
        Optional<T> row = selected();
        for (JButton button : selectionButtons) {
            button.setEnabled(row.isPresent());
        }
        onSelectionChanged(row);
    }

    private final class RowsModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final List<String> columns;
        private final transient Function<T, Object[]> toCells;
        private transient List<T> rows = List.of();

        RowsModel(List<String> columns, Function<T, Object[]> toCells) {
            this.columns = columns;
            this.toCells = toCells;
        }

        void setRows(List<T> newRows) {
            rows = List.copyOf(newRows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column);
        }

        @Override
        public Object getValueAt(int row, int column) {
            return toCells.apply(rows.get(row))[column];
        }
    }
}
