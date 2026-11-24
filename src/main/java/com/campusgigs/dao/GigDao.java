package com.campusgigs.dao;

import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Skill;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Data access for the Gigs and GigSkills tables.
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

    /** Stores a new OPEN gig and returns its id. */
    long insert(long clientId, long categoryId, String title, String description, long budget, LocalDate deadline);

    /** Overwrites the editable fields of a gig. */
    void update(long gigId, long categoryId, String title, String description, long budget, LocalDate deadline);

    /** Lists the client's gigs, newest first. */
    List<Gig> findByClientId(long clientId);

    /** Lists the gigs the freelancer was hired for, newest first. */
    List<Gig> findByFreelancerId(long freelancerId);

    /** Lists the gigs in the given status, newest first. */
    List<Gig> findByStatus(GigStatus status);

    /** Replaces the skills the gig asks for. Run it in the same transaction as the gig change. */
    void replaceSkills(long gigId, List<Long> skillIds);

    /** Lists the skills the gig asks for, by name. */
    List<Skill> findSkills(long gigId);

    /**
     * Lists the OPEN gigs that match every given condition, newest first. A null condition is ignored.
     *
     * @param keyword text that must appear in the title or the description (not a pattern: % and _ are plain characters)
     */
    List<Gig> searchOpen(String keyword, Long categoryId, Long minBudget, Long maxBudget);

    /** Returns, for each of the given gigs that asks for skills, the ids of those skills. */
    Map<Long, Set<Long>> findSkillIdsByGigIds(Collection<Long> gigIds);

    /** Lists all gigs, or only those in the given status when it is not null, newest first. */
    List<Gig> findAll(GigStatus statusOrNull);
}
