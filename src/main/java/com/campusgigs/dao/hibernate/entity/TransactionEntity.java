package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Row of the Transactions ledger. */
@Entity
@Table(name = "Transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "wallet_id", nullable = false)
    private long walletId;

    @Column(name = "gig_id")
    private Long gigId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "reference")
    private String reference;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TransactionEntity() {
    }

    /** A new ledger entry; the database fills in the id and creation time. */
    public TransactionEntity(long walletId, Long gigId, TransactionType type, long amount, String reference) {
        this.walletId = walletId;
        this.gigId = gigId;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public Transaction toModel() {
        return new Transaction(transactionId, walletId, gigId, type, amount, reference, createdAt);
    }
}
