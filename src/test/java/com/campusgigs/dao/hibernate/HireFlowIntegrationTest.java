package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusgigs.dto.GigDto;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Wallet;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.observer.FreelancerHired;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.impl.ApplicationServiceImpl;
import com.campusgigs.service.impl.EscrowServiceImpl;
import com.campusgigs.service.impl.GigLifecycleServiceImpl;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Apply, then hire, on the real database: applications, gig status and escrow change together or not at all. */
class HireFlowIntegrationTest {

    private SessionFactory db;
    private WalletDaoHibernate wallets;
    private GigDaoHibernate gigs;
    private ApplicationDaoHibernate applications;
    private ApplicationServiceImpl applicationService;
    private final List<Object> hiredEvents = new ArrayList<>();
    private long clientId;
    private long firstFreelancerId;
    private long secondFreelancerId;
    private long categoryId;
    private long gigId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        wallets = new WalletDaoHibernate(db);
        gigs = new GigDaoHibernate(db);
        applications = new ApplicationDaoHibernate(db);
        UserDaoHibernate users = new UserDaoHibernate(db);
        HibernateTransactionManager transactions = new HibernateTransactionManager(db);
        SettingsService settings = mock(SettingsService.class);
        when(settings.getPlatformFeePercent()).thenReturn(5);
        when(settings.getMaxApplicationsPerFreelancer()).thenReturn(5);
        EventBus bus = new EventBus();
        bus.subscribe(FreelancerHired.class, hiredEvents::add);
        GigLifecycleServiceImpl lifecycle = new GigLifecycleServiceImpl(gigs, new DisputeDaoHibernate(db), users,
                new EscrowServiceImpl(wallets, new TransactionDaoHibernate(db), transactions), settings,
                new GigStateMachine(), transactions, bus);
        applicationService = new ApplicationServiceImpl(applications, gigs, users, settings, lifecycle,
                transactions, bus);

        clientId = DbTestSupport.createUser(db, "CLIENT");
        firstFreelancerId = DbTestSupport.createUser(db, "FREELANCER");
        secondFreelancerId = DbTestSupport.createUser(db, "FREELANCER");
        categoryId = DbTestSupport.createCategory(db);
        gigId = DbTestSupport.createGig(db, clientId, categoryId, 600);
        wallets.create(firstFreelancerId, 0);
        wallets.create(secondFreelancerId, 0);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            DbTestSupport.deleteGig(db, gigId);
            DbTestSupport.deleteCategory(db, categoryId);
            DbTestSupport.deleteUser(db, clientId);
            DbTestSupport.deleteUser(db, firstFreelancerId);
            DbTestSupport.deleteUser(db, secondFreelancerId);
        }
    }

    private Wallet clientWallet() {
        return wallets.findByUserId(clientId).orElseThrow();
    }

    @Test
    void hiringOneApplicantAcceptsThemRejectsTheOtherAndFundsTheGig() {
        wallets.create(clientId, 1000);
        long first = applicationService.apply(firstFreelancerId, gigId, "I can design this logo").applicationId();
        long second = applicationService.apply(secondFreelancerId, gigId, "I also can design it").applicationId();

        GigDto hired = applicationService.hire(clientId, gigId, first);

        assertEquals(GigStatus.ASSIGNED, hired.status());
        assertEquals(firstFreelancerId, hired.assignedFreelancerId());
        assertEquals(ApplicationStatus.ACCEPTED, applications.findById(first).orElseThrow().status());
        assertEquals(ApplicationStatus.REJECTED, applications.findById(second).orElseThrow().status());
        assertEquals(400, clientWallet().availableBalance());
        assertEquals(600, clientWallet().escrowBalance());
        assertEquals(1, hiredEvents.size());
    }

    @Test
    void notEnoughTokensLeavesEveryApplicationPendingAndTheGigOpen() {
        wallets.create(clientId, 100);
        long first = applicationService.apply(firstFreelancerId, gigId, "I can design this logo").applicationId();
        long second = applicationService.apply(secondFreelancerId, gigId, "I also can design it").applicationId();

        assertThrows(ValidationException.class, () -> applicationService.hire(clientId, gigId, first));

        assertEquals(ApplicationStatus.PENDING, applications.findById(first).orElseThrow().status());
        assertEquals(ApplicationStatus.PENDING, applications.findById(second).orElseThrow().status());
        assertEquals(GigStatus.OPEN, gigs.findById(gigId).orElseThrow().status());
        assertEquals(100, clientWallet().availableBalance());
        assertEquals(0, clientWallet().escrowBalance());
        assertEquals(0, hiredEvents.size());
    }

    @Test
    void afterTheHireNobodyCanApplyAndTheOtherApplicantCannotWithdraw() {
        wallets.create(clientId, 1000);
        long first = applicationService.apply(firstFreelancerId, gigId, "I can design this logo").applicationId();
        long second = applicationService.apply(secondFreelancerId, gigId, "I also can design it").applicationId();
        applicationService.hire(clientId, gigId, first);

        assertThrows(ValidationException.class,
                () -> applicationService.apply(secondFreelancerId, gigId, "Please reconsider me"));
        assertThrows(ValidationException.class, () -> applicationService.withdraw(secondFreelancerId, second));
    }

    @Test
    void withdrawingAndApplyingAgainReusesTheSameApplication() {
        long id = applicationService.apply(firstFreelancerId, gigId, "My first proposal").applicationId();
        applicationService.withdraw(firstFreelancerId, id);

        long again = applicationService.apply(firstFreelancerId, gigId, "My second proposal").applicationId();

        assertEquals(id, again);
        assertEquals(ApplicationStatus.PENDING, applications.findById(id).orElseThrow().status());
        assertEquals("My second proposal", applications.findById(id).orElseThrow().proposalMessage());
    }
}
