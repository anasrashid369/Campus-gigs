package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusgigs.dao.DisputeDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.DisputeDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.PayoutDto;
import com.campusgigs.exception.InvalidGigTransitionException;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Dispute;
import com.campusgigs.model.DisputeStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.AppEvent;
import com.campusgigs.pattern.observer.DisputeRaised;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.observer.FreelancerHired;
import com.campusgigs.pattern.observer.RevisionRequested;
import com.campusgigs.pattern.observer.WorkApproved;
import com.campusgigs.pattern.observer.WorkSubmitted;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.service.EscrowService;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GigLifecycleServiceImplTest {

    private static final long CLIENT = 1;
    private static final long FREELANCER = 2;
    private static final long OTHER_CLIENT = 3;
    private static final long OTHER_FREELANCER = 4;
    private static final long GIG = 10;
    private static final long BUDGET = 600;

    @Mock GigDao gigDao;
    @Mock DisputeDao disputeDao;
    @Mock UserDao userDao;
    @Mock EscrowService escrow;
    @Mock SettingsService settings;

    private final List<AppEvent> events = new ArrayList<>();
    private GigLifecycleServiceImpl service;

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
        bus.subscribe(FreelancerHired.class, events::add);
        bus.subscribe(WorkSubmitted.class, events::add);
        bus.subscribe(WorkApproved.class, events::add);
        bus.subscribe(RevisionRequested.class, events::add);
        bus.subscribe(DisputeRaised.class, events::add);
        service = new GigLifecycleServiceImpl(gigDao, disputeDao, userDao, escrow, settings,
                new GigStateMachine(), passThrough, bus);
        lenient().when(userDao.findById(CLIENT)).thenReturn(Optional.of(user(CLIENT, Role.CLIENT, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(OTHER_CLIENT)).thenReturn(Optional.of(user(OTHER_CLIENT, Role.CLIENT, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(FREELANCER)).thenReturn(Optional.of(user(FREELANCER, Role.FREELANCER, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(OTHER_FREELANCER)).thenReturn(Optional.of(user(OTHER_FREELANCER, Role.FREELANCER, AccountStatus.ACTIVE)));
    }

    private static User user(long id, Role role, AccountStatus status) {
        return new User(id, role, "User " + id, "u" + id + "@example.com", "hash", status, LocalDateTime.now());
    }

    private void gig(GigStatus status, Long freelancerId) {
        when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.of(new Gig(GIG, CLIENT, 1, freelancerId,
                "Logo design", "Description", BUDGET, LocalDate.of(2030, 1, 1), status, LocalDateTime.now())));
    }

    // ---- hire

    @Test
    void hireHoldsTheBudgetAssignsTheFreelancerAndPublishesAnEvent() {
        gig(GigStatus.OPEN, null);

        GigDto result = service.hire(CLIENT, GIG, FREELANCER);

        assertEquals(GigStatus.ASSIGNED, result.status());
        assertEquals(FREELANCER, result.assignedFreelancerId());
        verify(escrow).holdInEscrow(CLIENT, GIG, BUDGET);
        verify(gigDao).assign(GIG, FREELANCER, GigStatus.ASSIGNED);
        assertEquals(List.of(new FreelancerHired(GIG, CLIENT, FREELANCER, BUDGET)), events);
    }

    @Test
    void hireFailsWithoutChangesWhenTheClientCannotAfford() {
        gig(GigStatus.OPEN, null);
        when(escrow.holdInEscrow(CLIENT, GIG, BUDGET)).thenThrow(new ValidationException("Not enough tokens"));

        assertThrows(ValidationException.class, () -> service.hire(CLIENT, GIG, FREELANCER));

        verify(gigDao, never()).assign(anyLong(), anyLong(), org.mockito.ArgumentMatchers.any());
        assertEquals(0, events.size());
    }

    @Test
    void onlyTheOwningClientCanHire() {
        gig(GigStatus.OPEN, null);

        assertThrows(ValidationException.class, () -> service.hire(OTHER_CLIENT, GIG, FREELANCER));
        assertThrows(ValidationException.class, () -> service.hire(FREELANCER, GIG, OTHER_FREELANCER));

        verifyNoInteractions(escrow);
    }

    @Test
    void hireRequiresAnActiveFreelancer() {
        gig(GigStatus.OPEN, null);
        when(userDao.findById(9)).thenReturn(Optional.of(user(9, Role.FREELANCER, AccountStatus.SUSPENDED)));
        when(userDao.findById(8)).thenReturn(Optional.of(user(8, Role.CLIENT, AccountStatus.ACTIVE)));
        when(userDao.findById(7)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.hire(CLIENT, GIG, 9));
        assertThrows(ValidationException.class, () -> service.hire(CLIENT, GIG, 8));
        assertThrows(ResourceNotFoundException.class, () -> service.hire(CLIENT, GIG, 7));

        verifyNoInteractions(escrow);
    }

    @Test
    void aGigThatIsNotOpenCannotBeHiredAgain() {
        gig(GigStatus.ASSIGNED, FREELANCER);

        assertThrows(InvalidGigTransitionException.class, () -> service.hire(CLIENT, GIG, OTHER_FREELANCER));

        verifyNoInteractions(escrow);
    }

    @Test
    void unknownGigIsReported() {
        when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.hire(CLIENT, GIG, FREELANCER));
    }

    // ---- submit

    @Test
    void theHiredFreelancerCanSubmitWork() {
        gig(GigStatus.ASSIGNED, FREELANCER);

        GigDto result = service.submitWork(FREELANCER, GIG);

        assertEquals(GigStatus.SUBMITTED, result.status());
        verify(gigDao).updateStatus(GIG, GigStatus.SUBMITTED);
        assertEquals(List.of(new WorkSubmitted(GIG, CLIENT, FREELANCER)), events);
    }

    @Test
    void otherUsersCannotSubmitWork() {
        gig(GigStatus.ASSIGNED, FREELANCER);

        assertThrows(ValidationException.class, () -> service.submitWork(OTHER_FREELANCER, GIG));
        assertThrows(ValidationException.class, () -> service.submitWork(CLIENT, GIG));

        verify(gigDao, never()).updateStatus(anyLong(), org.mockito.ArgumentMatchers.any());
        assertEquals(0, events.size());
    }

    @Test
    void workCannotBeSubmittedBeforeAnyoneIsHired() {
        gig(GigStatus.OPEN, null);

        assertThrows(ValidationException.class, () -> service.submitWork(FREELANCER, GIG));
    }

    @Test
    void workCannotBeSubmittedTwice() {
        gig(GigStatus.SUBMITTED, FREELANCER);

        assertThrows(InvalidGigTransitionException.class, () -> service.submitWork(FREELANCER, GIG));
    }

    // ---- approve

    @Test
    void approvePaysTheFreelancerMinusTheConfiguredFee() {
        gig(GigStatus.SUBMITTED, FREELANCER);
        when(settings.getPlatformFeePercent()).thenReturn(5);
        when(escrow.releaseToFreelancer(CLIENT, FREELANCER, GIG, BUDGET, 5))
                .thenReturn(new PayoutDto(BUDGET, 30, 570));

        GigDto result = service.approve(CLIENT, GIG);

        assertEquals(GigStatus.APPROVED, result.status());
        verify(gigDao).updateStatus(GIG, GigStatus.APPROVED);
        assertEquals(List.of(new WorkApproved(GIG, CLIENT, FREELANCER, 570)), events);
    }

    @Test
    void approveFailsAndKeepsTheStatusWhenThePayoutFails() {
        gig(GigStatus.SUBMITTED, FREELANCER);
        when(settings.getPlatformFeePercent()).thenReturn(5);
        when(escrow.releaseToFreelancer(CLIENT, FREELANCER, GIG, BUDGET, 5))
                .thenThrow(new ResourceNotFoundException("Wallet not found"));

        assertThrows(ResourceNotFoundException.class, () -> service.approve(CLIENT, GIG));

        verify(gigDao, never()).updateStatus(anyLong(), org.mockito.ArgumentMatchers.any());
        assertEquals(0, events.size());
    }

    @Test
    void onlyTheOwningClientCanApproveAndOnlySubmittedWork() {
        gig(GigStatus.SUBMITTED, FREELANCER);
        assertThrows(ValidationException.class, () -> service.approve(OTHER_CLIENT, GIG));
        assertThrows(ValidationException.class, () -> service.approve(FREELANCER, GIG));

        gig(GigStatus.ASSIGNED, FREELANCER);
        assertThrows(InvalidGigTransitionException.class, () -> service.approve(CLIENT, GIG));

        verifyNoInteractions(escrow);
    }

    // ---- revision

    @Test
    void revisionReturnsTheGigToTheFreelancer() {
        gig(GigStatus.SUBMITTED, FREELANCER);

        GigDto result = service.requestRevision(CLIENT, GIG);

        assertEquals(GigStatus.ASSIGNED, result.status());
        verify(gigDao).updateStatus(GIG, GigStatus.ASSIGNED);
        assertEquals(List.of(new RevisionRequested(GIG, CLIENT, FREELANCER)), events);
    }

    @Test
    void onlyTheOwningClientCanRequestARevision() {
        gig(GigStatus.SUBMITTED, FREELANCER);

        assertThrows(ValidationException.class, () -> service.requestRevision(OTHER_CLIENT, GIG));
        assertThrows(ValidationException.class, () -> service.requestRevision(FREELANCER, GIG));
    }

    // ---- dispute

    @Test
    void theClientCanDisputeSubmittedWork() {
        gig(GigStatus.SUBMITTED, FREELANCER);
        when(disputeDao.insert(GIG, CLIENT, "Files are missing"))
                .thenReturn(new Dispute(5, GIG, CLIENT, null, "Files are missing", DisputeStatus.OPEN, null, null));

        DisputeDto result = service.raiseDispute(CLIENT, GIG, "  Files are missing  ");

        assertEquals(5, result.disputeId());
        assertEquals(DisputeStatus.OPEN, result.status());
        assertEquals(FREELANCER, result.freelancerId());
        verify(gigDao).updateStatus(GIG, GigStatus.DISPUTED);
        assertEquals(List.of(new DisputeRaised(5, GIG, CLIENT, FREELANCER, CLIENT)), events);
    }

    @Test
    void theHiredFreelancerCanAlsoDispute() {
        gig(GigStatus.SUBMITTED, FREELANCER);
        when(disputeDao.insert(GIG, FREELANCER, "Client ignores me"))
                .thenReturn(new Dispute(6, GIG, FREELANCER, null, "Client ignores me", DisputeStatus.OPEN, null, null));

        service.raiseDispute(FREELANCER, GIG, "Client ignores me");

        assertEquals(List.of(new DisputeRaised(6, GIG, CLIENT, FREELANCER, FREELANCER)), events);
    }

    @Test
    void strangersCannotDispute() {
        gig(GigStatus.SUBMITTED, FREELANCER);

        assertThrows(ValidationException.class, () -> service.raiseDispute(OTHER_CLIENT, GIG, "reason"));
        assertThrows(ValidationException.class, () -> service.raiseDispute(OTHER_FREELANCER, GIG, "reason"));

        verifyNoInteractions(disputeDao);
    }

    @Test
    void aDisputeNeedsAReasonOfSensibleLength() {
        assertThrows(ValidationException.class, () -> service.raiseDispute(CLIENT, GIG, null));
        assertThrows(ValidationException.class, () -> service.raiseDispute(CLIENT, GIG, "   "));
        assertThrows(ValidationException.class,
                () -> service.raiseDispute(CLIENT, GIG, "x".repeat(1001)));

        verifyNoInteractions(gigDao, disputeDao);
    }

    @Test
    void onlySubmittedWorkCanBeDisputed() {
        gig(GigStatus.ASSIGNED, FREELANCER);

        assertThrows(InvalidGigTransitionException.class, () -> service.raiseDispute(CLIENT, GIG, "reason"));

        verifyNoInteractions(disputeDao);
    }
}
