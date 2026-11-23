package com.campusgigs.service;

import com.campusgigs.dto.DisputeDto;
import com.campusgigs.dto.GigDto;

/**
 * The life of a gig after it is posted: hire, submit, approve, revise and dispute.
 * <p>
 * Every method locks the gig row, checks who is calling, asks the gig state machine
 * whether the move is allowed, moves tokens through {@link EscrowService} and saves the
 * new status, all in one transaction. The matching event is published after the
 * transaction commits (or, when the caller already runs a transaction, when this call returns).
 * <p>
 * All methods throw {@link com.campusgigs.exception.ResourceNotFoundException} for an
 * unknown gig or user, {@link com.campusgigs.exception.ValidationException} when the caller
 * is not allowed to do this, and {@link com.campusgigs.exception.InvalidGigTransitionException}
 * when the gig's status does not allow the move.
 */
public interface GigLifecycleService {

    /** Longest allowed dispute reason, in characters. */
    int MAX_DISPUTE_REASON_LENGTH = 1000;

    /**
     * The client hires one freelancer: the gig budget moves from the client's available
     * tokens into escrow and the gig becomes ASSIGNED. Fails without changes if the client
     * has too few tokens. Publishes {@code FreelancerHired}.
     * <p>
     * Choosing among applicants and marking applications accepted or rejected is the
     * application service's job; this method only checks the freelancer is an active FREELANCER.
     */
    GigDto hire(long clientId, long gigId, long freelancerId);

    /** The hired freelancer delivers the work: ASSIGNED becomes SUBMITTED. Publishes {@code WorkSubmitted}. */
    GigDto submitWork(long freelancerId, long gigId);

    /**
     * The client approves the work: escrow is paid to the freelancer minus the platform fee
     * and the gig becomes APPROVED. Publishes {@code WorkApproved}.
     */
    GigDto approve(long clientId, long gigId);

    /** The client asks for changes: SUBMITTED returns to ASSIGNED. Publishes {@code RevisionRequested}. */
    GigDto requestRevision(long clientId, long gigId);

    /**
     * The client or the hired freelancer disputes submitted work: the gig becomes DISPUTED
     * and an OPEN dispute is stored with the reason. Publishes {@code DisputeRaised}.
     *
     * @param reason evidence notes, 1 to {@value #MAX_DISPUTE_REASON_LENGTH} characters
     */
    DisputeDto raiseDispute(long userId, long gigId, String reason);
}
