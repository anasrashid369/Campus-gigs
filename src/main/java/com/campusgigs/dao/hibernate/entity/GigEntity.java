package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Row of the Gigs table. */
@Entity
@Table(name = "Gigs")
public class GigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gig_id")
    private Long gigId;

    @Column(name = "client_id", nullable = false)
    private long clientId;

    @Column(name = "category_id", nullable = false)
    private long categoryId;

    @Column(name = "assigned_freelancer_id")
    private Long assignedFreelancerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "budget", nullable = false)
    private long budget;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GigStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected GigEntity() {
    }

    /** A new OPEN gig with no freelancer; the database fills in the id and creation time. */
    public GigEntity(long clientId, long categoryId, String title, String description, long budget,
                     LocalDate deadline) {
        this.clientId = clientId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.budget = budget;
        this.deadline = deadline;
        this.status = GigStatus.OPEN;
    }

    public long getGigId() {
        return gigId;
    }

    public Gig toModel() {
        return new Gig(gigId, clientId, categoryId, assignedFreelancerId, title, description, budget, deadline,
                status, createdAt);
    }
}
