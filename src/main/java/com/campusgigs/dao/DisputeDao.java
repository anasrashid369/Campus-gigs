package com.campusgigs.dao;

import com.campusgigs.model.Dispute;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;

import java.util.List;
import java.util.Optional;

/** Data access for the Disputes table. Contains SQL and mapping only. */
public interface DisputeDao {

    /** Stores a new OPEN dispute and returns it. */
    Dispute insert(long gigId, long raisedBy, String reason);

    /** Finds a dispute by id. */
    Optional<Dispute> findById(long disputeId);

    /**
     * Finds the dispute and locks its row until the surrounding transaction ends.
     * Must be called inside a transaction.
     */
    Optional<Dispute> findByIdForUpdate(long disputeId);

    /** Finds the open dispute of a gig, if there is one. */
    Optional<Dispute> findOpenByGigId(long gigId);

    /** Lists disputes with the given status, newest first. */
    List<Dispute> findByStatus(DisputeStatus status);

    /**
     * Marks an OPEN dispute as resolved.
     *
     * @return true if the dispute was open and is now resolved, false if it was already resolved
     */
    boolean resolve(long disputeId, long adminId, DisputeDecision decision);
}
