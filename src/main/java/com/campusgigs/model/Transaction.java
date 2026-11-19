package com.campusgigs.model;

import java.time.LocalDateTime;

/** A ledger entry (row of Transactions). The amount is always positive; the type gives the direction. */
public record Transaction(long transactionId, long walletId, Long gigId, TransactionType type,
                          long amount, String reference, LocalDateTime createdAt) {
}
