package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campusgigs.dto.PayoutDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.Wallet;
import com.campusgigs.service.impl.EscrowServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Escrow on the real database: tokens are conserved, a failure rolls everything back,
 * and concurrent holds can never spend the same tokens twice.
 */
class EscrowServiceIntegrationTest {

    private SessionFactory db;
    private WalletDaoHibernate wallets;
    private TransactionDaoHibernate ledger;
    private EscrowServiceImpl escrow;
    private long clientId;
    private long freelancerId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        wallets = new WalletDaoHibernate(db);
        ledger = new TransactionDaoHibernate(db);
        escrow = new EscrowServiceImpl(wallets, ledger, new HibernateTransactionManager(db));
        clientId = DbTestSupport.createUser(db, "CLIENT");
        freelancerId = DbTestSupport.createUser(db, "FREELANCER");
        categoryId = DbTestSupport.createCategory(db);
        gigId = DbTestSupport.createGig(db, clientId, categoryId, 1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            DbTestSupport.deleteGig(db, gigId);
            DbTestSupport.deleteCategory(db, categoryId);
            DbTestSupport.deleteUser(db, clientId);
            DbTestSupport.deleteUser(db, freelancerId);
        }
    }

    private long available(long userId) {
        return wallets.findByUserId(userId).orElseThrow().availableBalance();
    }

    private long escrowed(long userId) {
        return wallets.findByUserId(userId).orElseThrow().escrowBalance();
    }

    @Test
    void holdThenReleaseConservesEveryToken() {
        wallets.create(clientId, 1000);
        wallets.create(freelancerId, 0);

        escrow.holdInEscrow(clientId, gigId, 1000);
        assertEquals(0, available(clientId));
        assertEquals(1000, escrowed(clientId));

        PayoutDto payout = escrow.releaseToFreelancer(clientId, freelancerId, gigId, 1000, 5);

        assertEquals(new PayoutDto(1000, 50, 950), payout);
        assertEquals(0, available(clientId) + escrowed(clientId));
        assertEquals(950, available(freelancerId));
        assertEquals(1000, available(clientId) + escrowed(clientId) + available(freelancerId)
                + escrowed(freelancerId) + payout.feeAmount());
    }

    @Test
    void releaseWritesOneLedgerEntryPerMovement() {
        wallets.create(clientId, 1000);
        wallets.create(freelancerId, 0);
        escrow.holdInEscrow(clientId, gigId, 1000);

        escrow.releaseToFreelancer(clientId, freelancerId, gigId, 1000, 5);

        long clientWallet = wallets.findByUserId(clientId).orElseThrow().walletId();
        long freelancerWallet = wallets.findByUserId(freelancerId).orElseThrow().walletId();
        assertEquals(List.of(TransactionType.ESCROW_RELEASE, TransactionType.ESCROW_HOLD),
                ledger.findByWalletId(clientWallet).stream().map(t -> t.type()).toList());
        assertEquals(List.of(TransactionType.PLATFORM_FEE, TransactionType.ESCROW_RELEASE),
                ledger.findByWalletId(freelancerWallet).stream().map(t -> t.type()).toList());
    }

    @Test
    void refundReturnsTheTokensToTheClient() {
        wallets.create(clientId, 1000);
        escrow.holdInEscrow(clientId, gigId, 700);

        escrow.refundToClient(clientId, gigId, 700);

        assertEquals(1000, available(clientId));
        assertEquals(0, escrowed(clientId));
    }

    @Test
    void failedReleaseRollsBackTheClientWalletToo() {
        wallets.create(clientId, 1000);
        escrow.holdInEscrow(clientId, gigId, 1000);
        // the freelancer has no wallet, so the second half of the payout fails

        assertThrows(ResourceNotFoundException.class,
                () -> escrow.releaseToFreelancer(clientId, freelancerId, gigId, 1000, 5));

        Wallet client = wallets.findByUserId(clientId).orElseThrow();
        assertEquals(1000, client.escrowBalance());
        assertEquals(0, client.availableBalance());
        assertEquals(1, ledger.findByWalletId(client.walletId()).size());
    }

    @Test
    void concurrentHoldsNeverSpendTheSameTokensTwice() throws Exception {
        wallets.create(clientId, 500);
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    escrow.holdInEscrow(clientId, gigId, 100);
                    succeeded.incrementAndGet();
                } catch (ValidationException e) {
                    rejected.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(5, succeeded.get());
        assertEquals(5, rejected.get());
        assertEquals(0, available(clientId));
        assertEquals(500, escrowed(clientId));
    }
}
