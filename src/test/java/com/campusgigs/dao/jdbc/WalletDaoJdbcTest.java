package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.Wallet;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class WalletDaoJdbcTest {

    private ConnectionProvider provider;
    private TransactionHelper tx;
    private WalletDaoJdbc wallets;
    private TransactionDaoJdbc ledger;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        tx = new TransactionHelper(provider);
        wallets = new WalletDaoJdbc(provider);
        ledger = new TransactionDaoJdbc(provider);
        userId = DbTestSupport.createUser(provider);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider != null) {
            DbTestSupport.deleteUser(provider, userId);
        }
    }

    @Test
    void createsAndFindsWallet() {
        Wallet created = wallets.create(userId, 100);

        Wallet found = wallets.findByUserId(userId).orElseThrow();
        assertEquals(created.walletId(), found.walletId());
        assertEquals(100, found.availableBalance());
        assertEquals(0, found.escrowBalance());
    }

    @Test
    void unknownUserHasNoWallet() {
        assertTrue(wallets.findByUserId(-1).isEmpty());
    }

    @Test
    void updateBalancesIncrementsRowVersion() {
        Wallet w = wallets.create(userId, 100);

        wallets.updateBalances(w.walletId(), 70, 30);

        Wallet after = wallets.findByUserId(userId).orElseThrow();
        assertEquals(70, after.availableBalance());
        assertEquals(30, after.escrowBalance());
        assertEquals(1, after.rowVersion());
    }

    @Test
    void databaseRejectsNegativeBalance() {
        Wallet w = wallets.create(userId, 10);

        assertThrows(DataAccessException.class, () -> wallets.updateBalances(w.walletId(), -1, 0));
    }

    @Test
    void ledgerReturnsNewestFirst() {
        Wallet w = wallets.create(userId, 0);
        ledger.insert(w.walletId(), null, TransactionType.TOPUP, 5, "first");
        ledger.insert(w.walletId(), null, TransactionType.WITHDRAWAL, 2, "second");

        List<Transaction> history = ledger.findByWalletId(w.walletId());

        assertEquals(2, history.size());
        assertEquals("second", history.get(0).reference());
        assertEquals(TransactionType.TOPUP, history.get(1).type());
    }

    @Test
    void failureInsideTransactionRollsBackWalletAndLedger() {
        Wallet w = wallets.create(userId, 100);

        assertThrows(IllegalStateException.class, () -> tx.run(() -> {
            wallets.updateBalances(w.walletId(), 500, 0);
            ledger.insert(w.walletId(), null, TransactionType.TOPUP, 400, "will roll back");
            throw new IllegalStateException("simulated failure");
        }));

        assertEquals(100, wallets.findByUserId(userId).orElseThrow().availableBalance());
        assertTrue(ledger.findByWalletId(w.walletId()).isEmpty());
    }

    @Test
    void successfulTransactionCommitsWalletAndLedgerTogether() {
        Wallet w = wallets.create(userId, 100);

        tx.run(() -> {
            Wallet locked = wallets.findByUserIdForUpdate(userId).orElseThrow();
            wallets.updateBalances(locked.walletId(), locked.availableBalance() + 50, 0);
            ledger.insert(locked.walletId(), null, TransactionType.TOPUP, 50, "ok");
        });

        assertEquals(150, wallets.findByUserId(userId).orElseThrow().availableBalance());
        assertEquals(1, ledger.findByWalletId(w.walletId()).size());
    }
}
