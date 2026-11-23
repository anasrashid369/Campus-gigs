package com.campusgigs.presentation.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.campusgigs.dto.TransactionDto;
import com.campusgigs.model.TransactionType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TransactionHistoryPanelTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 23, 14, 30);

    @Test
    void mapsAnEntryWithAGigAndReference() {
        TransactionDto entry = new TransactionDto(1, TransactionType.ESCROW_HOLD, 1500, 12L, "Gig funded", WHEN);

        assertArrayEquals(new Object[] {"2026-11-23 14:30", "Escrow hold", "1,500", "12", "Gig funded"},
                TransactionHistoryPanel.toCells(entry));
    }

    @Test
    void showsADashWhenThereIsNoGigOrReference() {
        TransactionDto entry = new TransactionDto(2, TransactionType.TOPUP, 50, null, null, WHEN);

        assertArrayEquals(new Object[] {"2026-11-23 14:30", "Top-up", "50", "-", "-"},
                TransactionHistoryPanel.toCells(entry));
    }
}
