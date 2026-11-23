package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dto.PayoutDto;
import com.campusgigs.dto.WalletDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.Wallet;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EscrowServiceImplTest {

    private static final long CLIENT = 1;
    private static final long FREELANCER = 2;
    private static final long GIG = 10;

    @Mock WalletDao walletDao;
    @Mock TransactionDao transactionDao;

    private EscrowServiceImpl service;

    private final TransactionManager passThrough = new TransactionManager() {
        @Override
        public <T> T call(Supplier<T> work) {
            return work.get();
        }

        @Override
        public void run(Runnable work) {
            work.run();
        }
    };

    @BeforeEach
    void setUp() {
        service = new EscrowServiceImpl(walletDao, transactionDao, passThrough);
    }

    private void clientWallet(long available, long escrow) {
        when(walletDao.findByUserIdForUpdate(CLIENT)).thenReturn(Optional.of(new Wallet(100, CLIENT, available, escrow, 0)));
    }

    private void freelancerWallet(long available) {
        when(walletDao.findByUserIdForUpdate(FREELANCER)).thenReturn(Optional.of(new Wallet(200, FREELANCER, available, 0, 0)));
    }

    // ---- holdInEscrow

    @Test
    void holdMovesTokensFromAvailableToEscrowAndWritesLedger() {
        clientWallet(1000, 50);

        WalletDto result = service.holdInEscrow(CLIENT, GIG, 600);

        assertEquals(400, result.availableBalance());
        assertEquals(650, result.escrowBalance());
        verify(walletDao).updateBalances(100, 400, 650);
        verify(transactionDao).insert(100, GIG, TransactionType.ESCROW_HOLD, 600, "Budget held for gig #10");
    }

    @Test
    void holdCanUseTheWholeBalance() {
        clientWallet(500, 0);

        assertEquals(0, service.holdInEscrow(CLIENT, GIG, 500).availableBalance());
    }

    @Test
    void holdWithTooFewTokensChangesNothing() {
        clientWallet(499, 0);

        assertThrows(ValidationException.class, () -> service.holdInEscrow(CLIENT, GIG, 500));

        verify(walletDao, never()).updateBalances(anyLong(), anyLong(), anyLong());
        verifyNoInteractions(transactionDao);
    }

    @Test
    void holdRejectsZeroAndNegativeAmounts() {
        assertThrows(ValidationException.class, () -> service.holdInEscrow(CLIENT, GIG, 0));
        assertThrows(ValidationException.class, () -> service.holdInEscrow(CLIENT, GIG, -5));
        verifyNoInteractions(walletDao, transactionDao);
    }

    @Test
    void holdWithoutAWalletFails() {
        when(walletDao.findByUserIdForUpdate(CLIENT)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.holdInEscrow(CLIENT, GIG, 10));
    }

    // ---- releaseToFreelancer

    @Test
    void releasePaysTheFreelancerMinusTheFee() {
        clientWallet(0, 1000);
        freelancerWallet(20);

        PayoutDto payout = service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 1000, 5);

        assertEquals(new PayoutDto(1000, 50, 950), payout);
        verify(walletDao).updateBalances(100, 0, 0);
        verify(walletDao).updateBalances(200, 970, 0);
        verify(transactionDao).insert(100, GIG, TransactionType.ESCROW_RELEASE, 1000, "Paid out for gig #10");
        verify(transactionDao).insert(200, GIG, TransactionType.ESCROW_RELEASE, 1000, "Payment for gig #10");
        verify(transactionDao).insert(200, GIG, TransactionType.PLATFORM_FEE, 50, "Platform fee for gig #10");
    }

    @Test
    void feeIsRoundedDownSoNoTokenIsCreated() {
        clientWallet(0, 999);
        freelancerWallet(0);

        PayoutDto payout = service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 999, 5);

        assertEquals(49, payout.feeAmount());
        assertEquals(950, payout.netAmount());
        assertEquals(999, payout.feeAmount() + payout.netAmount());
    }

    @Test
    void zeroFeeWritesNoFeeEntry() {
        clientWallet(0, 100);
        freelancerWallet(0);

        PayoutDto payout = service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 100, 0);

        assertEquals(new PayoutDto(100, 0, 100), payout);
        verify(transactionDao, never()).insert(anyLong(), anyLong(), org.mockito.ArgumentMatchers.eq(TransactionType.PLATFORM_FEE),
                anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void hundredPercentFeeLeavesNothingForTheFreelancer() {
        clientWallet(0, 100);
        freelancerWallet(5);

        assertEquals(new PayoutDto(100, 100, 0),
                service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 100, 100));
        verify(walletDao).updateBalances(200, 5, 0);
    }

    @Test
    void releaseLocksTheLowerUserIdFirst() {
        // freelancer id 2 is locked before client id 5 to avoid deadlocks
        when(walletDao.findByUserIdForUpdate(5)).thenReturn(Optional.of(new Wallet(500, 5, 0, 100, 0)));
        when(walletDao.findByUserIdForUpdate(2)).thenReturn(Optional.of(new Wallet(200, 2, 0, 0, 0)));

        service.releaseToFreelancer(5, 2, GIG, 100, 10);

        InOrder order = inOrder(walletDao);
        order.verify(walletDao).findByUserIdForUpdate(2);
        order.verify(walletDao).findByUserIdForUpdate(5);
    }

    @Test
    void releaseWithTooLittleEscrowChangesNothing() {
        clientWallet(0, 99);
        freelancerWallet(0);

        assertThrows(ValidationException.class,
                () -> service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 100, 5));

        verify(walletDao, never()).updateBalances(anyLong(), anyLong(), anyLong());
        verifyNoInteractions(transactionDao);
    }

    @Test
    void releaseRejectsBadArguments() {
        assertThrows(ValidationException.class, () -> service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 0, 5));
        assertThrows(ValidationException.class, () -> service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 10, -1));
        assertThrows(ValidationException.class, () -> service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 10, 101));
        assertThrows(ValidationException.class, () -> service.releaseToFreelancer(CLIENT, CLIENT, GIG, 10, 5));
        verifyNoInteractions(walletDao, transactionDao);
    }

    @Test
    void releaseWithAMissingFreelancerWalletFails() {
        clientWallet(0, 100);
        when(walletDao.findByUserIdForUpdate(FREELANCER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.releaseToFreelancer(CLIENT, FREELANCER, GIG, 100, 5));
    }

    // ---- refundToClient

    @Test
    void refundMovesTokensBackToAvailable() {
        clientWallet(100, 400);

        WalletDto result = service.refundToClient(CLIENT, GIG, 400);

        assertEquals(500, result.availableBalance());
        assertEquals(0, result.escrowBalance());
        verify(walletDao).updateBalances(100, 500, 0);
        verify(transactionDao).insert(100, GIG, TransactionType.ESCROW_REFUND, 400, "Refund for gig #10");
    }

    @Test
    void refundWithTooLittleEscrowChangesNothing() {
        clientWallet(100, 399);

        assertThrows(ValidationException.class, () -> service.refundToClient(CLIENT, GIG, 400));

        verify(walletDao, never()).updateBalances(anyLong(), anyLong(), anyLong());
        verifyNoInteractions(transactionDao);
    }

    @Test
    void refundRejectsNonPositiveAmounts() {
        assertThrows(ValidationException.class, () -> service.refundToClient(CLIENT, GIG, 0));
        verifyNoInteractions(walletDao, transactionDao);
    }
}
