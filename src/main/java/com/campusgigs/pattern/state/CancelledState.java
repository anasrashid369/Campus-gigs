package com.campusgigs.pattern.state;

import com.campusgigs.model.GigStatus;

/** Cancelled by the client before anyone was hired. Final. */
final class CancelledState extends GigState {

    CancelledState() {
        super(GigStatus.CANCELLED);
    }
}
