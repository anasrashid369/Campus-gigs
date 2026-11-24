package com.campusgigs.service.impl;

import com.campusgigs.dao.DisputeDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.DisputeDto;
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
import com.campusgigs.pattern.state.GigAction;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.service.DisputeService;
import com.campusgigs.service.EscrowService;
import com.campusgigs.service.SettingsService;

import java.util.List;
import java.util.Objects;

/** Default {@link DisputeService}. */
public class DisputeServiceImpl implements DisputeService {

    private final DisputeDao disputeDao;
    private final GigDao gigDao;
    private final UserDao userDao;
    private final EscrowService escrowService;
    private final SettingsService settingsService;
    private final GigStateMachine stateMachine;
    private final TransactionManager transactions;
    private final EventBus eventBus;

    public DisputeServiceImpl(DisputeDao disputeDao, GigDao gigDao, UserDao userDao,
                              EscrowService escrowService, SettingsService settingsService,
                              GigStateMachine stateMachine, TransactionManager transactions,
                              EventBus eventBus) {
        this.disputeDao = Objects.requireNonNull(disputeDao, "disputeDao");
        this.gigDao = Objects.requireNonNull(gigDao, "gigDao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.escrowService = Objects.requireNonNull(escrowService, "escrowService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    @Override
    public List<DisputeDto> listDisputes(long adminId, DisputeStatus status) {
        Objects.requireNonNull(status, "status");
        requireAdmin(adminId);
        return disputeDao.findByStatus(status).stream()
                .map(dispute -> DisputeMapper.toDto(dispute, findGig(dispute.gigId())))
                .toList();
    }

    @Override
    public DisputeDto getDispute(long adminId, long disputeId) {
        requireAdmin(adminId);
        Dispute dispute = disputeDao.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        return DisputeMapper.toDto(dispute, findGig(dispute.gigId()));
    }

    @Override
    public DisputeDto resolve(long adminId, long disputeId, DisputeDecision decision) {
        Objects.requireNonNull(decision, "decision");
        requireAdmin(adminId);

        DisputeDto resolved = transactions.call(() -> {
            Dispute dispute = disputeDao.findByIdForUpdate(disputeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
            if (dispute.status() != DisputeStatus.OPEN) {
                throw new ValidationException("This dispute has already been resolved");
            }
            Gig gig = gigDao.findByIdForUpdate(dispute.gigId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
            GigStatus next = stateMachine.transition(gig.status(), GigAction.RESOLVE_DISPUTE);
            long freelancerId = hiredFreelancer(gig);

            if (decision == DisputeDecision.RELEASE) {
                escrowService.releaseToFreelancer(gig.clientId(), freelancerId, gig.gigId(), gig.budget(),
                        settingsService.getPlatformFeePercent());
            } else {
                escrowService.refundToClient(gig.clientId(), gig.gigId(), gig.budget());
            }
            if (!disputeDao.resolve(disputeId, adminId, decision)) {
                throw new ValidationException("This dispute has already been resolved");
            }
            gigDao.updateStatus(gig.gigId(), next);

            Dispute saved = disputeDao.findById(disputeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
            Gig resolvedGig = new Gig(gig.gigId(), gig.clientId(), gig.categoryId(), freelancerId, gig.title(),
                    gig.description(), gig.budget(), gig.deadline(), next, gig.createdAt());
            return DisputeMapper.toDto(saved, resolvedGig);
        });

        eventBus.publish(new DisputeResolved(resolved.disputeId(), resolved.gigId(), resolved.clientId(),
                resolved.freelancerId(), decision));
        return resolved;
    }

    private void requireAdmin(long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.role() != Role.ADMIN || user.accountStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Only an administrator can review disputes");
        }
    }

    private Gig findGig(long gigId) {
        return gigDao.findById(gigId).orElseThrow(() -> new ResourceNotFoundException("Gig not found"));
    }

    private static long hiredFreelancer(Gig gig) {
        if (gig.assignedFreelancerId() == null) {
            throw new IllegalStateException("Gig " + gig.gigId() + " has no hired freelancer");
        }
        return gig.assignedFreelancerId();
    }
}
