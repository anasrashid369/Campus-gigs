package com.campusgigs.service;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigRequest;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.model.GigStatus;
import com.campusgigs.pattern.state.GigAction;

import java.util.List;
import java.util.Set;

/**
 * Posting and managing gigs (F14, F15). Who may do what is checked here, not only in the screens.
 * <p>
 * Methods throw {@link com.campusgigs.exception.ValidationException} when the input or the caller breaks a rule,
 * {@link com.campusgigs.exception.ResourceNotFoundException} for an unknown gig or user, and
 * {@link com.campusgigs.exception.InvalidGigTransitionException} when a gig's status does not allow the move.
 */
public interface GigService {

    /**
     * Posts a new OPEN gig for the client.
     *
     * @throws com.campusgigs.exception.ValidationException if the caller is not an active client or the request breaks a
     *         rule: title 3-100 characters, description 10-2000, budget 1-100000 tokens, deadline today or later,
     *         an active category, at most 10 known skills
     */
    GigDto createGig(long clientId, GigRequest request);

    /**
     * Changes an OPEN gig. Only the client who posted it may do this.
     *
     * @throws com.campusgigs.exception.ValidationException if the gig is not open, the caller is not its owner, or the request breaks a rule
     */
    GigDto updateGig(long clientId, long gigId, GigRequest request);

    /**
     * Cancels an OPEN gig through the gig state machine. Only the client who posted it may do this.
     *
     * @throws com.campusgigs.exception.InvalidGigTransitionException if the gig is no longer open
     */
    GigDto cancelGig(long clientId, long gigId);

    /**
     * Reads one gig. An open gig can be read by any user; other gigs only by their client,
     * the hired freelancer or an administrator.
     */
    GigDto getGig(long userId, long gigId);

    /** The client's own gigs, newest first. */
    List<GigDto> listClientGigs(long clientId);

    /** The gigs the freelancer was hired for, newest first. */
    List<GigDto> listFreelancerGigs(long freelancerId);

    /** All gigs that are still open, newest first. */
    List<GigDto> listOpenGigs();

    /** The skills a gig asks for. */
    List<SkillDto> getGigSkills(long gigId);

    /** The categories a new gig can be posted in. */
    List<CategoryDto> getActiveCategories();

    /** The actions the gig state machine accepts in this status; screens use it to enable buttons. */
    Set<GigAction> allowedActions(GigStatus status);
}
