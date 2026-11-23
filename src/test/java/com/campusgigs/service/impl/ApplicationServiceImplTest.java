package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.ApplicationDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigApplication;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.AppEvent;
import com.campusgigs.pattern.observer.ApplicationSubmitted;
import com.campusgigs.pattern.observer.ApplicationWithdrawn;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.SettingsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    private static final long CLIENT = 1;
    private static final long OTHER_CLIENT = 2;
    private static final long FREELANCER = 3;
    private static final long OTHER_FREELANCER = 4;
    private static final long GIG = 10;
    private static final long APPLICATION = 20;
    private static final String PROPOSAL = "I have done this before";

    @Mock ApplicationDao applicationDao;
    @Mock GigDao gigDao;
    @Mock UserDao userDao;
    @Mock SettingsService settings;
    @Mock GigLifecycleService lifecycle;

    private final List<AppEvent> events = new ArrayList<>();
    private ApplicationServiceImpl service;

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
        EventBus bus = new EventBus();
        bus.subscribe(ApplicationSubmitted.class, events::add);
        bus.subscribe(ApplicationWithdrawn.class, events::add);
        service = new ApplicationServiceImpl(applicationDao, gigDao, userDao, settings, lifecycle, passThrough, bus);
        lenient().when(userDao.findById(CLIENT)).thenReturn(Optional.of(user(CLIENT, Role.CLIENT, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(OTHER_CLIENT)).thenReturn(Optional.of(user(OTHER_CLIENT, Role.CLIENT, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(FREELANCER)).thenReturn(Optional.of(user(FREELANCER, Role.FREELANCER, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(OTHER_FREELANCER)).thenReturn(Optional.of(user(OTHER_FREELANCER, Role.FREELANCER, AccountStatus.ACTIVE)));
        lenient().when(settings.getMaxApplicationsPerFreelancer()).thenReturn(5);
    }

    private static User user(long id, Role role, AccountStatus status) {
        return new User(id, role, "User " + id, "u" + id + "@example.com", "hash", status, LocalDateTime.now());
    }

    private static Gig gig(GigStatus status) {
        return new Gig(GIG, CLIENT, 1, null, "Logo design", "Description", 300, LocalDate.of(2030, 1, 1), status,
                LocalDateTime.now());
    }

    private static GigApplication application(long id, long freelancerId, ApplicationStatus status) {
        return new GigApplication(id, GIG, freelancerId, PROPOSAL, status, LocalDateTime.now());
    }

    private void openGig() {
        lenient().when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.of(gig(GigStatus.OPEN)));
        lenient().when(gigDao.findById(GIG)).thenReturn(Optional.of(gig(GigStatus.OPEN)));
    }

    // ---- apply

    @Test
    void applyStoresAPendingApplicationAndPublishesAnEvent() {
        openGig();
        when(applicationDao.findByGigAndFreelancer(GIG, FREELANCER)).thenReturn(Optional.empty());
        when(applicationDao.countByFreelancerAndStatus(FREELANCER, ApplicationStatus.PENDING)).thenReturn(0L);
        when(applicationDao.insert(GIG, FREELANCER, PROPOSAL)).thenReturn(APPLICATION);
        when(applicationDao.findById(APPLICATION)).thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));

        ApplicationDto result = service.apply(FREELANCER, GIG, "  " + PROPOSAL + " ");

        assertEquals(APPLICATION, result.applicationId());
        assertEquals(ApplicationStatus.PENDING, result.status());
        assertEquals("Logo design", result.gigTitle());
        assertEquals(List.of(new ApplicationSubmitted(APPLICATION, GIG, CLIENT, FREELANCER)), events);
    }

    @Test
    void theProposalMustBeBetween10And1000Characters() {
        assertThrows(ValidationException.class, () -> service.apply(FREELANCER, GIG, null));
        assertThrows(ValidationException.class, () -> service.apply(FREELANCER, GIG, "   "));
        assertThrows(ValidationException.class, () -> service.apply(FREELANCER, GIG, "123456789"));
        assertThrows(ValidationException.class, () -> service.apply(FREELANCER, GIG, "x".repeat(1001)));
        verifyNoInteractions(applicationDao, gigDao);
    }

    @Test
    void theProposalBoundariesAreAccepted() {
        openGig();
        when(applicationDao.findByGigAndFreelancer(GIG, FREELANCER)).thenReturn(Optional.empty());
        when(applicationDao.insert(anyLong(), anyLong(), anyString())).thenReturn(APPLICATION);
        when(applicationDao.findById(APPLICATION)).thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));

        service.apply(FREELANCER, GIG, "x".repeat(10));
        service.apply(FREELANCER, GIG, "x".repeat(1000));
    }

    @Test
    void onlyAnActiveFreelancerCanApply() {
        when(userDao.findById(5)).thenReturn(Optional.of(user(5, Role.FREELANCER, AccountStatus.SUSPENDED)));

        assertThrows(ValidationException.class, () -> service.apply(CLIENT, GIG, PROPOSAL));
        assertThrows(ValidationException.class, () -> service.apply(5, GIG, PROPOSAL));
        verifyNoInteractions(applicationDao, gigDao);
    }

    @Test
    void aGigThatIsNotOpenTakesNoApplications() {
        when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.of(gig(GigStatus.ASSIGNED)));

        assertThrows(ValidationException.class, () -> service.apply(FREELANCER, GIG, PROPOSAL));

        verify(applicationDao, never()).insert(anyLong(), anyLong(), anyString());
        assertEquals(0, events.size());
    }

    @Test
    void anUnknownGigIsReported() {
        when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.apply(FREELANCER, GIG, PROPOSAL));
    }

    @Test
    void applyingTwiceIsRefusedUnlessTheEarlierOneWasWithdrawn() {
        openGig();
        for (ApplicationStatus status : new ApplicationStatus[] {ApplicationStatus.PENDING,
                ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED}) {
            when(applicationDao.findByGigAndFreelancer(GIG, FREELANCER))
                    .thenReturn(Optional.of(application(APPLICATION, FREELANCER, status)));

            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.apply(FREELANCER, GIG, PROPOSAL));

            assertEquals("You already applied to this gig.", error.getMessage());
        }
        verify(applicationDao, never()).insert(anyLong(), anyLong(), anyString());
        verify(applicationDao, never()).reopen(anyLong(), anyString());
    }

    @Test
    void aWithdrawnApplicationIsReopened() {
        openGig();
        when(applicationDao.findByGigAndFreelancer(GIG, FREELANCER))
                .thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.WITHDRAWN)));
        when(applicationDao.findById(APPLICATION))
                .thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));

        ApplicationDto result = service.apply(FREELANCER, GIG, PROPOSAL);

        verify(applicationDao).reopen(APPLICATION, PROPOSAL);
        verify(applicationDao, never()).insert(anyLong(), anyLong(), anyString());
        assertEquals(ApplicationStatus.PENDING, result.status());
        assertEquals(1, events.size());
    }

    @Test
    void thePendingApplicationLimitIsEnforced() {
        openGig();
        when(applicationDao.findByGigAndFreelancer(GIG, FREELANCER)).thenReturn(Optional.empty());
        when(applicationDao.countByFreelancerAndStatus(FREELANCER, ApplicationStatus.PENDING)).thenReturn(5L);

        ValidationException error = assertThrows(ValidationException.class,
                () -> service.apply(FREELANCER, GIG, PROPOSAL));

        assertEquals("You can have at most 5 pending applications. Withdraw one first.", error.getMessage());
        verify(applicationDao, never()).insert(anyLong(), anyLong(), anyString());
    }

    @Test
    void oneBelowTheLimitStillWorks() {
        openGig();
        when(applicationDao.findByGigAndFreelancer(GIG, FREELANCER)).thenReturn(Optional.empty());
        when(applicationDao.countByFreelancerAndStatus(FREELANCER, ApplicationStatus.PENDING)).thenReturn(4L);
        when(applicationDao.insert(GIG, FREELANCER, PROPOSAL)).thenReturn(APPLICATION);
        when(applicationDao.findById(APPLICATION)).thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));

        service.apply(FREELANCER, GIG, PROPOSAL);

        verify(applicationDao).insert(GIG, FREELANCER, PROPOSAL);
    }

    // ---- withdraw

    @Test
    void theOwnerCanWithdrawAPendingApplication() {
        openGig();
        when(applicationDao.findById(APPLICATION))
                .thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)),
                        Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)),
                        Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.WITHDRAWN)));

        ApplicationDto result = service.withdraw(FREELANCER, APPLICATION);

        verify(applicationDao).updateStatus(APPLICATION, ApplicationStatus.WITHDRAWN);
        assertEquals(ApplicationStatus.WITHDRAWN, result.status());
        assertEquals(List.of(new ApplicationWithdrawn(APPLICATION, GIG, CLIENT, FREELANCER)), events);
    }

    @Test
    void onlyTheOwnerCanWithdraw() {
        openGig();
        when(applicationDao.findById(APPLICATION))
                .thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));

        assertThrows(ValidationException.class, () -> service.withdraw(OTHER_FREELANCER, APPLICATION));

        verify(applicationDao, never()).updateStatus(anyLong(), any());
        assertEquals(0, events.size());
    }

    @Test
    void onlyAPendingApplicationCanBeWithdrawn() {
        openGig();
        for (ApplicationStatus status : new ApplicationStatus[] {ApplicationStatus.ACCEPTED,
                ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN}) {
            when(applicationDao.findById(APPLICATION))
                    .thenReturn(Optional.of(application(APPLICATION, FREELANCER, status)));

            assertThrows(ValidationException.class, () -> service.withdraw(FREELANCER, APPLICATION));
        }
        verify(applicationDao, never()).updateStatus(anyLong(), any());
    }

    @Test
    void withdrawingAnUnknownApplicationIsReported() {
        when(applicationDao.findById(APPLICATION)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.withdraw(FREELANCER, APPLICATION));
        assertThrows(ValidationException.class, () -> service.withdraw(CLIENT, APPLICATION));
    }

    // ---- lists

    @Test
    void theClientSeesTheApplicationsOfTheirGig() {
        openGig();
        when(applicationDao.findByGigId(GIG)).thenReturn(List.of(
                application(20, FREELANCER, ApplicationStatus.PENDING),
                application(21, OTHER_FREELANCER, ApplicationStatus.PENDING)));

        List<ApplicationDto> result = service.listForGig(CLIENT, GIG);

        assertEquals(2, result.size());
        assertEquals("User 3", result.get(0).freelancerName());
        assertEquals("User 4", result.get(1).freelancerName());
    }

    @Test
    void onlyTheOwningClientSeesTheApplications() {
        openGig();

        assertThrows(ValidationException.class, () -> service.listForGig(OTHER_CLIENT, GIG));
        assertThrows(ValidationException.class, () -> service.listForGig(FREELANCER, GIG));
        verify(applicationDao, never()).findByGigId(anyLong());
    }

    @Test
    void aFreelancerSeesOnlyTheirOwnApplicationsWithTheGigState() {
        openGig();
        when(applicationDao.findByFreelancerId(FREELANCER))
                .thenReturn(List.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));

        List<ApplicationDto> result = service.listForFreelancer(FREELANCER);

        assertEquals(1, result.size());
        assertEquals(GigStatus.OPEN, result.get(0).gigStatus());
        assertThrows(ValidationException.class, () -> service.listForFreelancer(CLIENT));
    }

    // ---- hire

    @Test
    void hireAcceptsOneRejectsTheOthersAndThenHiresThroughTheLifecycle() {
        openGig();
        when(applicationDao.findById(APPLICATION))
                .thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));
        GigDto hired = new GigDto(GIG, CLIENT, 1, FREELANCER, "Logo design", "Description", 300,
                LocalDate.of(2030, 1, 1), GigStatus.ASSIGNED, LocalDateTime.now());
        when(lifecycle.hire(CLIENT, GIG, FREELANCER)).thenReturn(hired);

        GigDto result = service.hire(CLIENT, GIG, APPLICATION);

        assertEquals(hired, result);
        InOrder order = inOrder(applicationDao, lifecycle);
        order.verify(applicationDao).updateStatus(APPLICATION, ApplicationStatus.ACCEPTED);
        order.verify(applicationDao).rejectPendingExcept(GIG, APPLICATION);
        order.verify(lifecycle).hire(CLIENT, GIG, FREELANCER);
    }

    @Test
    void aFailedHirePropagatesSoTheTransactionRollsBack() {
        openGig();
        when(applicationDao.findById(APPLICATION))
                .thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));
        when(lifecycle.hire(CLIENT, GIG, FREELANCER)).thenThrow(new ValidationException("Not enough tokens"));

        ValidationException error = assertThrows(ValidationException.class,
                () -> service.hire(CLIENT, GIG, APPLICATION));

        assertEquals("Not enough tokens", error.getMessage());
    }

    @Test
    void hireChecksTheOwnerTheGigAndThePendingState() {
        openGig();
        lenient().when(applicationDao.findById(APPLICATION))
                .thenReturn(Optional.of(application(APPLICATION, FREELANCER, ApplicationStatus.PENDING)));
        when(applicationDao.findById(21))
                .thenReturn(Optional.of(new GigApplication(21, 99, FREELANCER, PROPOSAL, ApplicationStatus.PENDING, LocalDateTime.now())));
        when(applicationDao.findById(22))
                .thenReturn(Optional.of(application(22, FREELANCER, ApplicationStatus.REJECTED)));

        assertThrows(ValidationException.class, () -> service.hire(OTHER_CLIENT, GIG, APPLICATION));
        assertThrows(ValidationException.class, () -> service.hire(FREELANCER, GIG, APPLICATION));
        assertThrows(ValidationException.class, () -> service.hire(CLIENT, GIG, 21));
        assertThrows(ValidationException.class, () -> service.hire(CLIENT, GIG, 22));

        verify(applicationDao, never()).updateStatus(anyLong(), any());
        verifyNoInteractions(lifecycle);
    }

    @Test
    void hiringForAnUnknownApplicationIsReported() {
        openGig();
        when(applicationDao.findById(APPLICATION)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.hire(CLIENT, GIG, APPLICATION));
    }
}
