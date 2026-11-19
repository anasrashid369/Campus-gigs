package com.campusgigs.service.impl;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dto.TransactionDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.model.Wallet;
import com.campusgigs.service.TransactionService;

import java.util.List;

/** Default {@link TransactionService}. */
public class TransactionServiceImpl implements TransactionService {

    private final WalletDao walletDao;
    private final TransactionDao transactionDao;

    public TransactionServiceImpl(WalletDao walletDao, TransactionDao transactionDao) {
        this.walletDao = walletDao;
        this.transactionDao = transactionDao;
    }

    @Override
    public List<TransactionDto> getHistory(long userId) {
        Wallet wallet = walletDao.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return transactionDao.findByWalletId(wallet.walletId()).stream()
                .map(t -> new TransactionDto(t.transactionId(), t.type(), t.amount(),
                        t.gigId(), t.reference(), t.createdAt()))
                .toList();
    }
}
