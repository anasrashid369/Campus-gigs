package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigApplication;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Row of the GigApplications table. */
@Entity
@Table(name = "GigApplications")
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "gig_id", nullable = false)
    private long gigId;

    @Column(name = "freelancer_id", nullable = false)
    private long freelancerId;

    @Column(name = "proposal_message", nullable = false)
    private String proposalMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    /** Set by the database on insert; changed again when a withdrawn application is reopened. */
    @Column(name = "submitted_at", insertable = false)
    private LocalDateTime submittedAt;

    protected ApplicationEntity() {
    }

    /** A new PENDING application; the database fills in the id and submission time. */
    public ApplicationEntity(long gigId, long freelancerId, String proposalMessage) {
        this.gigId = gigId;
        this.freelancerId = freelancerId;
        this.proposalMessage = proposalMessage;
        this.status = ApplicationStatus.PENDING;
    }

    public long getApplicationId() {
        return applicationId;
    }

    public GigApplication toModel() {
        return new GigApplication(applicationId, gigId, freelancerId, proposalMessage, status, submittedAt);
    }
}
