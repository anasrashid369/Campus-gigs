package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dto.TransactionDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.Wallet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock WalletDao walletDao;
    @Mock TransactionDao transactionDao;
    @InjectMocks TransactionServiceImpl service;

    @Test
    void mapsLedgerRowsToDtosInOrder() {
        LocalDateTime now = LocalDateTime.now();
        when(walletDao.findByUserId(1)).thenReturn(Optional.of(new Wallet(7, 1, 0, 0, 0)));
        when(transactionDao.findByWalletId(7)).thenReturn(List.of(
                new Transaction(2, 7, null, TransactionType.WITHDRAWAL, 5, "w", now),
                new Transaction(1, 7, 3L, TransactionType.ESCROW_HOLD, 20, "h", now)));

        List<TransactionDto> history = service.getHistory(1);

        assertEquals(2, history.size());
        assertEquals(TransactionType.WITHDRAWAL, history.get(0).type());
        assertEquals(3L, history.get(1).gigId());
    }

    @Test
    void failsWhenUserHasNoWallet() {
        when(walletDao.findByUserId(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getHistory(1));
    }
}
