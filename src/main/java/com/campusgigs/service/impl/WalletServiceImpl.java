package com.campusgigs.service.impl;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dto.WalletDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.Wallet;
import com.campusgigs.payment.PaymentGateway;
import com.campusgigs.payment.PaymentRequest;
import com.campusgigs.payment.PaymentResult;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.observer.WalletToppedUp;
import com.campusgigs.service.WalletService;

/** Default {@link WalletService}. */
public class WalletServiceImpl implements WalletService {

    private final WalletDao walletDao;
    private final TransactionDao transactionDao;
    private final PaymentGateway paymentGateway;
    private final TransactionManager transactions;
    private final EventBus eventBus;

    public WalletServiceImpl(WalletDao walletDao, TransactionDao transactionDao,
                             PaymentGateway paymentGateway, TransactionManager transactions,
                             EventBus eventBus) {
        this.walletDao = walletDao;
        this.transactionDao = transactionDao;
        this.paymentGateway = paymentGateway;
        this.transactions = transactions;
        this.eventBus = eventBus;
    }

    @Override
    public WalletDto createWallet(long userId, long initialBalance) {
        if (initialBalance < 0) {
            throw new ValidationException("Initial balance cannot be negative");
        }
        return transactions.call(() -> {
            if (walletDao.findByUserIdForUpdate(userId).isPresent()) {
                throw new ValidationException("This user already has a wallet");
            }
            Wallet wallet = walletDao.create(userId, initialBalance);
            if (initialBalance > 0) {
                transactionDao.insert(wallet.walletId(), null, TransactionType.TOPUP,
                        initialBalance, "Initial balance");
            }
            return toDto(wallet);
        });
    }

    @Override
    public WalletDto getWallet(long userId) {
        return toDto(walletDao.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found")));
    }

    @Override
    public WalletDto topUp(long userId, long amount) {
        if (amount < MIN_TOP_UP || amount > MAX_TOP_UP) {
            throw new ValidationException("Top-up must be between " + MIN_TOP_UP
                    + " and " + MAX_TOP_UP + " tokens");
        }
        walletDao.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        PaymentResult payment = paymentGateway.process(new PaymentRequest(userId, amount, "Wallet top-up"));
        if (!payment.success()) {
            throw new ValidationException("Payment was declined: " + payment.message());
        }

        WalletDto updated = transactions.call(() -> {
            Wallet wallet = walletDao.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
            long newAvailable = Math.addExact(wallet.availableBalance(), amount);
            walletDao.updateBalances(wallet.walletId(), newAvailable, wallet.escrowBalance());
            transactionDao.insert(wallet.walletId(), null, TransactionType.TOPUP, amount, payment.reference());
            return new WalletDto(wallet.walletId(), userId, newAvailable, wallet.escrowBalance());
        });
        eventBus.publish(new WalletToppedUp(userId, amount));
        return updated;
    }

    private static WalletDto toDto(Wallet wallet) {
        return new WalletDto(wallet.walletId(), wallet.userId(),
                wallet.availableBalance(), wallet.escrowBalance());
    }
}
