package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusgigs.dto.EarningsDto;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.impl.EarningsServiceImpl;
import com.campusgigs.service.impl.EscrowServiceImpl;
import com.campusgigs.service.impl.GigLifecycleServiceImpl;
import com.campusgigs.service.impl.WalletServiceImpl;
import com.campusgigs.payment.SimulatedPaymentGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Earnings figures follow real payouts, withdrawals and open gigs on the database. */
class EarningsServiceIntegrationTest {

    private ConnectionProvider provider;
    private WalletDaoJdbc wallets;
    private GigLifecycleServiceImpl lifecycle;
    private WalletServiceImpl walletService;
    private EarningsServiceImpl earnings;
    private long clientId;
    private long freelancerId;
    private long categoryId;
    private long firstGigId;
    private long secondGigId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        wallets = new WalletDaoJdbc(provider);
        TransactionDaoJdbc ledger = new TransactionDaoJdbc(provider);
        GigDaoJdbc gigs = new GigDaoJdbc(provider);
        UserDaoJdbc users = new UserDaoJdbc(provider);
        TransactionHelper transactions = new TransactionHelper(provider);
        SettingsService settings = mock(SettingsService.class);
        when(settings.getPlatformFeePercent()).thenReturn(5);
        EventBus bus = new EventBus();
        lifecycle = new GigLifecycleServiceImpl(gigs, new DisputeDaoJdbc(provider), users,
                new EscrowServiceImpl(wallets, ledger, transactions), settings, new GigStateMachine(),
                transactions, bus);
        walletService = new WalletServiceImpl(wallets, ledger, new SimulatedPaymentGateway(), transactions, bus, users);
        earnings = new EarningsServiceImpl(users, wallets, ledger, gigs);

        clientId = DbTestSupport.createUser(provider, "CLIENT");
        freelancerId = DbTestSupport.createUser(provider, "FREELANCER");
        categoryId = DbTestSupport.createCategory(provider);
        firstGigId = DbTestSupport.createGig(provider, clientId, categoryId, 600);
        secondGigId = DbTestSupport.createGig(provider, clientId, categoryId, 300);
        wallets.create(clientId, 1000);
        wallets.create(freelancerId, 0);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider != null) {
            DbTestSupport.deleteGig(provider, firstGigId);
            DbTestSupport.deleteGig(provider, secondGigId);
            DbTestSupport.deleteCategory(provider, categoryId);
            DbTestSupport.deleteUser(provider, clientId);
            DbTestSupport.deleteUser(provider, freelancerId);
        }
    }

    @Test
    void followsPayoutsPendingGigsAndWithdrawals() {
        assertEquals(new EarningsDto(0, 0, 0, 0, 0), earnings.getEarnings(freelancerId));

        lifecycle.hire(clientId, firstGigId, freelancerId);
        lifecycle.submitWork(freelancerId, firstGigId);
        lifecycle.approve(clientId, firstGigId);
        lifecycle.hire(clientId, secondGigId, freelancerId);

        assertEquals(new EarningsDto(570, 570, 30, 0, 300), earnings.getEarnings(freelancerId));

        walletService.withdraw(freelancerId, 100);

        assertEquals(new EarningsDto(470, 570, 30, 100, 300), earnings.getEarnings(freelancerId));
    }
}
