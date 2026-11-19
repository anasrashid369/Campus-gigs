package com.campusgigs.model;

/** Ledger entry type. Names match Transactions.type in the database. */
public enum TransactionType {
    /** Tokens added through the simulated payment gateway. */
    TOPUP,
    /** Gig budget moved from available balance into escrow. */
    ESCROW_HOLD,
    /** Escrowed tokens paid out to the freelancer. */
    ESCROW_RELEASE,
    /** Escrowed tokens returned to the client. */
    ESCROW_REFUND,
    /** Platform fee deducted on release. */
    PLATFORM_FEE,
    /** Simulated withdrawal of tokens by a freelancer. */
    WITHDRAWAL
}
