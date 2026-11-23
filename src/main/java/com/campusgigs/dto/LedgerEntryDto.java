package com.campusgigs.dto;

import com.campusgigs.model.TransactionType;

import java.time.LocalDateTime;

/** One line of the platform ledger as the admin monitoring screen shows it. */
public record LedgerEntryDto(long transactionId, String userName, TransactionType type, long amount,
                             Long gigId, String reference, LocalDateTime createdAt) {
}
