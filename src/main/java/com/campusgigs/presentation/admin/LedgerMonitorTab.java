package com.campusgigs.presentation.admin;

import com.campusgigs.dto.LedgerEntryDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.TransactionType;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.Choice;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.MonitoringService;
import com.campusgigs.util.SessionManager;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** The token ledger of all wallets, filtered by type and by a range of days (F9). */
class LedgerMonitorTab extends BasePanel {

    private static final long serialVersionUID = 1L;
    private static final String NO_VALUE = "-";

    private final transient MonitoringService monitoringService;
    private final JComboBox<Choice<TransactionType>> typeBox = new JComboBox<>();
    private final JTextField fromField = new JTextField(9);
    private final JTextField toField = new JTextField(9);
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final MonitorTable<LedgerEntryDto> table;

    LedgerMonitorTab(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        setLayout(new BorderLayout(0, UiConstants.SPACING_MEDIUM));
        typeBox.addItem(new Choice<>("All types", null));
        for (TransactionType type : TransactionType.values()) {
            typeBox.addItem(new Choice<>(TokenFormat.typeLabel(type), type));
        }
        table = new MonitorTable<>("Ledger (latest 500 entries)",
                List.of("Date", "User", "Type", "Amount", "Gig", "Reference"), LedgerMonitorTab::toCells, this::load);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, UiConstants.SPACING_MEDIUM, 0));
        bar.setOpaque(false);
        bar.add(new JLabel("Type:"));
        bar.add(typeBox);
        bar.add(new JLabel("From (yyyy-MM-dd):"));
        bar.add(fromField);
        bar.add(new JLabel("To:"));
        bar.add(toField);
        JButton show = new JButton("Show");
        show.addActionListener(e -> load());
        bar.add(show);
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(bar);
        top.add(messageLabel);
        add(top, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        load();
    }

    /**
     * Reads a day typed as yyyy-MM-dd; a blank text means "no limit".
     *
     * @throws ValidationException with a message for the user if the text is not a date
     */
    static LocalDate parseDay(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Enter days as yyyy-MM-dd, for example 2026-11-24.");
        }
    }

    /** The cell values of one ledger entry, in column order. */
    static Object[] toCells(LedgerEntryDto entry) {
        return new Object[] {
                TokenFormat.dateTime(entry.createdAt()),
                entry.userName(),
                TokenFormat.typeLabel(entry.type()),
                TokenFormat.tokens(entry.amount()),
                entry.gigId() == null ? NO_VALUE : String.valueOf(entry.gigId()),
                entry.reference() == null || entry.reference().isBlank() ? NO_VALUE : entry.reference()
        };
    }

    private void load() {
        LocalDate from;
        LocalDate to;
        try {
            from = parseDay(fromField.getText());
            to = parseDay(toField.getText());
        } catch (ValidationException e) {
            FormLayout.showFailure(this, messageLabel, e);
            return;
        }
        messageLabel.setText(FormLayout.NO_MESSAGE);
        long adminId = SessionManager.getInstance().getCurrentUser().userId();
        @SuppressWarnings("unchecked")
        Choice<TransactionType> chosen = (Choice<TransactionType>) typeBox.getSelectedItem();
        TransactionType type = chosen == null ? null : chosen.value();
        runInBackground(() -> monitoringService.listLedger(adminId, type, from, to), table::show,
                error -> FormLayout.showFailure(this, messageLabel, error));
    }
}
