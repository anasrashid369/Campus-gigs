package com.campusgigs.dao;

import com.campusgigs.model.LedgerEntry;
import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;

import java.time.LocalDate;
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

    /** The most rows {@link #findLedger} returns. */
    int MAX_LEDGER_ROWS = 500;

    /**
     * Lists ledger entries of all wallets with the wallet owner's name, newest first, at most
     * {@value #MAX_LEDGER_ROWS} rows. A null condition is ignored.
     *
     * @param from first day to include (from 00:00)
     * @param to   last day to include (up to 23:59:59)
     */
    List<LedgerEntry> findLedger(TransactionType typeOrNull, LocalDate from, LocalDate to);
}
