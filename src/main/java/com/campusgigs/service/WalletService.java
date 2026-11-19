package com.campusgigs.service;

import com.campusgigs.dto.WalletDto;

/** Wallet operations. Every balance change is one transaction with a ledger entry. */
public interface WalletService {

    /** Smallest allowed single top-up, in tokens. */
    long MIN_TOP_UP = 1;

    /** Largest allowed single top-up, in tokens. */
    long MAX_TOP_UP = 10_000;

    /**
     * Creates the user's wallet (called at registration). A positive initial balance
     * is recorded in the ledger as a TOPUP entry so ledger totals match balances.
     *
     * @throws com.campusgigs.exception.ValidationException if the balance is negative or the user already has a wallet
     */
    WalletDto createWallet(long userId, long initialBalance);

    /**
     * Returns the user's wallet.
     *
     * @throws com.campusgigs.exception.ResourceNotFoundException if the user has no wallet
     */
    WalletDto getWallet(long userId);

    /**
     * Adds tokens through the payment gateway.
     *
     * @throws com.campusgigs.exception.ValidationException if the amount is outside the allowed range or the payment is declined
     * @throws com.campusgigs.exception.ResourceNotFoundException if the user has no wallet
     */
    WalletDto topUp(long userId, long amount);
}
