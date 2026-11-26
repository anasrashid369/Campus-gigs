package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.dao.TransactionDao;
import com.campusgigs.model.Transaction;
import com.campusgigs.model.TransactionType;
import com.campusgigs.model.Wallet;
import com.campusgigs.model.LedgerEntry;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class WalletDaoHibernateTest {

    private SessionFactory db;
    private HibernateTransactionManager tx;
    private WalletDaoHibernate wallets;
    private TransactionDaoHibernate ledger;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        tx = new HibernateTransactionManager(db);
        wallets = new WalletDaoHibernate(db);
        ledger = new TransactionDaoHibernate(db);
        userId = DbTestSupport.createUser(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            DbTestSupport.deleteUser(db, userId);
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

    @Test
    void sumsLedgerEntriesByType() {
        Wallet w = wallets.create(userId, 0);

        assertEquals(0, ledger.sumByType(w.walletId(), TransactionType.TOPUP));
        ledger.insert(w.walletId(), null, TransactionType.TOPUP, 5, "a");
        ledger.insert(w.walletId(), null, TransactionType.TOPUP, 7, "b");
        ledger.insert(w.walletId(), null, TransactionType.WITHDRAWAL, 2, "c");

        assertEquals(12, ledger.sumByType(w.walletId(), TransactionType.TOPUP));
        assertEquals(2, ledger.sumByType(w.walletId(), TransactionType.WITHDRAWAL));
        assertEquals(0, ledger.sumByType(w.walletId(), TransactionType.PLATFORM_FEE));
    }

    @Test
    void ledgerQueryShowsOwnerNamesAndFiltersByTypeAndDay() {
        Wallet w = wallets.create(userId, 0);
        Transaction topUp = ledger.insert(w.walletId(), null, TransactionType.TOPUP, 40, "ledger-test");
        Transaction fee = ledger.insert(w.walletId(), null, TransactionType.PLATFORM_FEE, 3, "ledger-test");
        LocalDate today = LocalDate.now();

        List<LedgerEntry> all = ledger.findLedger(null, today, today);
        List<LedgerEntry> onlyFees = ledger.findLedger(TransactionType.PLATFORM_FEE, today, today);
        List<LedgerEntry> yesterday = ledger.findLedger(null, today.minusDays(5), today.minusDays(1));

        LedgerEntry found = all.stream().filter(e -> e.transactionId() == topUp.transactionId()).findFirst().orElseThrow();
        assertEquals("Test", found.userName());
        assertEquals(userId, found.userId());
        assertEquals(40, found.amount());
        assertTrue(onlyFees.stream().anyMatch(e -> e.transactionId() == fee.transactionId()));
        assertTrue(onlyFees.stream().noneMatch(e -> e.transactionId() == topUp.transactionId()));
        assertTrue(yesterday.stream().noneMatch(e -> e.transactionId() == topUp.transactionId()));
        assertTrue(all.size() <= TransactionDao.MAX_LEDGER_ROWS);
    }
}
