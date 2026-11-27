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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** The token ledger of all wallets, filtered by type and by a range of days (F9). */
class LedgerMonitorTab extends BasePanel {

    private static final String NO_VALUE = "-";

    private final MonitoringService monitoringService;
    private final ComboBox<Choice<TransactionType>> typeBox = new ComboBox<>();
    private final DatePicker fromPicker = FormLayout.isoDatePicker();
    private final DatePicker toPicker = FormLayout.isoDatePicker();
    private final Label messageLabel = FormLayout.messageLabel();
    private final MonitorTable<LedgerEntryDto> table;

    LedgerMonitorTab(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
        typeBox.getItems().add(new Choice<>("All types", null));
        for (TransactionType type : TransactionType.values()) {
            typeBox.getItems().add(new Choice<>(TokenFormat.typeLabel(type), type));
        }
        typeBox.getSelectionModel().selectFirst();
        fromPicker.setPrefWidth(140);
        toPicker.setPrefWidth(140);
        table = new MonitorTable<>("Ledger (latest 500 entries)",
                List.of("Date", "User", "Type", "Amount", "Gig", "Reference"), LedgerMonitorTab::toCells, this::load);

        Button show = new Button("Show");
        show.getStyleClass().add("primary");
        show.setOnAction(event -> load());
        FlowPane bar = new FlowPane(UiConstants.SPACING_MEDIUM, UiConstants.SPACING_MEDIUM, new Label("Type:"),
                typeBox, new Label("From:"), fromPicker, new Label("To:"), toPicker, show);
        bar.setAlignment(Pos.CENTER_LEFT);
        VBox top = new VBox(UiConstants.SPACING_SMALL, bar, messageLabel);
        setMargin(top, new Insets(0, 0, UiConstants.SPACING_MEDIUM, 0));
        setTop(top);
        setCenter(table);
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
            from = parseDay(fromPicker.getEditor().getText());
            to = parseDay(toPicker.getEditor().getText());
        } catch (ValidationException e) {
            FormLayout.showFailure(this, messageLabel, e);
            return;
        }
        FormLayout.clear(messageLabel);
        long adminId = SessionManager.getInstance().getCurrentUser().userId();
        Choice<TransactionType> chosen = typeBox.getValue();
        TransactionType type = chosen == null ? null : chosen.value();
        runInBackground(() -> monitoringService.listLedger(adminId, type, from, to), table::show,
                error -> FormLayout.showFailure(this, messageLabel, error));
    }
}
