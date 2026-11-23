package com.campusgigs.service;

import com.campusgigs.dto.WalletDto;

/** Wallet operations. Every balance change is one transaction with a ledger entry. */
public interface WalletService {

    /** Smallest allowed single top-up, in tokens. */
    long MIN_TOP_UP = 1;

    /** Largest allowed single top-up, in tokens. */
    long MAX_TOP_UP = 10_000;

    /** Smallest allowed single withdrawal, in tokens. */
    long MIN_WITHDRAWAL = 1;

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
     * Adds tokens through the payment gateway and, after the transaction commits,
     * publishes a {@code WalletToppedUp} event.
     *
     * @throws com.campusgigs.exception.ValidationException if the amount is outside the allowed range, the user is not a client, or the payment is declined
     * @throws com.campusgigs.exception.ResourceNotFoundException if the user has no wallet
     */
    WalletDto topUp(long userId, long amount);

    /**
     * Simulated withdrawal of tokens by a freelancer. Reduces the available balance and
     * writes a WITHDRAWAL ledger entry in one transaction. No real money moves and the
     * payment gateway is not used.
     *
     * @throws com.campusgigs.exception.ValidationException if the user is not a freelancer, the amount is below the minimum, or it exceeds the available balance
     * @throws com.campusgigs.exception.ResourceNotFoundException if the user or the wallet does not exist
     */
    WalletDto withdraw(long userId, long amount);
}
