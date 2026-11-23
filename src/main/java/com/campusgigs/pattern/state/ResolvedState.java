package com.campusgigs.pattern.state;

import com.campusgigs.model.GigStatus;

/** Dispute decided by an administrator. Final. */
final class ResolvedState extends GigState {

    ResolvedState() {
        super(GigStatus.RESOLVED);
    }
}
