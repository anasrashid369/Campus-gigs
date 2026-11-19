package com.campusgigs.model;

/** Gig lifecycle status. Names match Gigs.status in the database. */
public enum GigStatus {
    OPEN, ASSIGNED, SUBMITTED, APPROVED, DISPUTED, RESOLVED, CANCELLED
}
