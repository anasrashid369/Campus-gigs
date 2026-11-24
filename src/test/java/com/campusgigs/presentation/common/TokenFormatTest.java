package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.campusgigs.model.TransactionType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TokenFormatTest {

    @Test
    void groupsThousands() {
        assertEquals("0", TokenFormat.tokens(0));
        assertEquals("999", TokenFormat.tokens(999));
        assertEquals("1,250", TokenFormat.tokens(1250));
        assertEquals("1,000,000", TokenFormat.tokens(1_000_000));
    }

    @Test
    void formatsDateAndTime() {
        assertEquals("2026-11-23 09:05", TokenFormat.dateTime(LocalDateTime.of(2026, 11, 23, 9, 5, 59)));
    }

    @Test
    void everyLedgerTypeHasAReadableLabel() {
        for (TransactionType type : TransactionType.values()) {
            String label = TokenFormat.typeLabel(type);
            assertFalse(label.isBlank());
            assertFalse(label.contains("_"));
        }
        assertEquals("Top-up", TokenFormat.typeLabel(TransactionType.TOPUP));
        assertEquals("Escrow release", TokenFormat.typeLabel(TransactionType.ESCROW_RELEASE));
    }
}
