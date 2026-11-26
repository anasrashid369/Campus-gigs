package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusgigs.dto.DisputeDto;
import com.campusgigs.exception.InvalidGigTransitionException;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.DisputeStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Wallet;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.impl.EscrowServiceImpl;
import com.campusgigs.service.impl.GigLifecycleServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The whole gig lifecycle on the real database: status, escrow and ledger change together or not at all. */
class GigLifecycleIntegrationTest {

    private static final long BUDGET = 600;

    private SessionFactory db;
    private WalletDaoHibernate wallets;
    private GigDaoHibernate gigs;
    private DisputeDaoHibernate disputes;
    private GigLifecycleService lifecycle;
    private long clientId;
    private long freelancerId;
    private long otherFreelancerId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        wallets = new WalletDaoHibernate(db);
        gigs = new GigDaoHibernate(db);
        disputes = new DisputeDaoHibernate(db);
        HibernateTransactionManager transactions = new HibernateTransactionManager(db);
        SettingsService settings = mock(SettingsService.class);
        when(settings.getPlatformFeePercent()).thenReturn(5);
        lifecycle = new GigLifecycleServiceImpl(gigs, disputes, new UserDaoHibernate(db),
                new EscrowServiceImpl(wallets, new TransactionDaoHibernate(db), transactions),
                settings, new GigStateMachine(), transactions, new EventBus());

        clientId = DbTestSupport.createUser(db, "CLIENT");
        freelancerId = DbTestSupport.createUser(db, "FREELANCER");
        otherFreelancerId = DbTestSupport.createUser(db, "FREELANCER");
        categoryId = DbTestSupport.createCategory(db);
        gigId = DbTestSupport.createGig(db, clientId, categoryId, BUDGET);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            DbTestSupport.deleteGig(db, gigId);
            DbTestSupport.deleteCategory(db, categoryId);
            DbTestSupport.deleteUser(db, clientId);
            DbTestSupport.deleteUser(db, freelancerId);
            DbTestSupport.deleteUser(db, otherFreelancerId);
        }
    }

    private Wallet wallet(long userId) {
        return wallets.findByUserId(userId).orElseThrow();
    }

    private Gig gig() {
        return gigs.findById(gigId).orElseThrow();
    }

    @Test
    void happyPathPaysTheFreelancerAndConservesTokens() {
        wallets.create(clientId, 1000);
        wallets.create(freelancerId, 0);

        lifecycle.hire(clientId, gigId, freelancerId);
        assertEquals(GigStatus.ASSIGNED, gig().status());
        assertEquals(400, wallet(clientId).availableBalance());
        assertEquals(600, wallet(clientId).escrowBalance());

        lifecycle.submitWork(freelancerId, gigId);
        lifecycle.approve(clientId, gigId);

        assertEquals(GigStatus.APPROVED, gig().status());
        assertEquals(400, wallet(clientId).availableBalance());
        assertEquals(0, wallet(clientId).escrowBalance());
        assertEquals(570, wallet(freelancerId).availableBalance());
        long fee = 30;
        assertEquals(1000, wallet(clientId).availableBalance() + wallet(freelancerId).availableBalance() + fee);
    }

    @Test
    void revisionSendsTheGigBackAndItCanBeResubmitted() {
        wallets.create(clientId, 1000);
        wallets.create(freelancerId, 0);
        lifecycle.hire(clientId, gigId, freelancerId);
        lifecycle.submitWork(freelancerId, gigId);

        lifecycle.requestRevision(clientId, gigId);
        assertEquals(GigStatus.ASSIGNED, gig().status());
        assertEquals(600, wallet(clientId).escrowBalance());

        lifecycle.submitWork(freelancerId, gigId);
        lifecycle.approve(clientId, gigId);
        assertEquals(GigStatus.APPROVED, gig().status());
    }

    @Test
    void hiringWithTooFewTokensLeavesEverythingUntouched() {
        wallets.create(clientId, 100);
        wallets.create(freelancerId, 0);

        assertThrows(ValidationException.class, () -> lifecycle.hire(clientId, gigId, freelancerId));

        Gig gig = gig();
        assertEquals(GigStatus.OPEN, gig.status());
        assertNull(gig.assignedFreelancerId());
        assertEquals(100, wallet(clientId).availableBalance());
        assertEquals(0, wallet(clientId).escrowBalance());
    }

    @Test
    void aFailedApprovalRollsBackTheStatusAndTheClientWallet() {
        wallets.create(clientId, 1000);
        // hiring only needs the client's wallet; the missing freelancer wallet breaks the payout later
        lifecycle.hire(clientId, gigId, freelancerId);
        lifecycle.submitWork(freelancerId, gigId);

        assertThrows(ResourceNotFoundException.class, () -> lifecycle.approve(clientId, gigId));

        assertEquals(GigStatus.SUBMITTED, gig().status());
        assertEquals(600, wallet(clientId).escrowBalance());
        assertEquals(400, wallet(clientId).availableBalance());
    }

    @Test
    void raisingADisputeStoresItAndMovesTheGigToDisputed() {
        wallets.create(clientId, 1000);
        wallets.create(freelancerId, 0);
        lifecycle.hire(clientId, gigId, freelancerId);
        lifecycle.submitWork(freelancerId, gigId);

        DisputeDto dispute = lifecycle.raiseDispute(clientId, gigId, "The logo is not what we agreed");

        assertEquals(GigStatus.DISPUTED, gig().status());
        assertEquals(DisputeStatus.OPEN, dispute.status());
        assertTrue(disputes.findOpenByGigId(gigId).isPresent());
        assertEquals(600, wallet(clientId).escrowBalance());
        assertThrows(InvalidGigTransitionException.class,
                () -> lifecycle.raiseDispute(freelancerId, gigId, "Second dispute"));
    }

    @Test
    void twoClientsClickingHireAtOnceFundTheGigOnlyOnce() throws Exception {
        wallets.create(clientId, 1000);
        wallets.create(freelancerId, 0);
        wallets.create(otherFreelancerId, 0);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger hired = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (long candidate : new long[] {freelancerId, otherFreelancerId}) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    lifecycle.hire(clientId, gigId, candidate);
                    hired.incrementAndGet();
                } catch (InvalidGigTransitionException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(1, hired.get());
        assertEquals(1, rejected.get());
        assertEquals(400, wallet(clientId).availableBalance());
        assertEquals(600, wallet(clientId).escrowBalance());
        assertEquals(GigStatus.ASSIGNED, gig().status());
    }
}
