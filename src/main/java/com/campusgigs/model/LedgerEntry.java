package com.campusgigs.model;

import java.time.LocalDateTime;

/** A ledger entry together with the owner of the wallet it belongs to (row of Transactions joined with Users). */
public record LedgerEntry(long transactionId, long userId, String userName, TransactionType type, long amount,
                          Long gigId, String reference, LocalDateTime createdAt) {
}
