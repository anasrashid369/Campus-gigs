package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.payment.SimulatedPaymentGateway;
import com.campusgigs.service.WalletService;
import com.campusgigs.service.impl.TransactionServiceImpl;
import com.campusgigs.service.impl.WalletServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** End-to-end service test on the real database: locking keeps concurrent top-ups exact. */
class WalletTopUpConcurrencyTest {

    private ConnectionProvider provider;
    private WalletService wallets;
    private TransactionServiceImpl history;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        WalletDaoJdbc walletDao = new WalletDaoJdbc(provider);
        TransactionDaoJdbc ledger = new TransactionDaoJdbc(provider);
        wallets = new WalletServiceImpl(walletDao, ledger, new SimulatedPaymentGateway(),
                new TransactionHelper(provider), new EventBus(), new UserDaoJdbc(provider));
        history = new TransactionServiceImpl(walletDao, ledger);
        userId = DbTestSupport.createUser(provider);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider != null) {
            DbTestSupport.deleteUser(provider, userId);
        }
    }

    @Test
    void concurrentTopUpsAreAllCountedAndLedgerMatchesBalance() throws Exception {
        wallets.createWallet(userId, 100);
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> wallets.topUp(userId, 10)));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(300, wallets.getWallet(userId).availableBalance());
        long ledgerTotal = history.getHistory(userId).stream().mapToLong(t -> t.amount()).sum();
        assertEquals(300, ledgerTotal);
    }
}
