package com.campusgigs.service.impl;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dto.PayoutDto;
import com.campusgigs.dto.WalletDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.Wallet;
import com.campusgigs.service.EscrowService;

/** Default {@link EscrowService}. */
public class EscrowServiceImpl implements EscrowService {

    private final WalletDao walletDao;
    private final TransactionDao transactionDao;
    private final TransactionManager transactions;

    public EscrowServiceImpl(WalletDao walletDao, TransactionDao transactionDao,
                             TransactionManager transactions) {
        this.walletDao = walletDao;
        this.transactionDao = transactionDao;
        this.transactions = transactions;
    }

    @Override
    public WalletDto holdInEscrow(long clientId, long gigId, long amount) {
        requirePositive(amount);
        return transactions.call(() -> {
            Wallet wallet = lock(clientId);
            if (wallet.availableBalance() < amount) {
                throw new ValidationException("Not enough tokens: " + wallet.availableBalance()
                        + " available, " + amount + " needed");
            }
            long available = wallet.availableBalance() - amount;
            long escrow = Math.addExact(wallet.escrowBalance(), amount);
            walletDao.updateBalances(wallet.walletId(), available, escrow);
            transactionDao.insert(wallet.walletId(), gigId, TransactionType.ESCROW_HOLD, amount,
                    "Budget held for gig #" + gigId);
            return new WalletDto(wallet.walletId(), clientId, available, escrow);
        });
    }

    @Override
    public PayoutDto releaseToFreelancer(long clientId, long freelancerId, long gigId, long amount,
                                         int feePercent) {
        requirePositive(amount);
        if (feePercent < 0 || feePercent > 100) {
            throw new ValidationException("The platform fee must be between 0 and 100 percent");
        }
        if (clientId == freelancerId) {
            throw new ValidationException("The client and the freelancer must be different users");
        }
        long fee = Math.multiplyExact(amount, (long) feePercent) / 100;
        long net = amount - fee;

        return transactions.call(() -> {
            // Lock both wallets in ascending user id order so concurrent payouts cannot deadlock.
            Wallet client;
            Wallet freelancer;
            if (clientId < freelancerId) {
                client = lock(clientId);
                freelancer = lock(freelancerId);
            } else {
                freelancer = lock(freelancerId);
                client = lock(clientId);
            }
            requireEscrow(client, amount);

            walletDao.updateBalances(client.walletId(), client.availableBalance(),
                    client.escrowBalance() - amount);
            walletDao.updateBalances(freelancer.walletId(),
                    Math.addExact(freelancer.availableBalance(), net), freelancer.escrowBalance());

            transactionDao.insert(client.walletId(), gigId, TransactionType.ESCROW_RELEASE, amount,
                    "Paid out for gig #" + gigId);
            transactionDao.insert(freelancer.walletId(), gigId, TransactionType.ESCROW_RELEASE, amount,
                    "Payment for gig #" + gigId);
            if (fee > 0) {
                transactionDao.insert(freelancer.walletId(), gigId, TransactionType.PLATFORM_FEE, fee,
                        "Platform fee for gig #" + gigId);
            }
            return new PayoutDto(amount, fee, net);
        });
    }

    @Override
    public WalletDto refundToClient(long clientId, long gigId, long amount) {
        requirePositive(amount);
        return transactions.call(() -> {
            Wallet wallet = lock(clientId);
            requireEscrow(wallet, amount);
            long escrow = wallet.escrowBalance() - amount;
            long available = Math.addExact(wallet.availableBalance(), amount);
            walletDao.updateBalances(wallet.walletId(), available, escrow);
            transactionDao.insert(wallet.walletId(), gigId, TransactionType.ESCROW_REFUND, amount,
                    "Refund for gig #" + gigId);
            return new WalletDto(wallet.walletId(), clientId, available, escrow);
        });
    }

    private Wallet lock(long userId) {
        return walletDao.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new ValidationException("The amount must be greater than zero");
        }
    }

    private static void requireEscrow(Wallet clientWallet, long amount) {
        if (clientWallet.escrowBalance() < amount) {
            throw new ValidationException("The escrow balance is lower than the gig amount");
        }
    }
}
