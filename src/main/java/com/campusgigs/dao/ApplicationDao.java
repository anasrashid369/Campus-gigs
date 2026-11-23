package com.campusgigs.dao;

import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigApplication;

import java.util.List;
import java.util.Optional;

/** Data access for the GigApplications table. Contains SQL and mapping only. */
public interface ApplicationDao {

    /** Stores a new PENDING application and returns its id. */
    long insert(long gigId, long freelancerId, String proposalMessage);

    /** Finds an application by id. */
    Optional<GigApplication> findById(long applicationId);

    /** Finds the application of a freelancer for a gig; there is at most one. */
    Optional<GigApplication> findByGigAndFreelancer(long gigId, long freelancerId);

    /** Lists the applications for a gig, oldest first. */
    List<GigApplication> findByGigId(long gigId);

    /** Lists a freelancer's applications, newest first. */
    List<GigApplication> findByFreelancerId(long freelancerId);

    /** Counts a freelancer's applications in the given status. */
    long countByFreelancerAndStatus(long freelancerId, ApplicationStatus status);

    /** Sets the status of an application. */
    void updateStatus(long applicationId, ApplicationStatus status);

    /** Turns a WITHDRAWN application back into a PENDING one with a new message and a new send time. */
    void reopen(long applicationId, String proposalMessage);

    /** Rejects every PENDING application of the gig except the accepted one. */
    void rejectPendingExcept(long gigId, long acceptedApplicationId);
}
