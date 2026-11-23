package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dto.WalletDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.User;
import com.campusgigs.model.Wallet;
import com.campusgigs.payment.PaymentGateway;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.observer.WalletToppedUp;
import com.campusgigs.payment.PaymentResult;
import com.campusgigs.service.WalletService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock WalletDao walletDao;
    @Mock TransactionDao transactionDao;
    @Mock PaymentGateway gateway;
    @Mock UserDao userDao;

    private WalletServiceImpl service;
    private final EventBus eventBus = new EventBus();
    private final List<WalletToppedUp> published = new ArrayList<>();

    /** Runs the work directly; real commit/rollback is covered by the JDBC tests. */
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
        eventBus.subscribe(WalletToppedUp.class, published::add);
        lenient().when(userDao.findById(1)).thenReturn(Optional.of(user(Role.CLIENT)));
        service = new WalletServiceImpl(walletDao, transactionDao, gateway, passThrough, eventBus, userDao);
    }

    @Test
    void topUpCreditsWalletAndWritesLedger() {
        Wallet wallet = new Wallet(7, 1, 100, 20, 0);
        when(walletDao.findByUserId(1)).thenReturn(Optional.of(wallet));
        when(walletDao.findByUserIdForUpdate(1)).thenReturn(Optional.of(wallet));
        when(gateway.process(any())).thenReturn(PaymentResult.succeeded("SIM-1"));

        WalletDto result = service.topUp(1, 500);

        assertEquals(600, result.availableBalance());
        assertEquals(20, result.escrowBalance());
        verify(walletDao).updateBalances(7, 600, 20);
        verify(transactionDao).insert(7, null, TransactionType.TOPUP, 500, "SIM-1");
    }

    @Test
    void topUpRejectsAmountOutsideRange() {
        assertThrows(ValidationException.class, () -> service.topUp(1, 0));
        assertThrows(ValidationException.class, () -> service.topUp(1, -5));
        assertThrows(ValidationException.class, () -> service.topUp(1, WalletService.MAX_TOP_UP + 1));
        verifyNoInteractions(walletDao, transactionDao, gateway);
    }

    @Test
    void topUpAcceptsBoundaryAmounts() {
        Wallet wallet = new Wallet(7, 1, 0, 0, 0);
        when(walletDao.findByUserId(1)).thenReturn(Optional.of(wallet));
        when(walletDao.findByUserIdForUpdate(1)).thenReturn(Optional.of(wallet));
        when(gateway.process(any())).thenReturn(PaymentResult.succeeded("SIM-1"));

        assertEquals(1, service.topUp(1, 1).availableBalance());
        assertEquals(10_000, service.topUp(1, 10_000).availableBalance());
    }

    @Test
    void declinedPaymentChangesNothing() {
        when(walletDao.findByUserId(1)).thenReturn(Optional.of(new Wallet(7, 1, 100, 0, 0)));
        when(gateway.process(any())).thenReturn(PaymentResult.failed("declined"));

        assertThrows(ValidationException.class, () -> service.topUp(1, 50));

        verify(walletDao, never()).updateBalances(anyLong(), anyLong(), anyLong());
        verifyNoInteractions(transactionDao);
    }

    @Test
    void topUpWithoutWalletFails() {
        when(walletDao.findByUserId(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.topUp(1, 50));
        verifyNoInteractions(gateway);
    }

    @Test
    void createWalletWithInitialBalanceWritesLedgerEntry() {
        when(walletDao.findByUserIdForUpdate(1)).thenReturn(Optional.empty());
        when(walletDao.create(1, 1000)).thenReturn(new Wallet(9, 1, 1000, 0, 0));

        WalletDto dto = service.createWallet(1, 1000);

        assertEquals(1000, dto.availableBalance());
        verify(transactionDao).insert(eq(9L), eq(null), eq(TransactionType.TOPUP), eq(1000L), eq("Initial balance"));
    }

    @Test
    void createWalletWithZeroBalanceWritesNoLedgerEntry() {
        when(walletDao.findByUserIdForUpdate(1)).thenReturn(Optional.empty());
        when(walletDao.create(1, 0)).thenReturn(new Wallet(9, 1, 0, 0, 0));

        service.createWallet(1, 0);

        verifyNoInteractions(transactionDao);
    }

    @Test
    void createWalletRejectsNegativeBalanceAndDuplicates() {
        assertThrows(ValidationException.class, () -> service.createWallet(1, -1));

        when(walletDao.findByUserIdForUpdate(1)).thenReturn(Optional.of(new Wallet(9, 1, 0, 0, 0)));
        assertThrows(ValidationException.class, () -> service.createWallet(1, 10));
        verify(walletDao, never()).create(anyLong(), anyLong());
    }

    @Test
    void getWalletReturnsBalances() {
        when(walletDao.findByUserId(1)).thenReturn(Optional.of(new Wallet(7, 1, 80, 20, 3)));

        WalletDto dto = service.getWallet(1);

        assertEquals(80, dto.availableBalance());
        assertEquals(20, dto.escrowBalance());
    }

    @Test
    void getWalletWithoutWalletFails() {
        when(walletDao.findByUserId(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getWallet(1));
    }

    @Test
    void topUpPublishesWalletToppedUpEvent() {
        Wallet wallet = new Wallet(7, 1, 100, 0, 0);
        when(walletDao.findByUserId(1)).thenReturn(Optional.of(wallet));
        when(walletDao.findByUserIdForUpdate(1)).thenReturn(Optional.of(wallet));
        when(gateway.process(any())).thenReturn(PaymentResult.succeeded("SIM-1"));

        service.topUp(1, 250);

        assertEquals(List.of(new WalletToppedUp(1, 250)), published);
    }

    @Test
    void declinedTopUpPublishesNoEvent() {
        when(walletDao.findByUserId(1)).thenReturn(Optional.of(new Wallet(7, 1, 100, 0, 0)));
        when(gateway.process(any())).thenReturn(PaymentResult.failed("declined"));

        assertThrows(ValidationException.class, () -> service.topUp(1, 50));

        assertEquals(0, published.size());
    }

    @Test
    void failedDatabaseWorkPublishesNoEvent() {
        Wallet wallet = new Wallet(7, 1, 100, 0, 0);
        when(walletDao.findByUserId(1)).thenReturn(Optional.of(wallet));
        when(walletDao.findByUserIdForUpdate(1)).thenReturn(Optional.empty());
        when(gateway.process(any())).thenReturn(PaymentResult.succeeded("SIM-1"));

        assertThrows(ResourceNotFoundException.class, () -> service.topUp(1, 50));

        assertEquals(0, published.size());
    }

    @Test
    void onlyClientsMayTopUp() {
        when(userDao.findById(2)).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(userDao.findById(3)).thenReturn(Optional.of(user(Role.ADMIN)));

        assertThrows(ValidationException.class, () -> service.topUp(2, 50));
        assertThrows(ValidationException.class, () -> service.topUp(3, 50));

        verifyNoInteractions(walletDao, transactionDao, gateway);
        assertEquals(0, published.size());
    }

    @Test
    void topUpForUnknownUserFails() {
        when(userDao.findById(9)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.topUp(9, 50));

        verifyNoInteractions(walletDao, gateway);
    }

    @Test
    void freelancerWithdrawalReducesBalanceAndWritesLedger() {
        when(userDao.findById(2)).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(walletDao.findByUserIdForUpdate(2)).thenReturn(Optional.of(new Wallet(8, 2, 300, 40, 0)));

        WalletDto result = service.withdraw(2, 120);

        assertEquals(180, result.availableBalance());
        assertEquals(40, result.escrowBalance());
        verify(walletDao).updateBalances(8, 180, 40);
        verify(transactionDao).insert(8, null, TransactionType.WITHDRAWAL, 120, "Simulated withdrawal");
        verifyNoInteractions(gateway);
    }

    @Test
    void withdrawalCanTakeTheWholeAvailableBalance() {
        when(userDao.findById(2)).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(walletDao.findByUserIdForUpdate(2)).thenReturn(Optional.of(new Wallet(8, 2, 300, 0, 0)));

        assertEquals(0, service.withdraw(2, 300).availableBalance());
    }

    @Test
    void withdrawalAboveTheAvailableBalanceChangesNothing() {
        when(userDao.findById(2)).thenReturn(Optional.of(user(Role.FREELANCER)));
        when(walletDao.findByUserIdForUpdate(2)).thenReturn(Optional.of(new Wallet(8, 2, 300, 500, 0)));

        assertThrows(ValidationException.class, () -> service.withdraw(2, 301));

        verify(walletDao, never()).updateBalances(anyLong(), anyLong(), anyLong());
        verifyNoInteractions(transactionDao);
    }

    @Test
    void onlyFreelancersMayWithdraw() {
        when(userDao.findById(1)).thenReturn(Optional.of(user(Role.CLIENT)));
        when(userDao.findById(3)).thenReturn(Optional.of(user(Role.ADMIN)));

        assertThrows(ValidationException.class, () -> service.withdraw(1, 10));
        assertThrows(ValidationException.class, () -> service.withdraw(3, 10));

        verifyNoInteractions(walletDao, transactionDao);
    }

    @Test
    void withdrawalRejectsZeroNegativeAndUnknownUser() {
        assertThrows(ValidationException.class, () -> service.withdraw(2, 0));
        assertThrows(ValidationException.class, () -> service.withdraw(2, -1));
        when(userDao.findById(9)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.withdraw(9, 10));
    }

    private static User user(Role role) {
        return new User(1, role, "Test User", "test@example.com", "hash", AccountStatus.ACTIVE,
                LocalDateTime.now());
    }
}
