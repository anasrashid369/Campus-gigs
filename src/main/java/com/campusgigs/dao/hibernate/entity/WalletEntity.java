package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.Wallet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the Wallets table. */
@Entity
@Table(name = "Wallets")
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "available_balance", nullable = false)
    private long availableBalance;

    @Column(name = "escrow_balance", nullable = false)
    private long escrowBalance;

    @Column(name = "row_version", nullable = false)
    private int rowVersion;

    protected WalletEntity() {
    }

    /** A new wallet with the given available balance and no escrow. */
    public WalletEntity(long userId, long availableBalance) {
        this.userId = userId;
        this.availableBalance = availableBalance;
    }

    public long getWalletId() {
        return walletId;
    }

    public Wallet toModel() {
        return new Wallet(walletId, userId, availableBalance, escrowBalance, rowVersion);
    }
}
