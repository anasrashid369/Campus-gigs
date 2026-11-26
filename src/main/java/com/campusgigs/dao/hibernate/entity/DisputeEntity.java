package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.Dispute;
import com.campusgigs.model.DisputeDecision;
import com.campusgigs.model.DisputeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Row of the Disputes table. */
@Entity
@Table(name = "Disputes")
public class DisputeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispute_id")
    private Long disputeId;

    @Column(name = "gig_id", nullable = false)
    private long gigId;

    @Column(name = "raised_by", nullable = false)
    private long raisedBy;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DisputeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision")
    private DisputeDecision decision;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected DisputeEntity() {
    }

    /** A new OPEN dispute. */
    public DisputeEntity(long gigId, long raisedBy, String reason) {
        this.gigId = gigId;
        this.raisedBy = raisedBy;
        this.reason = reason;
        this.status = DisputeStatus.OPEN;
    }

    public long getDisputeId() {
        return disputeId;
    }

    public Dispute toModel() {
        return new Dispute(disputeId, gigId, raisedBy, resolvedBy, reason, status, decision, resolvedAt);
    }
}
