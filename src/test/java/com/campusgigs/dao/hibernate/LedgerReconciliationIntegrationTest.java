package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import com.campusgigs.model.Wallet;
import com.campusgigs.model.TransactionType;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.payment.SimulatedPaymentGateway;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.impl.DisputeServiceImpl;
import com.campusgigs.service.impl.EscrowServiceImpl;
import com.campusgigs.service.impl.GigLifecycleServiceImpl;
import com.campusgigs.service.impl.WalletServiceImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Money reconciliation. Runs a long random mix of top-ups, hires, submissions, approvals,
 * revisions, disputes, resolutions and withdrawals on the real database, ignoring the calls
 * the rules reject, and then proves from the ledger alone that:
 * <ol>
 *   <li>every wallet's balance equals what its ledger entries add up to;</li>
 *   <li>every client's escrow equals holds minus refunds minus releases;</li>
 *   <li>no token was created or lost: all balances plus all platform fees equal
 *       everything topped up minus everything withdrawn.</li>
 * </ol>
 */
class LedgerReconciliationIntegrationTest {

    private static final int CLIENTS = 3;
    private static final int FREELANCERS = 3;
    private static final int GIGS_PER_CLIENT = 4;
    private static final int STEPS = 250;

    private SessionFactory db;
    private WalletDaoHibernate wallets;
    private TransactionDaoHibernate ledger;
    private GigLifecycleServiceImpl lifecycle;
    private DisputeServiceImpl disputes;
    private WalletServiceImpl walletService;

    private final List<Long> clientIds = new ArrayList<>();
    private final List<Long> freelancerIds = new ArrayList<>();
    private final List<Long> gigIds = new ArrayList<>();
    private final List<Long> gigOwners = new ArrayList<>();
    private final Map<String, Integer> succeeded = new HashMap<>();
    private long adminId;
    private long categoryId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        wallets = new WalletDaoHibernate(db);
        ledger = new TransactionDaoHibernate(db);
        GigDaoHibernate gigs = new GigDaoHibernate(db);
        DisputeDaoHibernate disputeDao = new DisputeDaoHibernate(db);
        UserDaoHibernate users = new UserDaoHibernate(db);
        HibernateTransactionManager transactions = new HibernateTransactionManager(db);
        EscrowServiceImpl escrow = new EscrowServiceImpl(wallets, ledger, transactions);
        SettingsService settings = mock(SettingsService.class);
        when(settings.getPlatformFeePercent()).thenReturn(7);   // 7% so fees are often rounded down
        GigStateMachine machine = new GigStateMachine();
        EventBus bus = new EventBus();
        lifecycle = new GigLifecycleServiceImpl(gigs, disputeDao, users, escrow, settings, machine, transactions, bus);
        disputes = new DisputeServiceImpl(disputeDao, gigs, users, escrow, settings, machine, transactions, bus);
        walletService = new WalletServiceImpl(wallets, ledger, new SimulatedPaymentGateway(), transactions, bus, users);

