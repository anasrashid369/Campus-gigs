package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusgigs.dto.DisputeDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Wallet;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.impl.DisputeServiceImpl;
import com.campusgigs.service.impl.EscrowServiceImpl;
import com.campusgigs.service.impl.GigLifecycleServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Dispute resolution on the real database: either outcome conserves tokens and a dispute is decided once. */
class DisputeResolutionIntegrationTest {

    private ConnectionProvider provider;
    private WalletDaoJdbc wallets;
    private GigDaoJdbc gigs;
    private DisputeServiceImpl disputeService;
    private GigLifecycleService lifecycle;
    private long clientId;
    private long freelancerId;
    private long adminId;
    private long otherAdminId;
    private long categoryId;
    private long gigId;
    private long disputeId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        wallets = new WalletDaoJdbc(provider);
        gigs = new GigDaoJdbc(provider);
        DisputeDaoJdbc disputes = new DisputeDaoJdbc(provider);
        UserDaoJdbc users = new UserDaoJdbc(provider);
        TransactionHelper transactions = new TransactionHelper(provider);
        EscrowServiceImpl escrow = new EscrowServiceImpl(wallets, new TransactionDaoJdbc(provider), transactions);
        SettingsService settings = mock(SettingsService.class);
        when(settings.getPlatformFeePercent()).thenReturn(5);
        GigStateMachine machine = new GigStateMachine();
        EventBus bus = new EventBus();
        lifecycle = new GigLifecycleServiceImpl(gigs, disputes, users, escrow, settings, machine, transactions, bus);
        disputeService = new DisputeServiceImpl(disputes, gigs, users, escrow, settings, machine, transactions, bus);

        clientId = DbTestSupport.createUser(provider, "CLIENT");
        freelancerId = DbTestSupport.createUser(provider, "FREELANCER");
        adminId = DbTestSupport.createUser(provider, "ADMIN");
        otherAdminId = DbTestSupport.createUser(provider, "ADMIN");
        categoryId = DbTestSupport.createCategory(provider);
        gigId = DbTestSupport.createGig(provider, clientId, categoryId, 600);

        wallets.create(clientId, 1000);
        wallets.create(freelancerId, 0);
        lifecycle.hire(clientId, gigId, freelancerId);
        lifecycle.submitWork(freelancerId, gigId);
        disputeId = lifecycle.raiseDispute(clientId, gigId, "The work is not what we agreed").disputeId();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider != null) {
            DbTestSupport.deleteGig(provider, gigId);
            DbTestSupport.deleteCategory(provider, categoryId);
            DbTestSupport.deleteUser(provider, clientId);
            DbTestSupport.deleteUser(provider, freelancerId);
            DbTestSupport.deleteUser(provider, adminId);
            DbTestSupport.deleteUser(provider, otherAdminId);
        }
    }

    private Wallet wallet(long userId) {
        return wallets.findByUserId(userId).orElseThrow();
    }

    @Test
    void releasePaysTheFreelancerAndConservesTokens() {
        DisputeDto result = disputeService.resolve(adminId, disputeId, DisputeDecision.RELEASE);

        assertEquals(DisputeStatus.RESOLVED, result.status());
        assertEquals(GigStatus.RESOLVED, gigs.findById(gigId).orElseThrow().status());
        assertEquals(0, wallet(clientId).escrowBalance());
        assertEquals(400, wallet(clientId).availableBalance());
        assertEquals(570, wallet(freelancerId).availableBalance());
        assertEquals(1000, wallet(clientId).availableBalance() + wallet(freelancerId).availableBalance() + 30);
    }

    @Test
    void refundReturnsEverythingToTheClient() {
        disputeService.resolve(adminId, disputeId, DisputeDecision.REFUND);

        assertEquals(GigStatus.RESOLVED, gigs.findById(gigId).orElseThrow().status());
        assertEquals(1000, wallet(clientId).availableBalance());
        assertEquals(0, wallet(clientId).escrowBalance());
        assertEquals(0, wallet(freelancerId).availableBalance());
    }

    @Test
    void aClientCannotResolveTheirOwnDispute() {
        assertThrows(ValidationException.class, () -> disputeService.resolve(clientId, disputeId, DisputeDecision.REFUND));

        assertEquals(GigStatus.DISPUTED, gigs.findById(gigId).orElseThrow().status());
        assertEquals(600, wallet(clientId).escrowBalance());
    }

    @Test
    void theAdminSeesTheOpenDisputeAndItLeavesTheListOnceResolved() {
        assertTrue(disputeService.listDisputes(adminId, DisputeStatus.OPEN).stream()
                .anyMatch(d -> d.disputeId() == disputeId));

        disputeService.resolve(adminId, disputeId, DisputeDecision.REFUND);

        assertTrue(disputeService.listDisputes(adminId, DisputeStatus.OPEN).stream()
                .noneMatch(d -> d.disputeId() == disputeId));
        assertTrue(disputeService.listDisputes(adminId, DisputeStatus.RESOLVED).stream()
                .anyMatch(d -> d.disputeId() == disputeId));
    }

    @Test
    void twoAdminsResolvingAtOnceDecideTheDisputeOnlyOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger resolved = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        long[] admins = {adminId, otherAdminId};
        DisputeDecision[] decisions = {DisputeDecision.RELEASE, DisputeDecision.REFUND};
        for (int i = 0; i < 2; i++) {
            long admin = admins[i];
            DisputeDecision decision = decisions[i];
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    disputeService.resolve(admin, disputeId, decision);
                    resolved.incrementAndGet();
                } catch (ValidationException e) {
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

        assertEquals(1, resolved.get());
        assertEquals(1, rejected.get());
        long total = wallet(clientId).availableBalance() + wallet(clientId).escrowBalance()
                + wallet(freelancerId).availableBalance();
        assertTrue(total == 1000 || total == 970, "tokens were paid out exactly once, total was " + total);
        assertEquals(0, wallet(clientId).escrowBalance());
    }
}
