package com.campusgigs.service;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.GigDto;

import java.util.List;

/**
 * Applications to gigs (F16, F23, F24) and the hire step that ends them.
 * Who may do what is checked here, not only in the screens.
 * <p>
 * Methods throw {@link com.campusgigs.exception.ValidationException} when the caller or the input breaks a rule and
 * {@link com.campusgigs.exception.ResourceNotFoundException} for an unknown gig, application or user.
 */
public interface ApplicationService {

    /** Shortest allowed proposal, in characters. */
    int MIN_PROPOSAL_LENGTH = 10;

    /** Longest allowed proposal, in characters. */
    int MAX_PROPOSAL_LENGTH = 1000;

    /**
     * A freelancer applies to an open gig. A freelancer who withdrew earlier can apply again; anyone else who
     * already applied is refused. The number of pending applications is limited by the platform setting
     * {@code max_applications_per_freelancer}. Publishes {@code ApplicationSubmitted} after the change is saved.
     */
    ApplicationDto apply(long freelancerId, long gigId, String proposalMessage);

    /** The freelancer takes back a PENDING application. Publishes {@code ApplicationWithdrawn}. */
    ApplicationDto withdraw(long freelancerId, long applicationId);

    /** The applications for a gig, oldest first. Only the client who posted the gig may ask. */
    List<ApplicationDto> listForGig(long clientId, long gigId);

    /** The freelancer's own applications, newest first, with the current state of each gig. */
    List<ApplicationDto> listForFreelancer(long freelancerId);

    /**
     * The client picks one applicant: that application becomes ACCEPTED, the other pending ones REJECTED, and
     * {@link GigLifecycleService#hire} moves the gig budget into escrow, all in one transaction. If the hire fails,
     * for example because the client has too few tokens, nothing changes and every application stays PENDING.
     */
    GigDto hire(long clientId, long gigId, long applicationId);
}
