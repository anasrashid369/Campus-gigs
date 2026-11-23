package com.campusgigs.dao;

import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;

import java.util.Collection;
import java.util.Optional;

/**
 * Data access for the Gigs table: the operations the gig lifecycle needs.
 * Contains SQL and mapping only; the allowed status changes are decided by the
 * gig state machine in the service layer.
 */
public interface GigDao {

    /** Finds a gig by id. */
    Optional<Gig> findById(long gigId);

    /**
     * Finds the gig and locks its row ({@code SELECT ... FOR UPDATE}) until the
     * surrounding transaction ends. Must be called inside a transaction.
     */
    Optional<Gig> findByIdForUpdate(long gigId);

    /** Sets the gig's status. */
    void updateStatus(long gigId, GigStatus status);

    /** Records the hired freelancer and sets the new status in one update. */
    void assign(long gigId, long freelancerId, GigStatus status);

    /** Adds up the budgets of the freelancer's gigs that are in any of the given statuses; 0 if there are none. */
    long sumBudgetByFreelancerAndStatuses(long freelancerId, Collection<GigStatus> statuses);
}
