package com.campusgigs.dto;

import com.campusgigs.model.TransactionType;

import java.time.LocalDateTime;

/** One line of a user's transaction history. */
public record TransactionDto(long transactionId, TransactionType type, long amount,
                             Long gigId, String reference, LocalDateTime createdAt) {
}
