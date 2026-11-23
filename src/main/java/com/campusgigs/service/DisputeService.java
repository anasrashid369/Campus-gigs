package com.campusgigs.service;

import com.campusgigs.dto.DisputeDto;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;

import java.util.List;

/**
 * Administrator review of disputes (F10) and their resolution (F11).
 * Only an active ADMIN may use it; the check is made here, not only in the UI.
 * <p>
 * Methods throw {@link com.campusgigs.exception.ValidationException} when the caller is not an
 * administrator and {@link com.campusgigs.exception.ResourceNotFoundException} for an unknown dispute or user.
 */
public interface DisputeService {

    /** Lists the disputes with the given status, newest first. */
    List<DisputeDto> listDisputes(long adminId, DisputeStatus status);

    /** Returns one dispute with the facts needed to decide it. */
    DisputeDto getDispute(long adminId, long disputeId);

    /**
     * Decides an OPEN dispute in one transaction:
     * <ul>
     *   <li>{@code RELEASE}: the escrow is paid to the freelancer minus the platform fee;</li>
     *   <li>{@code REFUND}: the escrow returns to the client's available tokens.</li>
     * </ul>
     * The gig becomes RESOLVED, the decision is recorded and {@code DisputeResolved} is published
     * after commit. A dispute can be resolved only once.
     *
     * @throws com.campusgigs.exception.ValidationException if the dispute is already resolved
     */
    DisputeDto resolve(long adminId, long disputeId, DisputeDecision decision);
}
