package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.Review;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the Reviews table. */
@Entity
@Table(name = "Reviews")
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "gig_id", nullable = false)
    private long gigId;

    @Column(name = "client_id", nullable = false)
    private long clientId;

    @Column(name = "freelancer_id", nullable = false)
    private long freelancerId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment")
    private String comment;

    protected ReviewEntity() {
    }

    /** A new review; the database fills in the id. */
    public ReviewEntity(long gigId, long clientId, long freelancerId, int rating, String comment) {
        this.gigId = gigId;
        this.clientId = clientId;
        this.freelancerId = freelancerId;
        this.rating = rating;
        this.comment = comment;
    }

    public long getReviewId() {
        return reviewId;
    }

    public Review toModel() {
        return new Review(reviewId, gigId, clientId, freelancerId, rating, comment);
    }
}
