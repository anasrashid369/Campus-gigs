package com.campusgigs.presentation.common;

import com.campusgigs.dto.TransactionDto;
import java.util.List;

/**
 * Read-only table of a user's token ledger (F6), newest entry first.
 * The owning screen loads the rows with {@link #showHistory}.
 */
public class TransactionHistoryPanel extends TablePanel<TransactionDto> {

    private static final List<String> COLUMNS = List.of("Date", "Type", "Amount", "Gig", "Reference");
    private static final String NO_VALUE = "-";

    public TransactionHistoryPanel() {
        super("Transaction history", COLUMNS, TransactionHistoryPanel::toCells);
    }

    /** Replaces the rows shown. */
    public void showHistory(List<TransactionDto> history) {
        setRows(history);
    }

    /** The cell values of one ledger entry, in column order. */
    static Object[] toCells(TransactionDto entry) {
        return new Object[] {
                TokenFormat.dateTime(entry.createdAt()),
                TokenFormat.typeLabel(entry.type()),
                TokenFormat.tokens(entry.amount()),
                entry.gigId() == null ? NO_VALUE : String.valueOf(entry.gigId()),
                entry.reference() == null || entry.reference().isBlank() ? NO_VALUE : entry.reference()
        };
    }
}
