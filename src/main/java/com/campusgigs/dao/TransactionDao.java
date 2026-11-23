package com.campusgigs.dao;

import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;

import java.util.List;

/** Data access for the Transactions ledger. Contains SQL and mapping only. */
public interface TransactionDao {

    /**
     * Appends a ledger entry. Must run in the same transaction as the wallet update it records.
     *
     * @param gigId related gig, or null
     */
    Transaction insert(long walletId, Long gigId, TransactionType type, long amount, String reference);

    /** Returns the wallet's ledger entries, newest first. */
    List<Transaction> findByWalletId(long walletId);

    /** Returns the total amount of the wallet's ledger entries of one type, or 0 if there are none. */
    long sumByType(long walletId, TransactionType type);
}