        categoryId = DbTestSupport.createCategory(db);
        adminId = DbTestSupport.createUser(db, "ADMIN");
        for (int i = 0; i < CLIENTS; i++) {
            long client = DbTestSupport.createUser(db, "CLIENT");
            clientIds.add(client);
            walletService.createWallet(client, 1500);
            for (int g = 0; g < GIGS_PER_CLIENT; g++) {
                gigIds.add(DbTestSupport.createGig(db, client, categoryId, 100 + 50 * g));
                gigOwners.add(client);
            }
        }
        for (int i = 0; i < FREELANCERS; i++) {
            long freelancer = DbTestSupport.createUser(db, "FREELANCER");
            freelancerIds.add(freelancer);
            walletService.createWallet(freelancer, 0);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db == null) {
            return;
        }
        for (long gigId : gigIds) {
            DbTestSupport.deleteGig(db, gigId);
        }
        DbTestSupport.deleteCategory(db, categoryId);
        for (long id : clientIds) {
            DbTestSupport.deleteUser(db, id);
        }
        for (long id : freelancerIds) {
            DbTestSupport.deleteUser(db, id);
        }
        DbTestSupport.deleteUser(db, adminId);
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 42, 2026})
    void ledgerAlwaysMatchesBalancesAndNoTokenIsCreatedOrLost(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < STEPS; step++) {
            randomStep(random);
        }
        assertTrue(succeeded.getOrDefault("hire", 0) > 0, "no hire ever succeeded: " + succeeded);
        assertTrue(succeeded.getOrDefault("approve", 0) > 0, "no approval ever succeeded: " + succeeded);
        assertTrue(succeeded.getOrDefault("withdraw", 0) > 0, "no withdrawal ever succeeded: " + succeeded);
        assertTrue(succeeded.getOrDefault("topUp", 0) > 0, "no top-up ever succeeded: " + succeeded);
        assertReconciled();
    }

    private void randomStep(Random random) {
        int gigIndex = random.nextInt(gigIds.size());
        long gigId = gigIds.get(gigIndex);
        long client = gigOwners.get(gigIndex);
        long freelancer = freelancerIds.get(random.nextInt(freelancerIds.size()));
        try {
            switch (random.nextInt(9)) {
                case 0, 8 -> succeed("hire", () -> lifecycle.hire(client, gigId, freelancer));
                case 1 -> succeed("submit", () -> lifecycle.submitWork(freelancer, gigId));
                case 2 -> succeed("approve", () -> lifecycle.approve(client, gigId));
                case 3 -> succeed("revision", () -> lifecycle.requestRevision(client, gigId));
                case 4 -> succeed("dispute",
                        () -> lifecycle.raiseDispute(random.nextBoolean() ? client : freelancer, gigId, "Reason " + gigId));
                case 5 -> resolveAnOpenDispute(random);
                case 6 -> succeed("topUp", () -> walletService.topUp(client, 1 + random.nextInt(800)));
                default -> succeed("withdraw", () -> walletService.withdraw(freelancer, 1 + random.nextInt(300)));
            }
        } catch (ValidationException | ResourceNotFoundException expectedRejection) {
            // the rules refused this random call (including invalid gig transitions); nothing may have
            // changed, which the audit at the end verifies
        }
    }

    /** Runs one call and counts it when the rules accepted it. */
    private void succeed(String name, Runnable call) {
        call.run();
        succeeded.merge(name, 1, Integer::sum);
    }

    private void resolveAnOpenDispute(Random random) {
        List<com.campusgigs.dto.DisputeDto> open = disputes.listDisputes(adminId, DisputeStatus.OPEN).stream()
                .filter(d -> gigIds.contains(d.gigId()))
                .toList();
        if (!open.isEmpty()) {
            long disputeId = open.get(random.nextInt(open.size())).disputeId();
            succeed("resolve", () -> disputes.resolve(adminId, disputeId,
                    random.nextBoolean() ? DisputeDecision.RELEASE : DisputeDecision.REFUND));
        }
    }

    private void assertReconciled() {
        long allBalances = 0;
        long allTopUps = 0;
        long allWithdrawals = 0;
        long allFees = 0;

        for (long clientId : clientIds) {
            Wallet w = wallets.findByUserId(clientId).orElseThrow();
            long topUps = ledger.sumByType(w.walletId(), TransactionType.TOPUP);
            long holds = ledger.sumByType(w.walletId(), TransactionType.ESCROW_HOLD);
            long refunds = ledger.sumByType(w.walletId(), TransactionType.ESCROW_REFUND);
            long released = ledger.sumByType(w.walletId(), TransactionType.ESCROW_RELEASE);

            assertEquals(topUps - released, w.availableBalance() + w.escrowBalance(),
                    "client " + clientId + " total balance must equal top-ups minus payouts");
            assertEquals(holds - refunds - released, w.escrowBalance(),
                    "client " + clientId + " escrow must equal holds minus refunds minus releases");
            assertEquals(topUps - holds + refunds, w.availableBalance(),
                    "client " + clientId + " available must equal top-ups minus holds plus refunds");
            allBalances += w.availableBalance() + w.escrowBalance();
            allTopUps += topUps;
        }

        for (long freelancerId : freelancerIds) {
            Wallet w = wallets.findByUserId(freelancerId).orElseThrow();
            long topUps = ledger.sumByType(w.walletId(), TransactionType.TOPUP);
            long received = ledger.sumByType(w.walletId(), TransactionType.ESCROW_RELEASE);
            long fees = ledger.sumByType(w.walletId(), TransactionType.PLATFORM_FEE);
            long withdrawn = ledger.sumByType(w.walletId(), TransactionType.WITHDRAWAL);

            assertEquals(topUps + received - fees - withdrawn, w.availableBalance(),
                    "freelancer " + freelancerId + " available must equal what the ledger adds up to");
            assertEquals(0, w.escrowBalance(), "a freelancer never holds escrow");
            allBalances += w.availableBalance();
            allTopUps += topUps;
            allWithdrawals += withdrawn;
            allFees += fees;
        }

        assertEquals(allTopUps - allWithdrawals, allBalances + allFees,
                "tokens in wallets plus platform fees must equal everything added minus everything withdrawn");
        assertTrue(allBalances >= 0);
    }
}
