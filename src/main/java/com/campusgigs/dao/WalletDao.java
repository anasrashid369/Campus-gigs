package com.campusgigs.dao;

import com.campusgigs.model.Wallet;

import java.util.Optional;

/** Data access for the Wallets table. Contains SQL and mapping only. */
public interface WalletDao {

    /** Finds the wallet owned by the user. */
    Optional<Wallet> findByUserId(long userId);

    /**
     * Finds the wallet and locks its row ({@code SELECT ... FOR UPDATE}) until the
     * surrounding transaction ends. Must be called inside a transaction.
     */
    Optional<Wallet> findByUserIdForUpdate(long userId);

    /** Creates a wallet for the user with the given available balance and no escrow. */
    Wallet create(long userId, long availableBalance);

    /** Overwrites both balances of the wallet and increments its row version. */
    void updateBalances(long walletId, long availableBalance, long escrowBalance);
}
