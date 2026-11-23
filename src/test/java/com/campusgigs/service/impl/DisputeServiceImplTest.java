package com.campusgigs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.campusgigs.dto.PayoutDto;
import com.campusgigs.exception.InvalidGigTransitionException;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.Dispute;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Role;
import com.campusgigs.model.User;
import com.campusgigs.pattern.observer.DisputeResolved;
import com.campusgigs.pattern.observer.EventBus;
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
class DisputeServiceImplTest {

    private static final long ADMIN = 99;
    private static final long CLIENT = 1;
    private static final long FREELANCER = 2;
    private static final long GIG = 10;
    private static final long DISPUTE = 5;
    private static final long BUDGET = 600;

    @Mock DisputeDao disputeDao;
    @Mock GigDao gigDao;
    @Mock UserDao userDao;
    @Mock EscrowService escrow;
    @Mock SettingsService settings;

    private final List<DisputeResolved> events = new ArrayList<>();
    private DisputeServiceImpl service;

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
        bus.subscribe(DisputeResolved.class, events::add);
        service = new DisputeServiceImpl(disputeDao, gigDao, userDao, escrow, settings,
                new GigStateMachine(), passThrough, bus);
        lenient().when(userDao.findById(ADMIN)).thenReturn(Optional.of(user(ADMIN, Role.ADMIN, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(CLIENT)).thenReturn(Optional.of(user(CLIENT, Role.CLIENT, AccountStatus.ACTIVE)));
        lenient().when(userDao.findById(FREELANCER)).thenReturn(Optional.of(user(FREELANCER, Role.FREELANCER, AccountStatus.ACTIVE)));
    }

    private static User user(long id, Role role, AccountStatus status) {
        return new User(id, role, "User " + id, "u" + id + "@example.com", "hash", status, LocalDateTime.now());
    }

    private static Gig gig(GigStatus status) {
        return new Gig(GIG, CLIENT, 1, FREELANCER, "Logo design", "Description", BUDGET,
                LocalDate.of(2030, 1, 1), status, LocalDateTime.now());
    }

    private static Dispute openDispute() {
        return new Dispute(DISPUTE, GIG, CLIENT, null, "Not what we agreed", DisputeStatus.OPEN, null, null);
    }

    private static Dispute resolvedDispute(DisputeDecision decision) {
        return new Dispute(DISPUTE, GIG, CLIENT, ADMIN, "Not what we agreed", DisputeStatus.RESOLVED,
                decision, LocalDateTime.now());
    }

    private void openCase(GigStatus gigStatus) {
        when(disputeDao.findByIdForUpdate(DISPUTE)).thenReturn(Optional.of(openDispute()));
        when(gigDao.findByIdForUpdate(GIG)).thenReturn(Optional.of(gig(gigStatus)));
    }

    @Test
    void releasePaysTheFreelancerMinusTheFee() {
        openCase(GigStatus.DISPUTED);
        when(settings.getPlatformFeePercent()).thenReturn(5);
        when(escrow.releaseToFreelancer(CLIENT, FREELANCER, GIG, BUDGET, 5)).thenReturn(new PayoutDto(BUDGET, 30, 570));
        when(disputeDao.resolve(DISPUTE, ADMIN, DisputeDecision.RELEASE)).thenReturn(true);
        when(disputeDao.findById(DISPUTE)).thenReturn(Optional.of(resolvedDispute(DisputeDecision.RELEASE)));

        DisputeDto result = service.resolve(ADMIN, DISPUTE, DisputeDecision.RELEASE);

        assertEquals(DisputeStatus.RESOLVED, result.status());
        assertEquals(DisputeDecision.RELEASE, result.decision());
        verify(gigDao).updateStatus(GIG, GigStatus.RESOLVED);
        verify(escrow, never()).refundToClient(anyLong(), anyLong(), anyLong());
        assertEquals(List.of(new DisputeResolved(DISPUTE, GIG, CLIENT, FREELANCER, DisputeDecision.RELEASE)), events);
    }

    @Test
    void refundReturnsTheEscrowToTheClient() {
        openCase(GigStatus.DISPUTED);
        when(disputeDao.resolve(DISPUTE, ADMIN, DisputeDecision.REFUND)).thenReturn(true);
        when(disputeDao.findById(DISPUTE)).thenReturn(Optional.of(resolvedDispute(DisputeDecision.REFUND)));

        service.resolve(ADMIN, DISPUTE, DisputeDecision.REFUND);

        verify(escrow).refundToClient(CLIENT, GIG, BUDGET);
        verify(escrow, never()).releaseToFreelancer(anyLong(), anyLong(), anyLong(), anyLong(), anyInt());
        verify(gigDao).updateStatus(GIG, GigStatus.RESOLVED);
        assertEquals(DisputeDecision.REFUND, events.get(0).decision());
    }

    @Test
    void anAlreadyResolvedDisputeCannotBeResolvedAgain() {
        when(disputeDao.findByIdForUpdate(DISPUTE)).thenReturn(Optional.of(resolvedDispute(DisputeDecision.REFUND)));

        assertThrows(ValidationException.class, () -> service.resolve(ADMIN, DISPUTE, DisputeDecision.RELEASE));

        verifyNoInteractions(escrow);
        verify(gigDao, never()).updateStatus(anyLong(), org.mockito.ArgumentMatchers.any());
        assertEquals(0, events.size());
    }

    @Test
    void aFailedPayoutChangesNothingAndPublishesNothing() {
        openCase(GigStatus.DISPUTED);
        when(settings.getPlatformFeePercent()).thenReturn(5);
        when(escrow.releaseToFreelancer(CLIENT, FREELANCER, GIG, BUDGET, 5))
                .thenThrow(new ResourceNotFoundException("Wallet not found"));

        assertThrows(ResourceNotFoundException.class, () -> service.resolve(ADMIN, DISPUTE, DisputeDecision.RELEASE));

        verify(disputeDao, never()).resolve(anyLong(), anyLong(), org.mockito.ArgumentMatchers.any());
        verify(gigDao, never()).updateStatus(anyLong(), org.mockito.ArgumentMatchers.any());
        assertEquals(0, events.size());
    }

    @Test
    void theGigMustBeInTheDisputedStatus() {
        openCase(GigStatus.SUBMITTED);

        assertThrows(InvalidGigTransitionException.class,
                () -> service.resolve(ADMIN, DISPUTE, DisputeDecision.REFUND));

        verifyNoInteractions(escrow);
    }

    @Test
    void onlyAnActiveAdministratorCanResolveOrReview() {
        when(userDao.findById(7)).thenReturn(Optional.of(user(7, Role.ADMIN, AccountStatus.SUSPENDED)));
        when(userDao.findById(8)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.resolve(CLIENT, DISPUTE, DisputeDecision.REFUND));
        assertThrows(ValidationException.class, () -> service.resolve(FREELANCER, DISPUTE, DisputeDecision.RELEASE));
        assertThrows(ValidationException.class, () -> service.resolve(7, DISPUTE, DisputeDecision.RELEASE));
        assertThrows(ResourceNotFoundException.class, () -> service.resolve(8, DISPUTE, DisputeDecision.RELEASE));
        assertThrows(ValidationException.class, () -> service.listDisputes(CLIENT, DisputeStatus.OPEN));
        assertThrows(ValidationException.class, () -> service.getDispute(FREELANCER, DISPUTE));

        verifyNoInteractions(disputeDao, escrow);
    }

    @Test
    void unknownDisputeIsReported() {
        when(disputeDao.findByIdForUpdate(DISPUTE)).thenReturn(Optional.empty());
        when(disputeDao.findById(DISPUTE)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resolve(ADMIN, DISPUTE, DisputeDecision.REFUND));
        assertThrows(ResourceNotFoundException.class, () -> service.getDispute(ADMIN, DISPUTE));
    }

    @Test
    void listsDisputesWithTheirGigDetails() {
        when(disputeDao.findByStatus(DisputeStatus.OPEN)).thenReturn(List.of(openDispute()));
        when(gigDao.findById(GIG)).thenReturn(Optional.of(gig(GigStatus.DISPUTED)));

        List<DisputeDto> result = service.listDisputes(ADMIN, DisputeStatus.OPEN);

        assertEquals(1, result.size());
        assertEquals("Logo design", result.get(0).gigTitle());
        assertEquals(CLIENT, result.get(0).clientId());
        assertEquals(FREELANCER, result.get(0).freelancerId());
        assertEquals(BUDGET, result.get(0).budget());
    }
}
