package com.campusgigs.model;

/** Administrator decision on a dispute. Names match Disputes.decision. */
public enum DisputeDecision {
    /** Escrow goes to the freelancer. */
    RELEASE,
    /** Escrow goes back to the client. */
    REFUND
}
