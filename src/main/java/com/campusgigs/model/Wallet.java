package com.campusgigs.model;

/**
 * A user's wallet (row of Wallets). Balances are whole tokens.
 * {@code rowVersion} supports optimistic locking.
 */
public record Wallet(long walletId, long userId, long availableBalance,
                     long escrowBalance, int rowVersion) {
}
